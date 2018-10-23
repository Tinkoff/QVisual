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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.objects.DiffReport;
import ru.tinkoff.objects.DiffSnapshot;
import ru.tinkoff.objects.DiffState;
import ru.tinkoff.objects.DiffStory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.apache.http.Consts.UTF_8;
import static org.apache.http.entity.ContentType.DEFAULT_BINARY;
import static org.apache.http.entity.mime.HttpMultipartMode.BROWSER_COMPATIBLE;

public class HttpUtils {

    public static final String FRONTEND_DOMAIN = System.getProperty("frontend.domain");
    public static final String BACKEND_DOMAIN = System.getProperty("backend.domain");

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static String post(String url, String fileName, String json) {
        Try<String> uploadedFile = Try.of(() -> {
            HttpClient client = HttpClientBuilder.create().build();

            HttpEntity entity = MultipartEntityBuilder
                    .create()
                    .setCharset(UTF_8)
                    .setMode(BROWSER_COMPATIBLE)
                    .addBinaryBody("file", json.getBytes(UTF_8), ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), UTF_8), fileName)
                    .build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity);

            HttpResponse response = client.execute(httpPost);
            return new BasicResponseHandler().handleResponse(response);
        }).onFailure(t -> logger.error("[POST json]", t));

        return (uploadedFile.isSuccess()) ? uploadedFile.get() : null;
    }

    public static String post(String url, String fileName, BufferedImage image) {
        Try<String> uploadedFile = Try.of(() -> {
            HttpClient client = HttpClientBuilder.create().build();

            HttpEntity entity = MultipartEntityBuilder
                    .create()
                    .setCharset(UTF_8)
                    .setMode(BROWSER_COMPATIBLE)
                    .addBinaryBody("file", getImageBytes(image), DEFAULT_BINARY, fileName)
                    .build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity);

            HttpResponse response = client.execute(httpPost);
            return new BasicResponseHandler().handleResponse(response);
        }).onFailure(t -> logger.error("[POST image]", t));

        return (uploadedFile.isSuccess()) ? uploadedFile.get() : null;
    }

    public static void post(String url, String snapshot) {
        Try.of(() -> {
            HttpClient client = HttpClientBuilder.create().build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new StringEntity(snapshot, "UTF-8"));

            HttpResponse response = client.execute(httpPost);
            return new BasicResponseHandler().handleResponse(response);
        }).onFailure(t -> logger.error("[POST snapshot]", t));
    }

    public static void setTestRunResults(String testRunId, DiffReport diffReport, String url) {
        try {
            TestRailClient client = new TestRailClient(
                    System.getProperty("testrail.url"),
                    System.getProperty("testrail.login"),
                    System.getProperty("testrail.password"));

            JSONArray resultsData = new JSONArray();
            for (DiffStory story : diffReport.getStories()) {
                List<DiffState> diffStates = story.getStates();

                String testId = diffStates.get(0).getSnapshots().get(0).getTestcaseId();
                if (testId != null && testId.length() > 0) {
                    try {
                        // Check testId exists in testRunId
                        Object testIdResult = client.sendGet("get_test/" + testId);

                        String statusId = "1";
                        for (DiffState state : diffStates) {
                            List<DiffSnapshot> diffSnapshots = state.getSnapshots();

                            for (DiffSnapshot snapshot : diffSnapshots) {
                                if (snapshot.getPixels() != null && snapshot.getPixels().length() > 0 ||
                                        snapshot.getError() != null && snapshot.getError().length() > 0 ||
                                        (snapshot.getElements() != null && snapshot.getElements().size() > 0)) {
                                    statusId = "5";
                                    break;
                                }
                            }

                            if (statusId.equals("5")) {
                                break;
                            }
                        }

                        JSONObject testData = new JSONObject();
                        testData.put("test_id", testId);
                        testData.put("status_id", statusId);
                        testData.put("comment", url + "?testcaseid=" + testId);

                        resultsData.add(testData);
                    } catch (Exception e) {
                        logger.error("[TestRail testId into testRunId]", e);
                    }
                }
            }

            if (resultsData.size() > 0) {
                JSONObject results = new JSONObject();
                results.put("results", resultsData);

                client.sendPost("add_results/" + testRunId, results);
            }
        } catch (Exception e) {
            logger.error("[TestRail]", e);
        }
    }

    private static byte[] getImageBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ImageIO.setUseCache(false);
        ImageIO.write(image, "png", baos);

        byte[] imageBytes = baos.toByteArray();

        baos.flush();

        return imageBytes;
    }
}