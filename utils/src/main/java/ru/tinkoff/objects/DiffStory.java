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

public class DiffStory {

    private String story;
    private List<DiffState> states;

    public DiffStory() {
    }

    public DiffStory(String story) {
        this.story = story;
    }

    public String getStory() {
        return story;
    }

    public void setStory(String story) {
        this.story = story;
    }

    public List<DiffState> getStates() {
        return states;
    }

    public void setStates(List<DiffState> states) {
        this.states = states;
    }
}