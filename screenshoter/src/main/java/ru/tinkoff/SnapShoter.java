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

import io.vavr.control.Try;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.objects.Snapshot;

import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.isNull;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static ru.tinkoff.HttpUtils.BACKEND_DOMAIN;
import static ru.tinkoff.HttpUtils.post;
import static ru.tinkoff.ParserUtils.writeAsString;
import static ru.tinkoff.ScreenShoter.resizeBrowser;
import static ru.tinkoff.WebDriverCapabilities.*;
import static ru.tinkoff.WebDriverUtils.getElements;

public class SnapShoter {

    public static String SNAPSHOT_SERVER = System.getProperty("server", System.getProperty("base.url"));
    public static final String DATE_TIME = System.getProperty("dateTime");
    public static final String SNAPSHOT_BRANCH = System.getProperty("branch");
    public static final String SNAPSHOT_COMMIT = System.getProperty("commit");

    private static final Logger logger = LoggerFactory.getLogger(SnapShoter.class);

    public static void snapshot(Object listLocators, Snapshot snapshot, WebDriver driver) {
        snapshot(listLocators, true, true, snapshot, driver);
    }

    public static void snapshot(Object listLocators, boolean resize, boolean fullScreen, Snapshot snapshot, WebDriver driver) {
        String locators;
        if (listLocators instanceof List) {
            locators = makeLocators((List) listLocators);
        } else {
            locators = (String) listLocators;
        }

        if (isNull(snapshot.getDatetime())) {
            Date reportTime = isNullOrEmpty(DATE_TIME) ? new Date() : Date.from(Instant.parse(DATE_TIME));
            snapshot.setDatetime(reportTime);
        }

        if (isNull(snapshot.getServer())) {
            snapshot.setServer(SNAPSHOT_SERVER);
        }

        if (isNull(snapshot.getBranch())) {
            snapshot.setBranch(SNAPSHOT_BRANCH);
        }

        if (isNull(snapshot.getCommit())) {
            snapshot.setCommit(SNAPSHOT_COMMIT);
        }

        if (isNull(snapshot.getDevice())) {
            snapshot.setDevice(DEVICE_NAME);
        }

        if (isNull(snapshot.getOsName())) {
            snapshot.setOsName(PLATFORM_NAME);
        }

        if (isNull(snapshot.getOsVersion())) {
            snapshot.setOsVersion(PLATFORM_VERSION);
        }

        if (isNull(snapshot.getBrowserName())) {
            snapshot.setBrowserName(BROWSER_NAME);
        }

        if (isNull(snapshot.getBrowserVersion())) {
            snapshot.setBrowserVersion(BROWSER_VERSION);
        }

        snapshot.setRetina(BROWSER_RETINA);

        if (!DEVICE_NAME.isEmpty()) {
            snapshot.setResolution(BROWSER_RESOLUTION);
            createSnapshot(locators, fullScreen, snapshot, driver);
        } else {
            if (resize) {
                String[] resolutions = SCREEN_RESOLUTIONS.replaceAll("\\s+", "").split(",");
                for (String resolution : resolutions) {
                    resizeBrowser(resolution, driver);
                    snapshot.setResolution(resolution);
                    createSnapshot(locators, fullScreen, snapshot, driver);
                }

                resizeBrowser(BROWSER_RESOLUTION, driver);
            } else {
                snapshot.setResolution(BROWSER_RESOLUTION);
                createSnapshot(locators, fullScreen, snapshot, driver);
            }
        }
    }

    private static String makeLocators(List<String> locators) {
        StringBuilder locatorsCombined = new StringBuilder();
        locators.forEach(e -> locatorsCombined.append(e).append("%%"));

        return locatorsCombined.toString().replaceAll("\"", "\\\\\"").replaceAll("%%$", "");
    }

    private static void createSnapshot(String locators, boolean fullScreen, Snapshot snapshot, WebDriver driver) {
        try {
            String elements = (locators != null && locators.length() > 0) ? getElements(locators, driver) : null;
            String screenPath = uploadImage(fullScreen, snapshot.getDatetime(), driver);

            snapshot.setElements(elements);
            snapshot.setUrl(screenPath);

            Try<String> snapshotJson = writeAsString(snapshot);
            if (snapshotJson.isSuccess()) {
                post(BACKEND_DOMAIN + "/snapshots/create", snapshotJson.get());
            }
        } catch (Exception e) {
            logger.error("[create snapshot]", e);
        }
    }

    private static String uploadImage(boolean fullScreen, Date dateTime, WebDriver driver) {
        String screenPath = null;
        BufferedImage screen = new ScreenShoter().takeScreen(fullScreen, driver);

        if (screen != null) {
            String fileName = String.format("%s-%s.png", dateTime.toInstant().getEpochSecond(), randomAlphanumeric(10));
            screenPath = post(BACKEND_DOMAIN + "/upload/image", fileName, screen);
        }

        return screenPath;
    }
}