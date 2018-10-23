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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.util.Pair;
import org.opencv.core.Mat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.objects.DiffElement;
import ru.tinkoff.objects.Displayed;
import ru.tinkoff.objects.Element;
import ru.tinkoff.objects.Result;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static com.github.wnameless.json.flattener.JsonFlattener.flattenAsMap;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static ru.tinkoff.HttpUtils.BACKEND_DOMAIN;
import static ru.tinkoff.ScreensComparator.compareScreens;
import static ru.tinkoff.objects.Displayed.*;

public class ElementsComparator {

    private static final Logger logger = LoggerFactory.getLogger(ElementsComparator.class);

    private static final ForkJoinPool elementsThreadsPool = new ForkJoinPool(100);

    private static final Set<String> IGNORE_CSS = new HashSet<String>() {{
    }};

    private static final Set<String> IGNORE_ATTRIBUTES = new HashSet<String>() {{
        add("id");
        add("data-reactid");
        add("data-qa-file");
        add("data-qa-node");
    }};

    public static List<DiffElement> compare(boolean image,
                                            HashMap<String, Element> actual,
                                            HashMap<String, Element> expected,
                                            Mat actualImage,
                                            Mat expectedImage,
                                            boolean isRetina,
                                            int inaccuracy,
                                            StringBuffer error) {
        List<DiffElement> diffElements = new ArrayList<>();

        try {

            Set<String> intersectionNames = intersection(actual.keySet(), expected.keySet());
            if (!intersectionNames.isEmpty()) {
                List<Future<?>> futureList = new LinkedList<>();
                for (String elementName : intersectionNames) {
                    futureList.add(elementsThreadsPool.submit(() -> {
                        Element actualElement = actual.get(elementName);
                        Element expectedElement = expected.get(elementName);
                        Displayed displayed = assertDisplay(actualElement, expectedElement, actualImage, expectedImage, isRetina);

                        DiffElement diffElement = new DiffElement();
                        diffElement.setName(elementName);
                        diffElement.setDisplay(displayed.name().toLowerCase().replaceAll("_", " "));

                        switch (displayed) {
                            case ADDED: {
                                diffElement.setArea(new DiffElement.Area(actualElement, new Element()));
                                diffElements.add(diffElement);
                                break;
                            }
                            case REMOVED: {
                                diffElement.setArea(new DiffElement.Area(new Element(), expectedElement));
                                diffElements.add(diffElement);
                                break;
                            }
                            case SHOULD_BE_DISPLAYED:
                            case SHOULD_NOT_BE_DISPLAYED: {
                                diffElement.setArea(new DiffElement.Area(actualElement, expectedElement));
                                diffElements.add(diffElement);
                                break;
                            }
                            case DISPLAYED: {
                                diffElement.setArea(new DiffElement.Area(actualElement, expectedElement));

                                if (image) {
                                    Pair<Float, String> imageDiff = assertImage(actualImage, expectedImage,
                                            actualElement.getArea(), expectedElement.getArea(),
                                            isRetina,
                                            inaccuracy,
                                            error);

                                    if (imageDiff != null && imageDiff.getKey() > 0 && imageDiff.getValue() != null) {
                                        diffElement.addDiff("image");
                                        diffElement.setImage(String.format("%s/images/reports/%s", BACKEND_DOMAIN, imageDiff.getValue()));
                                    }
                                }

                                Map<String, Object> actualFlatten = toFlattenMap(actualElement);
                                Map<String, Object> expectedFlatten = toFlattenMap(expectedElement);

                                Set<String> actualKeys = actualFlatten.keySet();
                                Set<String> expectedKeys = expectedFlatten.keySet();

                                Set<String> notEqual = new HashSet<>(intersection(actualKeys, expectedKeys));
                                if (!notEqual.isEmpty()) {
                                    Set<String> equalKeys = new HashSet<>();
                                    for (String k : notEqual) {
                                        if (actualFlatten.get(k).equals(expectedFlatten.get(k))) {
                                            equalKeys.add(k);
                                        }
                                    }
                                    notEqual.removeAll(equalKeys);

                                    if (!notEqual.isEmpty()) {
                                        if (notEqual.contains("area.left") ||
                                                notEqual.contains("area.top") ||
                                                notEqual.contains("area.right") ||
                                                notEqual.contains("area.bottom")) {
                                            diffElement.addDiff("moved");
                                        }

                                        if (notEqual.contains("area.width") || notEqual.contains("area.height")) {
                                            diffElement.addDiff("resized");
                                        }

                                        if (notEqual.contains("text")) {
                                            diffElement.addDiff("text");
                                            diffElement.setText(new Result(actualElement.getText(), expectedElement.getText()));
                                        }
                                    }
                                }

                                Set<String> added = new HashSet<>(difference(actualKeys, expectedKeys));
                                Set<String> removed = new HashSet<>(difference(expectedKeys, actualKeys));

                                Set<String> all = new HashSet<>();
                                if (!notEqual.isEmpty()) {
                                    all.addAll(notEqual);
                                }

                                if (!added.isEmpty()) {
                                    all.addAll(added);
                                }

                                if (!removed.isEmpty()) {
                                    all.addAll(removed);
                                }

                                HashMap<String, Result> cssDiff = new HashMap<>();
                                Set<String> css = all.stream().filter(n -> n.startsWith("css.")).collect(Collectors.toCollection(HashSet::new));
                                if (!css.isEmpty()) {
                                    for (String k : css) {
                                        String name = substringAfter(k, "css.");
                                        if (!IGNORE_CSS.contains(name)) {
                                            if (notEqual.contains(k)) {
                                                cssDiff.put(name, new Result(actualFlatten.get(k), expectedFlatten.get(k)));
                                            } else if (added.contains(k)) {
                                                cssDiff.put(name, new Result("[value added]", expectedFlatten.get(k)));
                                            } else if (removed.contains(k)) {
                                                cssDiff.put(name, new Result("[value removed]", expectedFlatten.get(k)));
                                            }
                                        }
                                    }

                                    if (cssDiff.size() > 0) {
                                        diffElement.addDiff("css");
                                        diffElement.setCss(cssDiff);
                                    }
                                }

                                HashMap<String, Result> attDiff = new HashMap<>();
                                Set<String> attributes = all.stream().filter(n -> n.startsWith("attributes.")).collect(Collectors.toCollection(HashSet::new));
                                if (!attributes.isEmpty()) {
                                    for (String k : attributes) {
                                        String name = substringAfter(k, "attributes.");
                                        if (!IGNORE_ATTRIBUTES.contains(name)) {
                                            if (notEqual.contains(k)) {
                                                attDiff.put(name, new Result(actualFlatten.get(k), expectedFlatten.get(k)));
                                            } else if (added.contains(k)) {
                                                attDiff.put(name, new Result("[value added]", expectedFlatten.get(k)));
                                            } else if (removed.contains(k)) {
                                                attDiff.put(name, new Result("[value removed]", expectedFlatten.get(k)));
                                            }
                                        }
                                    }

                                    if (attDiff.size() > 0) {
                                        diffElement.addDiff("attributes");
                                        diffElement.setAttributes(attDiff);
                                    }
                                }

                                if (diffElement.getDiff().size() > 0) {
                                    diffElements.add(diffElement);
                                }

                                break;
                            }
                            case NOT_FOUND: {
                                diffElements.add(diffElement);
                                break;
                            }
                            case NOT_DISPLAYED: {
                                break;
                            }
                        }
                    }));
                }

                for (Future<?> f : futureList) {
                    f.get();
                }
            }

            Set<String> addedNames = difference(actual.keySet(), expected.keySet());
            if (!addedNames.isEmpty()) {
                for (String elementAdded : addedNames) {
                    DiffElement diffElement = new DiffElement();
                    diffElement.setName(elementAdded);
                    diffElement.setDisplay(ADDED.name().toLowerCase().replaceAll("_", " "));
                    diffElement.setArea(new DiffElement.Area(actual.get(elementAdded), new Element()));

                    diffElements.add(diffElement);
                }
            }

            Set<String> removedNames = difference(expected.keySet(), actual.keySet());
            if (!removedNames.isEmpty()) {
                for (String elementRemoved : removedNames) {
                    DiffElement diffElement = new DiffElement();
                    diffElement.setName(elementRemoved);
                    diffElement.setDisplay(REMOVED.name().toLowerCase().replaceAll("_", " "));
                    diffElement.setArea(new DiffElement.Area(new Element(), expected.get(elementRemoved)));

                    diffElements.add(diffElement);
                }
            }
        } catch (Exception e) {
            error.append(System.currentTimeMillis() + "Could not compare elements: " + e.getMessage()).append("\n");
            logger.error("[compare elements]", e);
        }

        return newArrayList(diffElements.iterator());
    }

