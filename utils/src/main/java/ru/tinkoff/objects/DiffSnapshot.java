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
package ru.tinkoff.objects;

/*
 * @author Snezhana Krass
 */

import java.util.List;

public class DiffSnapshot {

    private String testcaseId;
    private Result server;
    private Result branch;
    private Result commit;
    private String pixels;
    private String error;
    private Browser browser;
    private Float diffPercentage;
    private Result images;
    private List<DiffElement> elements;

    public DiffSnapshot() {
    }

    public static class Browser {

        private String device;
        private String osName;
        private String osVersion;
        private String browserName;
        private String browserVersion;
        private String resolution;
        private boolean retina;

        public Browser() {
        }

        public Browser(String device, String osName, String osVersion,
                       String browserName, String browserVersion,
                       String resolution, boolean retina) {
            this.device = device;
            this.osName = osName;
            this.osVersion = osVersion;
            this.browserName = browserName;
            this.browserVersion = browserVersion;
            this.resolution = resolution;
            this.retina = retina;
        }

        public String getDevice() {
            return device;
        }

        public void setDevice(String device) {
            this.device = device;
        }

        public String getOsName() {
            return osName;
        }

        public void setOsName(String osName) {
            this.osName = osName;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getBrowserName() {
            return browserName;
        }

        public void setBrowserName(String browserName) {
            this.browserName = browserName;
        }

        public String getBrowserVersion() {
            return browserVersion;
        }

        public void setBrowserVersion(String browserVersion) {
            this.browserVersion = browserVersion;
        }

        public String getResolution() {
            return resolution;
        }

        public void setResolution(String resolution) {
            this.resolution = resolution;
        }

        public boolean isRetina() {
            return retina;
        }

        public void setRetina(boolean retina) {
            this.retina = retina;
        }

        @Override
        public String toString() {
            return String.format("%s|%s|%s|%s|%s|%s|%s", device, osName, osVersion, browserName, browserVersion, resolution, retina);
        }
    }

    public String getTestcaseId() {
        return testcaseId;
    }

    public void setTestcaseId(String testcaseId) {
        this.testcaseId = testcaseId;
    }

    public Result getServer() {
        return server;
    }

    public void setServer(Result server) {
        this.server = server;
    }

    public Result getBranch() {
        return branch;
    }

    public void setBranch(Result branch) {
        this.branch = branch;
    }

    public Result getCommit() {
        return commit;
    }

    public void setCommit(Result commit) {
        this.commit = commit;
    }

    public String getPixels() {
        return pixels;
    }

    public Float getDiffPercentage() {
        return diffPercentage;
    }

    public void setDiffPercentage(Float diffPercentage) {
        this.diffPercentage = diffPercentage;
    }

    public void setPixels(String pixels) {
        this.pixels = pixels;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Result getImages() {
        return images;
    }

    public void setImages(Result images) {
        this.images = images;
    }

    public List<DiffElement> getElements() {
        return elements;
    }

    public void setElements(List<DiffElement> elements) {
        this.elements = elements;
    }

    public Browser getBrowser() {
        return browser;
    }

    public void setBrowser(Browser browser) {
        this.browser = browser;
    }
}