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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

public class ParserUtils {

    private static final Logger logger = LoggerFactory.getLogger(ParserUtils.class);

    public static <T> Try<T> parseJson(String json, TypeReference<T> type) {
        return (Try<T>) Try.of(() -> getMapper().readValue(json, type)).onFailure(t -> logger.error("[parse json]", t));
    }

    public static Try<String> writeAsString(Object json) {
        return Try.of(() -> getMapper().writeValueAsString(json)).onFailure(t -> logger.error("[write json]", t));
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.disable(FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(NON_NULL);

        return mapper;
    }

    public static String readFile(String path) {
        try {
            return Resources.toString(Resources.getResource(path), Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error("[reading file] by path: " + path, e);
        }

        return null;
    }

    public static List<String> readLines(String path) {
        try {
            return Resources.readLines(Resources.getResource(path), Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error("[reading file] by path: " + path, e);
        }

        return new ArrayList<>();
    }
}