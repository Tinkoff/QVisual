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

import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.util.JSON;
import io.vavr.control.Try;
import javafx.util.Pair;
import org.bson.Document;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.objects.*;
import ru.tinkoff.objects.DiffSnapshot.Browser;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.apache.commons.io.FileUtils.*;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang.StringUtils.*;
import static org.opencv.imgcodecs.Imgcodecs.CV_LOAD_IMAGE_UNCHANGED;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static ru.tinkoff.ElementsComparator.assertImage;
import static ru.tinkoff.ElementsComparator.compare;
import static ru.tinkoff.HttpUtils.*;
import static ru.tinkoff.ParserUtils.parseJson;
import static ru.tinkoff.ParserUtils.writeAsString;
import static spark.Spark.get;
import static spark.Spark.post;

public class SnapshotApiService {

    public static final String IMAGES_PATH = System.getProperty("screenshooter.dir") + "/origin/images/";
    public static final String REPORTS_PATH = System.getProperty("screenshooter.dir") + "/reports/";

    private static final ForkJoinPool imagesThreadsPool = new ForkJoinPool(100);
    private final SnapshotStorage snapshotStorage;
    private static final Logger logger = LoggerFactory.getLogger(SnapshotApiService.class);

    public SnapshotApiService(SnapshotStorage snapshotStorage) {
        this.snapshotStorage = snapshotStorage;
        setupEndpoints();

        try {
            forceMkdir(new File(IMAGES_PATH));
            forceMkdir(new File(REPORTS_PATH));
        } catch (IOException e) {
            logger.error("[create directory]", e);
        }
    }

