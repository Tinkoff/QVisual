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

import org.bson.Document;

import java.util.Date;

public class Snapshot {

    private Date datetime;

    private String server;
    private String branch;
    private String commit;
    private String testcaseId;

    private String story;
    private String state;

    private String elements;
    private String url;

    private String device;
    private String osName;
    private String osVersion;
    private String browserName;
    private String browserVersion;
    private String resolution;
    private boolean retina;

    public Snapshot() {
    }

    public Snapshot(String testcaseId, String story, String state) {
        this.testcaseId = testcaseId;
        this.story = story;
        this.state = state;
    }

    public Snapshot(Document dbObject) {
        this.server = dbObject.getString("server");
        this.branch = dbObject.getString("branch");
        this.commit = dbObject.getString("commit");
        this.testcaseId = dbObject.getString("testcaseId");
        this.story = dbObject.getString("story");
        this.state = dbObject.getString("state");
        this.datetime = dbObject.getDate("datetime");
        this.elements = dbObject.getString("elements");
        this.url = dbObject.getString("url");
        this.device = dbObject.getString("device");
        this.osName = dbObject.getString("osName");
        this.osVersion = dbObject.getString("osVersion");
        this.browserName = dbObject.getString("browserName");
        this.browserVersion = dbObject.getString("browserVersion");
        this.resolution = dbObject.getString("resolution");
        this.retina = dbObject.getBoolean("retina");
    }

    public String getHash() {
        return new StringBuilder()
                .append(story).append("|&story&|")
                .append(state).append("|&state&|")
                .append(device).append("|")
                .append(osName).append("|")
                .append(osVersion).append("|")
                .append(browserName).append("|")
                .append(browserVersion).append("|")
                .append(resolution).append("|")
                .append(retina).toString();
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = commit;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public String getTestcaseId() {
        return testcaseId;
    }

    public void setTestcaseId(String testcaseId) {
        this.testcaseId = testcaseId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Date getDatetime() {
        return datetime;
    }

    public void setDatetime(Date datetime) {
        this.datetime = datetime;
    }

    public String getElements() {
        return elements;
    }

    public void setElements(String elements) {
        this.elements = elements;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
}