    public static boolean isDisplayed(Element element, Mat image, boolean isRetina) {
        boolean isDisplayed = !element.getDisplay().equals("none") && element.getArea().getWidth() != 0 && element.getArea().getHeight() != 0;

        if (isDisplayed) {
            int index = (isRetina) ? 2 : 1;

            isDisplayed = (element.getArea().getLeft() * index + element.getArea().getWidth() * index <= image.size().width) &&
                    (element.getArea().getTop() * index + element.getArea().getHeight() * index <= image.size().height);
        }

        return isDisplayed;
    }

    private static Displayed assertDisplay(Element actual, Element expected, Mat actualImage, Mat expectedImage, boolean isRetina) {
        if (expected.getDisplay().equals("not found")) {
            if (actual.getDisplay().equals("not found")) {
                return NOT_FOUND;
            } else {
                return ADDED;
            }
        } else {
            if (actual.getDisplay().equals("not found")) {
                return REMOVED;
            } else {
                boolean displayExpected = isDisplayed(expected, expectedImage, isRetina);
                boolean displayActual = isDisplayed(actual, actualImage, isRetina);

                if (displayActual != displayExpected) {
                    if (displayExpected) {
                        return SHOULD_BE_DISPLAYED;
                    } else {
                        return SHOULD_NOT_BE_DISPLAYED;
                    }
                } else {
                    if (displayExpected) {
                        return DISPLAYED;
                    } else {
                        return NOT_DISPLAYED;
                    }
                }
            }
        }
    }

