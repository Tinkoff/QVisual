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

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static ru.tinkoff.ParserUtils.readFile;

public class WebDriverUtils {

    private static final Logger logger = LoggerFactory.getLogger(WebDriverUtils.class);

    public static String getElements(String elements, WebDriver driver) {
        try {
            return (String) ((JavascriptExecutor) driver).executeScript("var locators = \"" + elements + "\";\n" + readFile("getElements.js"));
        } catch (Exception e) {
            logger.error("[execute script] getElements.js", e);
        }

        return null;
    }

    public static void moveTo(int x, int y, WebDriver driver) {
        new Actions(driver).moveByOffset(x, y);
    }

    public static void changeVisibility(boolean displayNone, String selectors, WebDriver driver) {
        try {
            if (selectors != null && selectors.length() > 0) {
                ((JavascriptExecutor) driver).executeScript("var displayNone = " + displayNone + ";\n" +
                        "var selectors = \"" + selectors + "\";\n" +
                        readFile("hideElements.js"));
                MILLISECONDS.sleep(100);
            }
        } catch (Exception e) {
            logger.error("[execute script] hideElements.js", e);
        }
    }
}