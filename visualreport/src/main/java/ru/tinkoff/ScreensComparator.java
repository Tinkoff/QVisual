/*
 * Copyright © 2018 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.tinkoff;

/*
 * @author Snezhana Krass
 */

import javafx.util.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.objects.Element;

import java.util.Date;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_8UC4;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.threshold;
import static ru.tinkoff.SnapshotApiService.REPORTS_PATH;

public class ScreensComparator {

    private static Logger logger = LoggerFactory.getLogger(ScreensComparator.class);

    public static Pair<Float, String> compareScreens(Mat actualImage, Mat expectedImage,
                                                     Element.Area actualArea, Element.Area expectedArea,
                                                     boolean background,
                                                     boolean isRetina,
                                                     int inaccuracy,
                                                     StringBuffer error) {
        int xActualOffset = 0;
        int yActualOffset = 0;
        int xExpectedOffset = 0;
        int yExpectedOffset = 0;

        int minWidth = 0;
        int maxWidth = 0;
        int outerWidth = 0;

        int minHeight = 0;
        int maxHeight = 0;
        int outerHeight = 0;

        if (actualArea != null && expectedArea != null) {
            xActualOffset = actualArea.getLeft().intValue();
            yActualOffset = actualArea.getTop().intValue();
            xExpectedOffset = expectedArea.getLeft().intValue();
            yExpectedOffset = expectedArea.getTop().intValue();

            minWidth = (int) min(actualArea.getWidth(), expectedArea.getWidth());
            maxWidth = (int) max(actualArea.getWidth(), expectedArea.getWidth());
            outerWidth = maxWidth - minWidth;

            minHeight = (int) min(actualArea.getHeight(), expectedArea.getHeight());
            maxHeight = (int) max(actualArea.getHeight(), expectedArea.getHeight());
            outerHeight = maxHeight - minHeight;

            if (isRetina) {
                xActualOffset *= 2;
                yActualOffset *= 2;
                xExpectedOffset *= 2;
                yExpectedOffset *= 2;
                minWidth *= 2;
                maxWidth *= 2;
                outerWidth *= 2;
                minHeight *= 2;
                maxHeight *= 2;
                outerHeight *= 2;
            }
        } else {
            minWidth = min(actualImage.width(), expectedImage.width());
            maxWidth = max(actualImage.width(), expectedImage.width());
            outerWidth = maxWidth - minWidth;

            minHeight = min(actualImage.height(), expectedImage.height());
            maxHeight = max(actualImage.height(), expectedImage.height());
            outerHeight = maxHeight - minHeight;
        }

        String imagePath = "";
        float diffPercentage = 0.00f;
        int diffPixels = 0;

        /*
        CV_8UC4 = Blue Green Red Alpha.
        Scalar(0, 0, 255, 255) = red.
        Scalar(255, 255, 255, 0) = transparent.
         */
        Mat diffResult = null;
        Mat redMask = null;
        Mat actualImageMin = null;
        Mat expectedImageMin = null;
        Mat diffTemp = null;

        try {
            actualImageMin = actualImage.submat(new Rect(xActualOffset, yActualOffset, minWidth, minHeight));
            expectedImageMin = expectedImage.submat(new Rect(xExpectedOffset, yExpectedOffset, minWidth, minHeight));

            redMask = new Mat(minHeight, minWidth, CV_8UC4);

            absdiff(actualImageMin, expectedImageMin, redMask);

            threshold(redMask, redMask, inaccuracy, 255, THRESH_BINARY);

            inRange(redMask, new Scalar(255, 255, 255, 0), new Scalar(255, 255, 255, 0), redMask);

            diffPixels = countNonZero(redMask);

            if (outerWidth > 0) {
                diffPixels += outerWidth * minHeight;
            }

            if (outerHeight > 0) {
                diffPixels += minWidth * outerHeight;
            }

            if (diffPixels > 0) {
                diffResult = new Mat(maxHeight, maxWidth, CV_8UC4, new Scalar(0, 0, 255, 255));

                if (background) {
                    diffTemp = actualImageMin;
                } else {
                    diffTemp = new Mat(minHeight, minWidth, CV_8UC4, new Scalar(255, 255, 255, 0));
                }

                diffTemp.setTo(new Scalar(0, 0, 255, 255), redMask);
                diffTemp.copyTo(new Mat(diffResult, new Rect(0, 0, minWidth, minHeight)));

                imagePath = saveImage(diffResult, error);

                diffPercentage = (diffPixels * 100.0f) / (maxWidth * maxHeight);
            }
        } catch (Exception e) {
            error.append(System.currentTimeMillis() + "Could not compare screens: " + e.getMessage()).append("\n");
            logger.error("[compare screens]", e);
        } finally {
            if (diffResult != null) {
                diffResult.release();
            }

            if (redMask != null) {
                redMask.release();
            }

            if (actualImageMin != null) {
                actualImageMin.release();
            }

            if (expectedImageMin != null) {
                expectedImageMin.release();
            }

            if (diffTemp != null) {
                diffTemp.release();
            }
        }

        return new Pair(diffPercentage, imagePath);
    }

    public static String saveImage(Mat image, StringBuffer error) {
        if (!image.empty()) {
            try {
                String fileName = String.format("%s-%s.png", new Date().toInstant().getEpochSecond(), randomAlphanumeric(10));
                imwrite(REPORTS_PATH + fileName, image);

                return fileName;
            } catch (Exception e) {
                error.append(System.currentTimeMillis() + "Could not save image: " + e.getMessage()).append("\n");
                logger.error("[save image]", e);
            }
        }

        return "";
    }
}