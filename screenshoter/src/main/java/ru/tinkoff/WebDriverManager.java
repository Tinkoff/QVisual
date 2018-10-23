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
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.CommandInfo;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.safari.SafariDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URL;

import static org.openqa.selenium.remote.http.HttpMethod.POST;
import static org.testng.Assert.fail;

public class WebDriverManager {

    private static final Logger logger = LoggerFactory.getLogger(WebDriverManager.class);

    public static WebDriver getDriver() {
        return getDriver(new DesiredCapabilities());
    }

    public static WebDriver getDriver(DesiredCapabilities driverCapabilities) {
        WebDriver driver = null;

        try {
            WebDriverCapabilities.getInstance().getProperties().forEach(System::setProperty);

            DesiredCapabilities capabilities = WebDriverCapabilities.getInstance().getCapabilities();
            driverCapabilities.asMap().forEach(capabilities::setCapability);

            URL hubUrl = WebDriverCapabilities.getInstance().getHubUrl();

            if (hubUrl != null) {
                driver = new RemoteWebDriverEx(hubUrl, capabilities);

                if (capabilities.getBrowserName().toLowerCase().equals("chrome")) {
                    setCommand(((RemoteWebDriverEx) driver).getCommandExecutor());
                }
            } else {
                switch (capabilities.getBrowserName().toLowerCase()) {
                    case "chrome": {
                        driver = new ChromeDriverEx(capabilities);
                        setCommand(((ChromeDriverEx) driver).getCommandExecutor());
                        break;
                    }
                    case "firefox": {
                        driver = new FirefoxDriver(capabilities);
                        break;
                    }
                    case "safari": {
                        driver = new SafariDriver(capabilities);
                        break;
                    }
                    case "internet explorer": {
                        driver = new InternetExplorerDriver(capabilities);
                        break;
                    }
                    case "edge": {
                        driver = new EdgeDriver(capabilities);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            closeDriver(driver);
            fail("Error on starting WebDriver: " + e.getMessage());
        }

        return driver;
    }

    public static void closeDriver(WebDriver driver) {
        if (driver != null) {
            driver.quit();
        }
    }

    private static void setCommand(CommandExecutor executor) throws Exception {
        CommandInfo cmd = new CommandInfo("/session/:sessionId/chromium/send_command_and_get_result", POST);
        Method defineCommand = HttpCommandExecutor.class.getDeclaredMethod("defineCommand", String.class, CommandInfo.class);
        defineCommand.setAccessible(true);
        defineCommand.invoke(executor, "sendCommand", cmd);
    }
}