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

import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import static com.google.common.base.Strings.isNullOrEmpty;
import static io.github.bonigarcia.wdm.WebDriverManager.*;
import static java.lang.Boolean.parseBoolean;
import static java.util.logging.Level.ALL;
import static org.apache.commons.io.FileUtils.forceMkdir;
import static org.openqa.selenium.logging.LogType.DRIVER;
import static org.openqa.selenium.logging.LogType.PERFORMANCE;
import static org.openqa.selenium.remote.CapabilityType.ACCEPT_SSL_CERTS;
import static org.openqa.selenium.remote.CapabilityType.LOGGING_PREFS;

public class WebDriverCapabilities {

    public static final String DRIVER_SERVICE = System.getProperty("driver.service");
    public static final boolean ENABLE_VNC = parseBoolean(System.getProperty("enableVNC"));
    public static final boolean ENABLE_VIDEO = parseBoolean(System.getProperty("enableVideo"));
    public static final String WEBDRIVER_REMOTE_URL = System.getProperty("webdriver.remote.url");
    public static final String DEVICE_NAME = System.getProperty("device");
    public static final String PLATFORM_NAME = isNullOrEmpty(System.getProperty("platform.name")) ? Platform.getCurrent().name() : System.getProperty("platform.name");
    public static final String PLATFORM_VERSION = System.getProperty("platform.version");
    public static final String BROWSER_NAME = System.getProperty("browser.name");
    public static final String BROWSER_VERSION = System.getProperty("browser.version");
    public static final String BROWSER_RESOLUTION = System.getProperty("browser.resolution");
    public static final String SCREEN_RESOLUTIONS = System.getProperty("screen.resolutions");
    public static final boolean BROWSER_RETINA = parseBoolean(System.getProperty("browser.retina"));
    public static final boolean BROWSER_HEADLESS = parseBoolean(System.getProperty("browser.headless"));
    public static final String BROWSERSTACK_URL = System.getProperty("browserstack.url");
    public static final String BROWSERSTACK_SELENIUM_VERSION = System.getProperty("browserstack.selenium.version");
    public static final String BROWSERSTACK_APPIUM_VERSION = System.getProperty("browserstack.appium_version");
    public static final String BROWSERSTACK_DEVICE_ORIENTATION = System.getProperty("browserstack.device.orientation");
    public static final boolean BROWSERSTACK_REAL_MOBILE = parseBoolean(System.getProperty("realMobile"));
    public static final String DOWNLOAD_DIRECTORY = System.getProperty("download_dir.path");
    public static final String WEBDRIVER_DIRECTORY = System.getProperty("webdriver.dir");

    private static final Logger logger = LoggerFactory.getLogger(WebDriverCapabilities.class);

    private URL HUB_URL = null;
    private HashMap<String, String> properties;
    private DesiredCapabilities capabilities;

    private static WebDriverCapabilities instance = null;

    public static WebDriverCapabilities getInstance() {
        if (instance == null) {
            instance = new WebDriverCapabilities();
        }

        return instance;
    }

