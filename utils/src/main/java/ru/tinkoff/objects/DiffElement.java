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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DiffElement {

    private List<String> diff;
    private String name;
    private String display;
    private String image;
    private Result text;
    private HashMap<String, Result> css;
    private HashMap<String, Result> attributes;
    private Area area;

    public DiffElement() {
        this.name = null;
        this.diff = new ArrayList<>();
        this.display = null;
        this.text = null;
        this.css = new HashMap<>();
        this.attributes = new HashMap<>();
        this.area = new Area();
    }

    public static class Area {

        private Result left;
        private Result top;
        private Result right;
        private Result bottom;
        private Result width;
        private Result height;

        public Area() {
        }

        public Area(Element actual, Element expected) {
            Element.Area actualArea = actual.getArea();
            Element.Area expectedArea = expected.getArea();

            left = new Result(actualArea.getLeft(), expectedArea.getLeft());
            top = new Result(actualArea.getTop(), expectedArea.getTop());
            right = new Result(actualArea.getRight(), expectedArea.getRight());
            bottom = new Result(actualArea.getBottom(), expectedArea.getBottom());
            width = new Result(actualArea.getWidth(), expectedArea.getWidth());
            height = new Result(actualArea.getHeight(), expectedArea.getHeight());
        }

        public Result getLeft() {
            return left;
        }

        public void setLeft(Result left) {
            this.left = left;
        }

        public Result getTop() {
            return top;
        }

        public void setTop(Result top) {
            this.top = top;
        }

        public Result getRight() {
            return right;
        }

        public void setRight(Result right) {
            this.right = right;
        }

        public Result getBottom() {
            return bottom;
        }

        public void setBottom(Result bottom) {
            this.bottom = bottom;
        }

        public Result getWidth() {
            return width;
        }

        public void setWidth(Result width) {
            this.width = width;
        }

        public Result getHeight() {
            return height;
        }

        public void setHeight(Result height) {
            this.height = height;
        }
    }

    public List<String> getDiff() {
        return diff;
    }

    public void addDiff(String diff) {
        this.diff.add(diff);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public Result getText() {
        return text;
    }

    public void setText(Result text) {
        this.text = text;
    }

    public HashMap<String, Result> getCss() {
        return css;
    }

    public void setCss(HashMap<String, Result> css) {
        this.css = css;
    }

    public HashMap<String, Result> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, Result> attributes) {
        this.attributes = attributes;
    }

    public Area getArea() {
        return area;
    }

    public void setArea(Area area) {
        this.area = area;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }
}