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
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;

public class ChromeDriverEx extends ChromeDriver {

    public ChromeDriverEx(Capabilities capabilities) {
        this(ChromeDriverService.createDefaultService(), capabilities);
    }

    public ChromeDriverEx(ChromeDriverService service, Capabilities capabilities) {
        super(service, capabilities);
    }

    public Object sendCommand(String cmd, Object params) {
        return execute("sendCommand", ImmutableMap.of("cmd", cmd, "params", params)).getValue();
    }
}