    private void setupEndpoints() {

        get("/snapshots", "application/json", ((request, response) -> {
            try {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET");
                response.header("Content-Type", "application/json");

                List<Document> documents = snapshotStorage.find();
                return JSON.serialize(documents);
            } catch (Exception e) {
                logger.error("[GET /snapshots]", e);
            }

            return null;
        }));

        /*
         * Path parameters:
         * actual - actual date in ISO format yyyy-MM-ddTHH:MM:SS.sssZ
         * expected - expected date in ISO format yyyy-MM-ddTHH:MM:SS.sssZ
         *
         * Query parameters:
         * reload - to create report or get from existing file (equals false by default).
         * rgb - pixel-by-pixels comparison inaccuracy (equals 0 by default).
         * testrunid - to update tests results in TestRail by test run ID (equals empty by default).
         */
        get("/snapshots/report", ((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET");
            response.header("Content-Type", "application/json");

            String testRunId = request.queryParams("testrunid");
            String actualDate = request.queryParams("actual");
            String expectedDate = request.queryParams("expected");
            String datesHash = makeDatesHash(actualDate, expectedDate);

            int inaccuracy = 0;
            String inaccuracyParameter = request.queryParams("rgb");
            if (inaccuracyParameter != null && inaccuracyParameter.length() > 0 && isNumeric(inaccuracyParameter)) {
                inaccuracy = Integer.parseInt(inaccuracyParameter);
            }

            long start = System.currentTimeMillis();
            logger.info("[start report] " + actualDate + "-" + expectedDate);
            String reportJson = "{}";

            try {
                String cacheJson = null;

                boolean reload = Boolean.parseBoolean(request.queryParams("reload"));
                if (!reload) {
                    File jsonFile = new File(REPORTS_PATH + datesHash + ".json");
                    cacheJson = jsonFile.exists() ? readFileToString(jsonFile, "UTF-8") : null;
                }

                if (cacheJson != null) {
                    reportJson = cacheJson;
                } else {
                    reportJson = createDiffReport(actualDate, expectedDate, inaccuracy);
                }

                logger.info("[finished report] " + actualDate + "-" + expectedDate + ", time ms: " + (System.currentTimeMillis() - start));

                if (testRunId != null && testRunId.length() > 0) {
                    setTestRunResults(testRunId, parseJson(reportJson, new TypeReference<DiffReport>() {
                    }).get(), String.format("%s/web/actual/%s/expected/%s", FRONTEND_DOMAIN, actualDate, expectedDate));
                }
            } catch (Exception e) {
                logger.error("[error report]" + actualDate + "-" + expectedDate, e);
            }

            return reportJson;
        }));

        get("/snapshots/:datetime", "application/json", (request, response) ->
                snapshotStorage.find(request.params(":datetime")), new JsonTransformer());

        get("/images/original/:filename", "image/png", ((request, response) -> {
            try {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET");
                response.header("Content-Type", "image/png");
                response.header("Accept-Ranges", "bytes");

                File image = new File(IMAGES_PATH + request.params(":filename"));
                if (image.exists()) {
                    return new FileInputStream(image);
                }
            } catch (Exception e) {
                logger.error("[GET /images/original/:filename]", e);
            }

            return "";
        }));

        get("/images/reports/:filename", "image/png", ((request, response) -> {
            try {
                response.header("Access-Control-Allow-Origin", "*");
                response.header("Access-Control-Allow-Methods", "GET");
                response.header("Content-Type", "image/png");
                response.header("Accept-Ranges", "bytes");

                File image = new File(REPORTS_PATH + request.params(":filename"));
                if (image.exists()) {
                    return new FileInputStream(image);
                }
            } catch (Exception e) {
                logger.error("[GET /images/reports/:filename]", e);
            }

            return "";
        }));

        post("/snapshots/create", ((request, response) -> {
            Try<Snapshot> snapshot = parseJson(request.body(), new TypeReference<Snapshot>() {
            });
            if (snapshot.isSuccess()) {
                SnapshotStorage.getInstance().create(snapshot.get());
            }

            return response;
        }));

        post("/upload/image", (request, response) -> {
            try {
                request.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(System.getProperty("java.io.tmpdir")));

                Part uploadedFile = request.raw().getPart("file");
                byte[] image = toByteArray(uploadedFile.getInputStream());
                if (image.length > 0) {
                    String fileName = uploadedFile.getSubmittedFileName();
                    writeByteArrayToFile(new File(IMAGES_PATH + fileName), image);

                    return fileName;
                }
            } catch (Exception e) {
                logger.error("[POST /upload/image]", e);
            }

            return null;
        });
    }

    private String createDiffReport(String actualDate, String expectedDate, int inaccuracy) {
        String reportJson = "{}";
        String datesHash = makeDatesHash(actualDate, expectedDate);

        try {
            Map<String, DiffSnapshot> diffSnapshots = new ConcurrentHashMap<>();

            Set<String> storiesNames = ConcurrentHashMap.newKeySet();
            Set<String> statesNames = ConcurrentHashMap.newKeySet();

            long start = System.currentTimeMillis();
            List<Document> documents = snapshotStorage.aggregate(actualDate, expectedDate);
            logger.info("[aggregate] " + actualDate + "-" + expectedDate + " documents:" + documents.size() + ", time ms: " + (System.currentTimeMillis() - start));

            List<Future<?>> futureList = new LinkedList<>();

            for (Document d : documents) {
                futureList.add(imagesThreadsPool.submit(() -> {
                    StringBuffer error = new StringBuffer("");

                    List<Document> snapshots = (List) d.get("snapshot");

                    Snapshot snapshot = new Snapshot(snapshots.get(0));
                    String testcaseId = snapshot.getTestcaseId();
                    String story = snapshot.getStory();
                    String state = snapshot.getState();
                    Browser browser = new Browser(snapshot.getDevice(), snapshot.getOsName(),
                            snapshot.getOsVersion(), snapshot.getBrowserName(), snapshot.getBrowserVersion(),
                            snapshot.getResolution(), snapshot.isRetina());
                    String actualUrl = null;
                    String expectedUrl = null;
                    String actualServer = null;
                    String expectedServer = null;
                    String actualBranch = null;
                    String expectedBranch = null;
                    String actualCommit = null;
                    String expectedCommit = null;
                    Mat actualImage = null;
                    Mat expectedImage = null;
                    Pair<Float, String> diffImage = null;
                    String pixels = null;
                    Float diffPercentage = 0.000f;
                    List<DiffElement> diffElements = new ArrayList<>();

                    if (snapshots.size() == 2) {
                        int actualIndex = 1;
                        int expectedIndex = 0;
                        if (snapshot.getDatetime().compareTo(Date.from(Instant.parse(expectedDate))) != 0) {
                            actualIndex = 0;
                            expectedIndex = 1;
                        }

                        Snapshot actualSnapshot = new Snapshot(snapshots.get(actualIndex));
                        Snapshot expectedSnapshot = new Snapshot(snapshots.get(expectedIndex));

                        actualUrl = actualSnapshot.getUrl();
                        expectedUrl = expectedSnapshot.getUrl();

                        actualServer = actualSnapshot.getServer();
                        expectedServer = expectedSnapshot.getServer();

                        actualBranch = actualSnapshot.getBranch();
                        expectedBranch = expectedSnapshot.getBranch();

                        actualCommit = actualSnapshot.getCommit();
                        expectedCommit = expectedSnapshot.getCommit();

                        try {
                            if (actualUrl != null && actualUrl.length() > 0 && new File(IMAGES_PATH + actualUrl).exists()) {
                                actualImage = imread(IMAGES_PATH + actualUrl, CV_LOAD_IMAGE_UNCHANGED);
                            } else {
                                error.append("Image not found: actual " + actualUrl).append("\n");
                            }

                            if (expectedUrl != null && expectedUrl.length() > 0 && new File(IMAGES_PATH + expectedUrl).exists()) {
                                expectedImage = imread(IMAGES_PATH + expectedUrl, CV_LOAD_IMAGE_UNCHANGED);
                            } else {
                                error.append("Image not found: expected " + expectedUrl).append("\n");
                            }

                            if (actualImage != null && expectedImage != null) {
                                diffImage = assertImage(actualImage, expectedImage, browser.isRetina(), inaccuracy, error);

                                boolean image = false;
                                if (diffImage != null && diffImage.getKey() > 0 && diffImage.getValue() != null) {
                                    image = true;

                                    diffPercentage = Float.valueOf(String.format(Locale.US, "%.3f", diffImage.getKey()));
                                    pixels = String.format("%s/images/reports/%s", BACKEND_DOMAIN, diffImage.getValue());
                                }

                                if (actualSnapshot.getElements() != null
                                        && expectedSnapshot.getElements() != null
                                        && actualSnapshot.getElements().length() > 0
                                        && expectedSnapshot.getElements().length() > 0) {
                                    try {
                                        diffElements = getDiffElements(image, actualSnapshot, expectedSnapshot, actualImage, expectedImage, browser.isRetina(), inaccuracy, error);
                                    } catch (Exception e) {
                                        error.append("Could not parse elements").append("\n");
                                    }
                                }
                            } else {
                                error.append("Images not found: actual ").append(actualUrl)
                                        .append(" , expected ").append(expectedUrl)
                                        .append("\n");
                            }
                        } catch (Exception e) {
                            logger.error("[error report] " + actualDate + "-" + expectedDate, e);
                        } finally {
                            if (actualImage != null) {
                                actualImage.release();
                            }

                            if (expectedImage != null) {
                                expectedImage.release();
                            }

                            System.gc();
                        }
                    } else if (snapshots.size() == 1) {
                        String errorFormat = "%s snapshot not found";
                        if (snapshot.getDatetime().compareTo(Date.from(Instant.parse(actualDate))) == 0) {
                            actualUrl = snapshot.getUrl();
                            actualServer = snapshot.getServer();
                            actualBranch = snapshot.getBranch();
                            actualCommit = snapshot.getCommit();

                            error.append(String.format(errorFormat, "expected")).append("\n");
                        } else {
                            expectedUrl = snapshot.getUrl();
                            expectedServer = snapshot.getServer();
                            expectedBranch = snapshot.getBranch();
                            expectedCommit = snapshot.getCommit();

                            error.append(String.format(errorFormat, "actual")).append("\n");
                        }
                    } else if (snapshots.size() == 0) {
                        error.append("Not found snapshots").append("\n");
                    }

                    String actualScreen = (actualUrl != null && actualUrl.length() > 0) ?
                            String.format("%s/images/original/%s", BACKEND_DOMAIN, actualUrl) : null;
                    String expectedScreen = (expectedUrl != null && expectedUrl.length() > 0) ?
                            String.format("%s/images/original/%s", BACKEND_DOMAIN, expectedUrl) : null;

                    DiffSnapshot diffSnapshot = new DiffSnapshot();
                    diffSnapshot.setTestcaseId(testcaseId);
                    diffSnapshot.setDiffPercentage(diffPercentage);
                    diffSnapshot.setPixels(pixels);
                    diffSnapshot.setError(error.toString());
                    diffSnapshot.setBrowser(browser);
                    diffSnapshot.setImages(new Result(actualScreen, expectedScreen));
                    diffSnapshot.setElements(diffElements);
                    diffSnapshot.setServer(new Result(actualServer, expectedServer));
                    diffSnapshot.setBranch(new Result(actualBranch, expectedBranch));
                    diffSnapshot.setCommit(new Result(actualCommit, expectedCommit));

                    storiesNames.add(story);
                    statesNames.add(makeStateHash(story, state));
                    diffSnapshots.put(makeSnapshotHash(story, state, browser), diffSnapshot);
                }));
            }

            for (Future<?> f : futureList) {
                f.get();
            }

            DiffReport diffReport = new DiffReport();

            List<DiffStory> diffStories = new ArrayList<>();
            for (String story : storiesNames) {

                DiffStory diffStory = new DiffStory(story);

                List<DiffState> diffStates = new ArrayList<>();
                List<String> storyStates = statesNames.stream().filter(s -> substringBefore(s, "|&story&|").equals(story)).collect(Collectors.toList());

                for (String storyState : storyStates) {
                    DiffState diffState = new DiffState(substringAfter(storyState, "|&story&|"));

                    List<DiffSnapshot> snapshots = new ArrayList<>();
                    diffSnapshots.forEach((k, v) -> {
                        if (substringBefore(k, "|&state&|").equals(storyState)) {
                            snapshots.add(v);
                        }
                    });

                    diffState.setSnapshots(snapshots);
                    diffStates.add(diffState);
                }

                diffStory.setStates(diffStates);

                diffStories.add(diffStory);
            }

            diffReport.setStories(diffStories);

            Try<String> json = writeAsString(diffReport);
            if (json.isSuccess()) {
                reportJson = json.get();
                saveReport(datesHash, reportJson);
            }
        } catch (Exception e) {
            logger.error("[create report] " + actualDate + "-" + expectedDate, e);
        }

        return reportJson;
    }

    private List<DiffElement> getDiffElements(boolean image,
                                              Snapshot actualElements, Snapshot expectedElements,
                                              Mat actualImage, Mat expectedImage,
                                              boolean isRetina,
                                              int inaccuracy,
                                              StringBuffer error) {
        Try<HashMap<String, Element>> actualTry = parseJson(actualElements.getElements(), new TypeReference<HashMap<String, Element>>() {});
        Try<HashMap<String, Element>> expectedTry = parseJson(expectedElements.getElements(), new TypeReference<HashMap<String, Element>>() {});

        if (actualTry.isSuccess() && expectedTry.isSuccess()) {
            HashMap<String, Element> actual = actualTry.get();
            HashMap<String, Element> expected = expectedTry.get();

            return compare(image, actual, expected, actualImage, expectedImage, isRetina, inaccuracy, error);
        } else {
            error.append("Could not parse elements json: actual " + actualElements.getElements()).append("\n")
                    .append("expected " + expectedElements.getElements()).append("\n");
            return new ArrayList<>();
        }
    }

    private String makeSnapshotHash(String story, String state, DiffSnapshot.Browser browser) {
        return String.format("%s|&story&|%s|&state&|%s", story, state, browser.toString());
    }

    private String makeStateHash(String story, String state) {
        return String.format("%s|&story&|%s", story, state);
    }

    private String makeDatesHash(String actualDate, String expectedDate) {
        StringBuilder hash = new StringBuilder();
        hash.append(actualDate.replaceAll(":", "-").replaceAll("\\.", "-"))
                .append("_")
                .append(expectedDate.replaceAll(":", "-").replaceAll("\\.", "-"));

        return hash.toString();
    }

    public static String saveReport(String datesHash, String json) {
        if (json != null) {
            try {
                String fileName = datesHash + ".json";
                Files.write(Paths.get(REPORTS_PATH, fileName), json.getBytes("UTF-8"));
                return fileName;
            } catch (Exception e) {
                logger.error("[save report]", e);
            }
        }

        return "";
    }
}