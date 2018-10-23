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

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.*;
import org.openqa.selenium.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.coordinates.WebDriverCoordsProvider;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;
import ru.yandex.qatools.ashot.shooting.cutter.VariableCutStrategy;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static javax.imageio.ImageIO.read;
import static org.openqa.selenium.OutputType.FILE;
import static org.openqa.selenium.Platform.WINDOWS;
import static ru.tinkoff.WebDriverCapabilities.*;
import static ru.tinkoff.WebDriverUtils.moveTo;
import static ru.yandex.qatools.ashot.shooting.ShootingStrategies.*;

public class ScreenShoter {

    private static final int SCREEN_SCROLL_TIMEOUT = Integer.parseInt(System.getProperty("screen.scroll.timeout"));
    private static final int RESIZE_TIMEOUT = Integer.parseInt(System.getProperty("resize.timeout"));

    private static final Logger logger = LoggerFactory.getLogger(ScreenShoter.class);

    public static void resizeBrowser(String resolution, WebDriver driver) {
        try {
            if (DEVICE_NAME.isEmpty()) {
                setDriverPosition(driver);

                if (resolution.isEmpty()) {
                    if (Platform.fromString(PLATFORM_NAME).is(WINDOWS)) {
                        driver.manage().window().maximize();
                    } else {
                        java.awt.Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                        driver.manage().window().setSize(new Dimension((int) screenSize.getWidth(), (int) screenSize.getHeight()));
                    }
                } else {
                    int width = parseInt(resolution.split("x")[0]);

                    if (parseInt(String.valueOf(((JavascriptExecutor) driver).executeScript("return window.outerWidth;"))) != width) {
                        driver.manage().window().setSize(new Dimension(width, parseInt(resolution.split("x")[1])));
                    }
                }
            }

            // Timeout to update render of a web-page
            MILLISECONDS.sleep(RESIZE_TIMEOUT);
        } catch (Exception e) {
            logger.error("[resize browser]", e);
        }
    }

    public BufferedImage takeScreen(boolean fullScreen, WebDriver driver) {
        try {
            moveTo(0, 0, driver);

            switch (BROWSER_NAME.toLowerCase()) {
                case "chrome": {
                    if (fullScreen) {
                        return read(getScreenshotFull(driver, FILE));
                    } else {
                        return read(((TakesScreenshot) driver).getScreenshotAs(FILE));
                    }
                }
                case "firefox": {
                    if (fullScreen) {
                        ((JavascriptExecutor) driver).executeScript("return window.scrollTo(0, 0);");

                        return new AShot().coordsProvider(new WebDriverCoordsProvider())
                                .shootingStrategy(getStrategy())
                                .takeScreenshot(driver)
                                .getImage();
                    } else {
                        return read(((TakesScreenshot) driver).getScreenshotAs(FILE));
                    }
                }
                case "safari": {
                    return read(((TakesScreenshot) driver).getScreenshotAs(FILE));
                }
                case "ie":
                case "internet explorer":
                case "edge": {
                    return read(((TakesScreenshot) driver).getScreenshotAs(FILE));
                }
                default: {
                    return new AShot().coordsProvider(new WebDriverCoordsProvider())
                            .shootingStrategy(getStrategy())
                            .takeScreenshot(driver)
                            .getImage();
                }
            }
        } catch (Exception e) {
            logger.error("[take screenshot]", e);
        }

        return null;
    }

    private static ShootingStrategy getStrategy() {
        ShootingStrategy strategy = null;

        if (!DEVICE_NAME.isEmpty()) {
            switch (DEVICE_NAME) {
                case "iPhone 6S":
                    strategy = ShootingStrategies.viewportRetina(SCREEN_SCROLL_TIMEOUT, new VariableCutStrategy(41, 65, 45, 100), 2F);
                    break;
                case "iPad 2 (5.0)":
                    strategy = iPad2WithIOS8Simulator();
                    break;
                default:
                    throw new RuntimeException("Unknown device name: " + DEVICE_NAME);
            }
        } else {
            if (BROWSER_RETINA) {
                strategy = viewportPasting(scaling(2), SCREEN_SCROLL_TIMEOUT);
            } else {
                strategy = viewportPasting(SCREEN_SCROLL_TIMEOUT);
            }
        }

        return strategy;
    }

    // The zero driver position is (X, Y) = (0, 23)
    private static void setDriverPosition(WebDriver driver) {
        Point driverPosition = driver.manage().window().getPosition();
        if (driverPosition.getX() > 0 || driverPosition.getY() > 23) {
            driver.manage().window().setPosition(new Point(0, 0));
        }
    }

    private <X> X getScreenshotFull(WebDriver driver, OutputType<X> outputType) throws WebDriverException {
        Object metrics = sendEvaluate(driver,
                "({" +
                        "width: Math.max(window.innerWidth,document.body.scrollWidth,document.documentElement.scrollWidth)|0," +
                        "height: Math.max(window.innerHeight,document.body.scrollHeight,document.documentElement.scrollHeight)|0," +
                        "deviceScaleFactor: window.devicePixelRatio || 1," +
                        "mobile: typeof window.orientation !== 'undefined'" +
                        "})");
        sendCommand(driver, "Emulation.setDeviceMetricsOverride", metrics);
        Object result = sendCommand(driver, "Page.captureScreenshot", ImmutableMap.of("format", "png", "fromSurface", true));
        sendCommand(driver, "Emulation.clearDeviceMetricsOverride", ImmutableMap.of());
        String base64EncodedPng = (String) ((Map<String, ?>) result).get("data");
        return outputType.convertFromBase64Png(base64EncodedPng);
    }

    private Object sendCommand(WebDriver driver, String cmd, Object params) {
        if (driver instanceof RemoteWebDriverEx) {
            return ((RemoteWebDriverEx) driver).sendCommand(cmd, params);
        } else if (driver instanceof ChromeDriverEx) {
            return ((ChromeDriverEx) driver).sendCommand(cmd, params);
        }

        return null;
    }

    private Object sendEvaluate(WebDriver driver, String script) {
        Object response = sendCommand(driver, "Runtime.evaluate", ImmutableMap.of("returnByValue", true, "expression", script));
        Object result = ((Map<String, ?>) response).get("result");
        return ((Map<String, ?>) result).get("value");
    }
}