# QVisual

Tinkoff tool to control quality by visual testing.

It's the fastest approach for developers to take a feedback of web page layout and styles quality for Reactjs Storybook or Angular, about a lot of landing pages and to comcentrate QA team attention on functional tests by automaticaly get 100% pixels comparison for screenshots of all popular browsers.

This tool contains 3 modules: screenshoter (framework to take and save visual data), visualreport (jar application for REST API to save and generate visual comparison report), webreport (Reactjs application to view screenshots and pixel-by-pixel comparison).

# Module screenshoter

This module takes rendered web page visual data by frameworks [SeleniumHQ/selenium](https://github.com/SeleniumHQ/selenium) and [yandex-qatools/ashot](https://github.com/yandex-qatools/ashot).

### Requirements

[Maven 3.5.2+](https://maven.apache.org/install.html) (or check Maven is installed by command **mvn -version**)

## Usage screenshoter

### 1. To start Selenium WebDriver

It's required to have Selenium WebDriver to take a screenshot. There is a method **getDriver** in a class **WebDriverManager** that starts all popular browsers Chrome, Safari, Firefox, Internet explorer on your local machine or on services Selenium Grid, Selenoid, BrowserStack.

(**BrowserStack** requires to launch binary for local testing (for Windows run command: ./screenshoter/src/main/resources/BrowserStackLocal-windows.exe --key BROWSERSTACK_TOKEN --force-local, for MAC run command: ./screenshoter/src/main/resources/BrowserStackLocal-mac --key BROWSERSTACK_TOKEN --force-local)

There are a lot of parameters for starting browser in properties section of the parent pom.xml with default values:

    <properties>
        <!--Browser parameters-->
        <driver.service></driver.service>
        <webdriver.remote.url></webdriver.remote.url>
        <device></device>
        <platform.name></platform.name>
        <platform.version></platform.version>
        <browser.headless>false</browser.headless>
        <browser.name>chrome</browser.name>
        <browser.version></browser.version>
        <browser.retina>false</browser.retina>
        <webdriver.dir>${project.basedir}/target/webdriver</webdriver.dir>
        <download_dir.path>${project.basedir}/target/download</download_dir.path>

        <!--BrowserStack parameters-->
        <browserstack.url></browserstack.url>
        <browserstack.selenium.version>3.14.0</browserstack.selenium.version>
        <browserstack.appium_version>1.8.0</browserstack.appium_version>
        <browserstack.device.orientation>portrait</browserstack.device.orientation>
        <realMobile>false</realMobile>

        <!--Selenoid parameters-->
        <enableVNC>true</enableVNC>
        <enableVideo>false</enableVideo>
    </properties>

---
    driver.service

> If value is empty then tests are started on local machine.
>
> Values: selenoid, grid, browserstack.

    webdriver.remote.url

> (Required for non-local run) This value should be equal to the HUB of Selenium Grid, Selenoid or BrowserStack.

    device

> (Required for mobile device in BrowserStack) A device name from [BrowserStack capabilities](https://www.browserstack.com/automate/java#run-tests-on-desktop-mobile).

    platform.name

> (Required) A platform name.
>
> Values: WIN, MAC, LINUX.

    platform.version

> A platform version.

    browser.headless

> To execute browser in the background.

    browser.name

> (Required) A browser name.
>
> Values: chrome, safari, firefox, internet explorer.

    browser.version

> (Required for BrowserStack, Selenoid) A browser version.

    webdriver.dir

> (Required) A directory path to download the last version of webdriver by library [bonigarcia/webdrivermanager](https://github.com/bonigarcia/webdrivermanager).

    download_dir.path

> (Required) A directory path to download files from browser.

    browserstack.url

> (Required for BrowserStack) BrowserStack hub URL.

    browserstack.selenium.version

> (Required for BrowserStack) BrowserStack selenium version.

    browserstack.appium_version

> (Required for BrowserStack) BrowserStack Appium version.

    browserstack.device.orientation

> (Required for BrowserStack) BrowserStack mobile device orientation.

    realMobile

> (Required for BrowserStack) BrowserStack real mobile or emulator.

    enableVNC

> (Required for Selenoid) Disable or enable VNC.

    enableVideo

> (Required for Selenoid) Disable or enable video recording.

#### 2. To take screenshots

There is a method **takeScreen** in a class **ScreenShooter** that takes a screenshot in all popular browsers: Chrome, Safari, Firefox, Internet Explorer on your local machine or through services Selenium Grid, Selenoid, BrowserStack.

This method provides resizing browser display and taking a full page or a visible viewport screenshot, but it depends on browser:

Chrome - full page or viewport,

Safari - full page,

Firefox - full page or viewport,

Internet Explorer - there are troubles because Selenium API method to take a screenshot returns a black image.

There are required parameters in properties section of the main pom.xml with default values:

    <properties>
        <browser.resolution>1440x900</browser.resolution>
        <screen.resolutions>1440x900,1024x900,768x900</screen.resolutions>
        <screen.scroll.timeout>500</screen.scroll.timeout>
        <resize.timeout>1000</resize.timeout>
    </properties>

---

    browser.resolution

> (Required) Default display size. If value is empty browser will be maximized.

    screen.resolutions

> Array of browser sizes sequentially applied to browser windows before to take a new screenshot. After the last size of sequence is applied the browser is restored to default browser size from parameter **browser.resolution**.

    resize.timeout

> Timeout after resizing browser.

    screen.scroll.timeout

> (Required for Firefox) Timeout of scrolling for AShot framework.

### 3. To take computed CSS values of web-elements

There is a method **getElements** in a class **WebDriverUtils** that takes a web-element computed CSS values, coordinates and size.

This method requires a list of web-elements XPath (or CSS) locators of web page.

Example web-elements XPath locators: ./demo/src/test/resources/main.txt.

> If you have to store just one item from a founded list of elements - add its order number (start from 0) at the end of locator, for example: **//div%0** returns the first founded item from list of WebElements.
>
> If locator founds a list of web-elements - all of them would be stored with their order numbers at the end.
>
> To find element inside of **iframe** - add a prefix to locator with iframe and @id attribute, for example: **//iframe\[@id=‘my-iframe’\]//div\[contains(@class, ‘my-class’)\]** returns WebElement by locator //div\[contains(@class, ‘my-class’)\] inside of iframe with ID "my-iframe".

### 4. To take and store visual data

There is a method **createSnapshot** in a class **SnapShooter** that sends all visual data of web page to the backend server by API POST /snapshots/create.

There are required parameters in properties section of the main pom.xml with default values:

    <properties>
        <backend.domain>http://localhost:8081</backend.domain>
        <frontend.domain>http://localhost:5000</frontend.domain>
        <dateTime></dateTime>
    </properties>

---

    backend.domain

> (Required) visualreport REST API domain.

    frontend.domain

> (Required) webreport domain.

    dateTime

> (Required) Datetime ISO format yyyy-MM-ddTHH:MM:SS.sssZ. This date is required to save and get report of screenshots comparison.

Non required parameters, it's shown above screenshots in webreport:

    <properties>
        <server></server>
        <base.url></base.url>
        <branch></branch>
        <commit></commit>
    </properties>

---

    server (or base.url)

> Domain of web page.

    branch

> Branch that is deployed to tested server.

    commit

> Commit hash that is deployed to tested server.

# Module visualreport

API methods to save screenshots and elements styles of tested web pages to database and to generate pixel-by-pixel difference report by [OpenCV library for Java](https://opencv-java-tutorials.readthedocs.io/en/latest/04-opencv-basics.html).

### Requirements

[Java 1.8+](https://java.com/en/download/help/download_options.xml) (or check Java is installed by command **java -version**)

[Maven 3.5.2+](https://maven.apache.org/install.html) (or check Maven is installed by command **mvn -version**)

[MongoDB 3.4.7+](https://docs.mongodb.com/manual/administration/install-community/) (or check MongoDB is installed by command **mongo -version**)

[OpenCV 3.4.1_4](https://opencv-java-tutorials.readthedocs.io/en/latest/01-installing-opencv-for-java.html#introduction-to-opencv-for-java) (or use compiled resources from project directory ./visualreport/src/main/resources/)

### Start API server

1. Go to the QVisual project root directory.

2. Compile and build API jar:

        mvn clean compile assembly:single

3. Start visualreport jar with parameters (replace the PROJECT_DIR with absolute path):

        java -Xms2g -Xmx13g -XX:+AlwaysPreTouch -XX:+UseG1GC -XX:+ScavengeBeforeFullGC
        -Dspark.host=localhost
        -Dspark.port=8081
        -Dbackend.domain=http://localhost:8081
        -Dfrontend.domain=http://localhost:5000
        -Dmongodb.host=localhost
        -Dmongodb.port=27017
        -Dscreenshooter.dir=PROJECT_DIR/results
        -Djava.library.path=PROJECT_DIR/visualreport/src/main/resources/
        -jar PROJECT_DIR/visualreport/target/visualreport-1.0.jar

> API server log messages are stored in ./visualreport/visualreport.log.
>
> Setup logger with [qos-ch/logback](https://github.com/qos-ch/logback) in a file ./visualreport/src/main/resources/logback.xml

### API Parameters

    spark.host

> (Required) Host for API server.

    spark.port

> (Required) Port for API server.

    mongodb.host

> (Required) Host for MongoDB.

    mongodb.port

> (Required) Port for MongoDB.

    screenshooter.dir

> (Required) Path to save original screenshots and report of screens comparison.

    opencv.dir

> (Required for Linux) Path to OpenCV .so library (available resource file ./visualreport/src/main/resources/libopencv_java-3.4.1.so)

    java.library.path

> (Required) Path to OpenCV .jar library (available resources directory ./visualreport/src/main/resources/). There are also required files .so for Linux, .dylib for MAC, .dll for Windows.

## API documentation

Api methods from **SnapshotApiService** class.

#### GET /snapshots/report?actual={actual}&expected={expected}

Get a pixel-by-pixel screenshots comparison and web-elements styles differences report.

Query parameters:

    actual

> (Required) ISO format datetime yyyy-MM-ddTHH:MM:SS.sssZ for actual results.

    expected

> (Required) ISO format datetime yyyy-MM-ddTHH:MM:SS.sssZ for expected results.

    reload

> true - to remove previously saved report and create new report by getting data from db.
>
> false - to get a report from the reports directory with all previously generated reports or to create a new report if there is no saved report for that by actual and expected dates.
>
> Default: false

    rgb

> Accuracy of screens comparison between of each 3 colors RGB.
>
> Default: 0, that means 100% comparison of screenshots.

    testrunid

> ID of Testrun in TestRail. Testcase results will set in TestRail after report is created.
>
> Default: empty, that means not to run TestRail API scripts.

#### GET /snapshots

To get all stored tests grouped by datetime.

#### GET /snapshots/:datetime

To get all stored tests searched by datetime.

#### GET /images/original/:filename

To get an original screenshot by filename.

#### GET /images/reports/:filename

To get a screenshot of pixels comparison by filename.

#### POST /snapshots/create

Parameters: ./utils/src/main/java/ru/tinkoff/objects/Snapshot.java.

To store visual data.

#### POST /upload/image

Parameters: String url, String fileName, BufferedImage image

To upload a screenshot to the original images directory.

# Module webreport

Frontend report to view screenshots pixels differences by tests with filters by error types of styles and visibility and pixels comparison percentage.

### Requirements

[npm](https://www.npmjs.com/get-npm)

[npm serve package](https://www.npmjs.com/package/serve)

### Start webreport

1. Download QVisual-webreport project and go to the QVisual-webproject root directory.

2. Install dependencies:

        npm i

3. Compile build with default backend URL http://localhost:8081 :

        npm run build:test:zip

   Or compile build with custom backend URL (if case of using custom backend URL check that all parameters have the same host and port - spark.host, spark.port, backend.domain):

        REACT_APP_API='BACKEND_URL' PUBLIC_API='/' npx react-scripts-ts build

4. Start web server:

        serve -s -p 5000 ./build

5. Open web report:

        http://localhost:5000/web/actual/ACTUAL_DATE_TIME/expected/EXPECTED_DATE_TIME

# Run demo tests

You just have to add one method **snapshot** from **SnapShoter** class to take and save screenshots. **OpenUrlsTests** is an example test with 2 web pages and list of web-elements.

The **snapshot** method takes a screenshot and calls the API method of visualreport server to store data of tests in MonogoDB and save screenshots to the original screenshots directory.

1. Start visualreport and webreport.

2. Build tests:

        mvn -pl demo -am clean install -DskipTests

3. Run tests on test server to store visual data with parameter ACTUAL_DATETIME (use **-P** with profile name from parent pom.xml to override default browser properties):

        mvn -f demo/pom.xml test -DdateTime=ACTUAL_DATETIME -DsuiteXmlFile=testng.xml -P browser_chrome

4. Run tests on release server to store visual data with parameter EXPECTED_DATETIME (use **-P** with profile name from parent pom.xml to override default browser properties):

        mvn -f demo/pom.xml test -DdateTime=EXPECTED_DATETIME -DsuiteXmlFile=testng.xml -P browser_chrome

5. View web report after tests finished by ACTUAL_DATETIME and EXPECTED_DATETIME dates by URL http://localhost:5000/web/actual/ACTUAL_DATETIME/expected/EXPECTED_DATETIME