    private WebDriverCapabilities() {
        try {
            if (!isNullOrEmpty(DOWNLOAD_DIRECTORY)) {
                forceMkdir(new File(DOWNLOAD_DIRECTORY));
            }
        } catch (IOException e) {
            logger.error("[create directory for downloaded files by path] " + DOWNLOAD_DIRECTORY, e);
        }

        capabilities = new DesiredCapabilities();
        setupCapabilities();

        capabilities.setCapability(LOGGING_PREFS, setupLogging());

        properties = new HashMap<>();
        setupProperties();

        String hubUrl = null;
        if (properties.keySet().contains("browserstack.url")) {
            hubUrl = properties.get("browserstack.url");
        } else if (properties.keySet().contains("webdriver.remote.url")) {
            hubUrl = properties.get("webdriver.remote.url");
        }

        if (hubUrl != null) {
            try {
                HUB_URL = new URL(hubUrl);
            } catch (MalformedURLException e) {
                logger.error("[create HUB for URL] " + hubUrl, e);
            }
        }
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public DesiredCapabilities getCapabilities() {
        return capabilities;
    }

    public URL getHubUrl() {
        return HUB_URL;
    }

    private void setupProperties() {
        switch (DRIVER_SERVICE) {
            case "grid": {
                setupGrid();
                break;
            }
            case "selenoid": {
                setupSelenoid();
                break;
            }
            case "browserstack": {
                setupBrowserstack();
                break;
            }
            default: {
                String targetPath = System.getProperty("wdm.targetPath", WEBDRIVER_DIRECTORY);
                config().setTargetPath(targetPath);

                switch (BROWSER_NAME.toLowerCase()) {
                    case "chrome": {
                        chromedriver().setup();
                        break;
                    }
                    case "firefox": {
                        firefoxdriver().setup();
                        break;
                    }
                    case "internet explorer":
                        iedriver().setup();
                        break;
                    case "edge":
                        edgedriver().setup();
                        break;
                }

                break;
            }
        }
    }

    private void setupBrowserstack() {
        properties.put("browserstack.url", BROWSERSTACK_URL);

        capabilities.setCapability("browserstack.selenium.version", BROWSERSTACK_SELENIUM_VERSION);
        capabilities.setCapability("browserstack.acceptSslCerts", "true");
        capabilities.setCapability("browserstack.local", "true");

        if (!DEVICE_NAME.isEmpty()) {
            capabilities.setCapability("browserstack.appium_version", BROWSERSTACK_APPIUM_VERSION);
            capabilities.setCapability("browserstack.device.orientation", BROWSERSTACK_DEVICE_ORIENTATION);
            capabilities.setCapability("browserstack.device", DEVICE_NAME);

            if (!BROWSERSTACK_REAL_MOBILE) {
                capabilities.setCapability("browserstack.platform", PLATFORM_NAME);
                capabilities.setCapability("browserstack.browserName", BROWSER_NAME);
            }
        } else {
            capabilities.setCapability("browserstack.os", PLATFORM_NAME);
            capabilities.setCapability("browserstack.os.version", PLATFORM_VERSION);
            capabilities.setCapability("browserstack.browser", BROWSER_NAME);
            capabilities.setCapability("browserstack.browser.version", BROWSER_VERSION);
            capabilities.setCapability("browserstack.resolution", BROWSER_RESOLUTION);
        }
    }

    private void setupSelenoid() {
        properties.put("webdriver.remote.url", WEBDRIVER_REMOTE_URL);
        properties.put("webdriver.remote.driver", BROWSER_NAME);
        properties.put("webdriver.remote.driver.version", BROWSER_VERSION);
        properties.put("webdriver.remote.os", PLATFORM_NAME);
    }

    private void setupGrid() {
        properties.put("webdriver.remote.url", WEBDRIVER_REMOTE_URL);
        properties.put("webdriver.remote.driver", BROWSER_NAME);
        properties.put("webdriver.remote.driver.version", BROWSER_VERSION);
        properties.put("webdriver.remote.os", PLATFORM_NAME);
    }

    private DesiredCapabilities setupCapabilities() {
        if (DEVICE_NAME.isEmpty()) {
            capabilities.setCapability(ACCEPT_SSL_CERTS, true);
            capabilities.setBrowserName(BROWSER_NAME);

            switch (BROWSER_NAME) {
                case "firefox":
                    setupFirefox();
                    break;
                case "chrome":
                    setupChrome();
                    break;
                case "safari":
                    setupSafari();
                    break;
                case "internet explorer":
                    setupIE();
                    break;
                case "edge":
                    setupEdge();
                    break;
            }

            switch (DRIVER_SERVICE) {
                case "selenoid": {
                    capabilities.setVersion(BROWSER_VERSION);
                    capabilities.setCapability("enableVNC", ENABLE_VNC);
                    capabilities.setCapability("enableVideo", ENABLE_VIDEO);
                    break;
                }
            }
        }

        return capabilities;
    }

    private LoggingPreferences setupLogging() {
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(DRIVER, ALL);
        logPrefs.enable(PERFORMANCE, ALL);

        return logPrefs;
    }

    private void setupFirefox() {
        FirefoxProfile firefoxProfile = new FirefoxProfile();
        firefoxProfile.setPreference("dom.webnotifications.enabled", false);
        firefoxProfile.setAcceptUntrustedCertificates(true);
        firefoxProfile.setPreference("app.update.enabled", false);
        firefoxProfile.setPreference("devtools.devedition.promo.url", "");
        firefoxProfile.setPreference("xpinstall.signatures.required", false);
        firefoxProfile.setPreference("browser.startup.homepage;about:home", "about:blank");
        firefoxProfile.setPreference("browser.startup.homepage_override.mstone", "ignore");
        firefoxProfile.setPreference("browser.usedOnWindows10", false);
        firefoxProfile.setPreference("browser.usedOnWindows10.introURL", "about:blank");
        firefoxProfile.setPreference("startup.homepage_welcome_url", "about:blank");
        firefoxProfile.setPreference("startup.homepage_welcome_url.additional", "about:blank");
        firefoxProfile.setPreference("browser.helperApps.alwaysAsk.force", false);
        firefoxProfile.setPreference("browser.download.folderList", 2);
        firefoxProfile.setPreference("browser.download.manager.showWhenStarting", false);
        firefoxProfile.setPreference("browser.download.manager.useWindow", false);
        firefoxProfile.setPreference("browser.download.manager.alertOnEXEOpen", false);
        firefoxProfile.setPreference("browser.download.dir", DOWNLOAD_DIRECTORY);
        firefoxProfile.setPreference("browser.download.manager.focusWhenStarting", false);
        firefoxProfile.setPreference("browser.download.useDownloadDir", true);
        firefoxProfile.setPreference("browser.download.manager.closeWhenDone", true);
        firefoxProfile.setPreference("browser.download.manager.showAlertOnComplete", false);
        firefoxProfile.setPreference("browser.download.panel.shown", false);
        firefoxProfile.setPreference("services.sync.prefs.sync.browser.download.manager.showWhenStarting", false);
        firefoxProfile.setPreference("pdfjs.disabled", true);
        firefoxProfile.setPreference("plugin.scan.Acrobat", "99.0");
        firefoxProfile.setPreference("plugin.scan.plid.all", false);
        firefoxProfile.setPreference("browser.helperApps.neverAsk.saveToDisk", "application/x-msdownload," +
                "application/AppleLink," +
                "application/x-newton-compatible-pkg," +
                "image/png," +
                "application/ris," +
                "text/csv," +
                "text/xml," +
                "text/html," +
                "text/plain," +
                "application/xml," +
                "application/zip," +
                "application/x-zip," +
                "application/x-zip-compressed," +
                "application/download," +
                "application/octet-stream," +
                "application/excel," +
                "application/vnd.ms-excel," +
                "application/x-excel," +
                "application/x-msexcel," +
                "application/vnd.openxmlformats-officedocument.spreadsheetml.template," +
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet," +
                "application/msword," +
                "application/csv," +
                "application/pdf," +
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document," +
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template");

        FirefoxOptions firefoxOptions = new FirefoxOptions();
        firefoxOptions.setProfile(firefoxProfile);
        firefoxOptions.setHeadless(BROWSER_HEADLESS);

        capabilities.merge(firefoxOptions);
    }

    /**
     * List of Chromium Command Line Switches
     * http://peter.sh/experiments/chromium-command-line-switches/#disable-popup-blocking
     */
    private void setupChrome() {
        HashMap<String, Object> prefs = new HashMap<>();
        prefs.put("profile.default_content_setting_values.notifications", 2);
        prefs.put("profile.default_content_settings.popups", 0);
        prefs.put("download.default_directory", DOWNLOAD_DIRECTORY);
        prefs.put("download.prompt_for_download", false);
        prefs.put("profile.content_settings.pattern_pairs.*.multiple-automatic-downloads", 1);
        prefs.put("safebrowsing.enabled", true);
        prefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setExperimentalOption("useAutomationExtension", false);
        chromeOptions.setExperimentalOption("prefs", prefs);
        chromeOptions.addArguments("allow-file-access");
        chromeOptions.addArguments("allow-file-access-from-files");
        chromeOptions.addArguments("disable-background-networking");
        chromeOptions.addArguments("disable-background-timer-throttling");
        chromeOptions.addArguments("disable-breakpad");
        chromeOptions.addArguments("disable-child-account-detection");
        chromeOptions.addArguments("disable-clear-browsing-data-counters");
        chromeOptions.addArguments("disable-client-side-phishing-detection");
        chromeOptions.addArguments("disable-cloud-import");
        chromeOptions.addArguments("disable-component-cloud-policy");
        chromeOptions.addArguments("disable-component-update");
        chromeOptions.addArguments("disable-default-apps");
        chromeOptions.addArguments("disable-download-notification");
        chromeOptions.addArguments("disable-extensions");
        chromeOptions.addArguments("disable-extensions-file-access-check");
        chromeOptions.addArguments("disable-extensions-http-throttling");
        chromeOptions.addArguments("disable-hang-monitor");
        chromeOptions.addArguments("disable-infobars");
        chromeOptions.addArguments("disable-popup-blocking");
        chromeOptions.addArguments("disable-print-preview");
        chromeOptions.addArguments("disable-prompt-on-repost");
        chromeOptions.addArguments("disable-sync");
        chromeOptions.addArguments("disable-translate");
        chromeOptions.addArguments("disable-web-resources");
        chromeOptions.addArguments("disable-web-security");
        chromeOptions.addArguments("dns-prefetch-disable");
        chromeOptions.addArguments("download-whole-document");
        chromeOptions.addArguments("enable-logging");
        chromeOptions.addArguments("enable-screenshot-testing-with-mode");
        chromeOptions.addArguments("ignore-certificate-errors");
        chromeOptions.addArguments("log-level=0");
        chromeOptions.addArguments("metrics-recording-only");
        chromeOptions.addArguments("mute-audio");
        chromeOptions.addArguments("no-default-browser-check");
        chromeOptions.addArguments("no-displaying-insecure-content");
        chromeOptions.addArguments("no-experiments");
        chromeOptions.addArguments("no-first-run");
        chromeOptions.addArguments("no-sandbox");
        chromeOptions.addArguments("no-service-autorun");
        chromeOptions.addArguments("noerrdialogs");
        chromeOptions.addArguments("password-store=basic");
        chromeOptions.addArguments("reduce-security-for-testing");
        chromeOptions.addArguments("safebrowsing-disable-auto-update");
        chromeOptions.addArguments("safebrowsing-disable-download-protection");
        chromeOptions.addArguments("safebrowsing-disable-extension-blacklist");
        chromeOptions.addArguments("start-maximized");
        chromeOptions.addArguments("test-type=webdriver");
        chromeOptions.addArguments("use-mock-keychain");
        chromeOptions.setHeadless(BROWSER_HEADLESS);

        capabilities.merge(chromeOptions);
    }

    /**
     * Download https://developer.apple.com/safari/download/ and activate menu Develop with enable Allow Remote Automation
     */
    private void setupSafari() {
        SafariOptions safariOptions = new SafariOptions();
        safariOptions.setUseTechnologyPreview(true);

        capabilities.merge(safariOptions);
    }

    private void setupIE() {
        InternetExplorerOptions explorerOptions = new InternetExplorerOptions();
        explorerOptions.destructivelyEnsureCleanSession();

        capabilities.setCapability("se:ieOptions", explorerOptions);
        capabilities.merge(explorerOptions);
    }

    private void setupEdge() {
        EdgeOptions edgeOptions = new EdgeOptions();

        capabilities.merge(edgeOptions);
    }
}