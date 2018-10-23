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

import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.tinkoff.objects.Snapshot;

import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static ru.tinkoff.ParserUtils.readLines;
import static ru.tinkoff.SnapShoter.snapshot;
import static ru.tinkoff.WebDriverManager.closeDriver;
import static ru.tinkoff.WebDriverManager.getDriver;

public class OpenUrlsTests {

    private static ThreadLocal<WebDriver> driverPool = new ThreadLocal<>();

    @BeforeMethod(alwaysRun = true)
    public void beforeMethod() {
        driverPool.set(getDriver());
    }

    @AfterMethod(alwaysRun = true)
    public void afterMethod() {
        try {
            closeDriver(driverPool.get());
        } finally {
            driverPool.remove();
        }
    }

    @Test(dataProvider = "urls")
    public void demo(String path, List<String> locators) throws Exception {
        driverPool.get().get(System.getProperty("domain") + path);
        SECONDS.sleep(1);
        snapshot(locators, new Snapshot(null, path, "Default page"), driverPool.get());
    }

    @DataProvider(name = "urls", parallel = true)
    public Object[][] urls() {
        List<String> lines = readLines("urls.txt");

        Object[][] objectArray = new Object[lines.size()][2];
        for (int i = 0; i < lines.size(); i++) {
            String[] line = lines.get(i).split("\\|\\|");

            objectArray[i][0] = line[0];
            objectArray[i][1] = readLines(line[1]);
        }

        return objectArray;
    }
}