    private static Pair<Float, String> assertImage(Mat actualImage, Mat expectedImage,
                                                   Element.Area actualArea, Element.Area expectedArea,
                                                   boolean isRetina,
                                                   int inaccuracy,
                                                   StringBuffer error) {
        try {
            return compareScreens(actualImage, expectedImage, actualArea, expectedArea, true, isRetina, inaccuracy, error);
        } catch (Exception e) {
            error.append(System.currentTimeMillis() + "Could not compare elements images: " + e.getMessage()).append("\n");
            logger.error("[compare screens]", e);
            return null;
        }
    }

    public static Pair<Float, String> assertImage(Mat actualImage, Mat expectedImage,
                                                  boolean isRetina,
                                                  int inaccuracy,
                                                  StringBuffer error) {
        try {
            return compareScreens(actualImage, expectedImage, null, null, false, isRetina, inaccuracy, error);
        } catch (Exception e) {
            error.append(System.currentTimeMillis() + "Could not compare images: " + e.getMessage()).append("\n");
            logger.error("[compare screens]", e);
            return null;
        }
    }

    private static HashMap<String, Integer> getWordsFrequency(Element element) {
        List<String> words = new ArrayList<>(asList(element.getText()
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .split(" ")));

        HashMap<String, Integer> wordsAndCount = new HashMap<>();
        new HashSet<>(words).forEach(w -> wordsAndCount.put(w, Collections.frequency(words, w)));

        return wordsAndCount;
    }

    private static ObjectMapper createJsonMapper() {
        return new ObjectMapper()
                .disable(FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(FAIL_ON_EMPTY_BEANS)
                .enable(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setSerializationInclusion(NON_NULL)
                .configure(WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private static Map<String, Object> toFlattenMap(Object pojo) {
        try {
            return flattenAsMap(createJsonMapper().writeValueAsString(pojo));
        } catch (JsonProcessingException e) {
            throw new AssertionError(e.getMessage());
        }
    }
}