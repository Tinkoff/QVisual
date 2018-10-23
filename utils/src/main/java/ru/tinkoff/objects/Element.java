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

import java.util.HashMap;

public class Element {

    private String display;
    private String text;
    private HashMap<String, String> css;
    private HashMap<String, String> attributes;
    private Area area;

    public Element() {
        this.display = null;
        this.text = null;
        this.css = new HashMap<>();
        this.attributes = new HashMap<>();
        this.area = new Area();
    }

    public static class Area {

        private Double left;
        private Double top;
        private Double right;
        private Double bottom;
        private Double width;
        private Double height;

        public Area() {
            this.left = 0.0d;
            this.top = 0.0d;
            this.right = 0.0d;
            this.bottom = 0.0d;
            this.width = 0.0d;
            this.height = 0.0d;
        }

        public Area(Double left, Double top, Double right, Double bottom, Double width, Double height) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return String.format("left = %f \ntop = %f \nright = %f \nbottom = %f \nwidth = %f \nheight = %f \n",
                    left, top, right, bottom, width, height);
        }

        public Double getLeft() {
            return left;
        }

        public void setLeft(Double left) {
            this.left = left;
        }

        public Double getTop() {
            return top;
        }

        public void setTop(Double top) {
            this.top = top;
        }

        public Double getRight() {
            return right;
        }

        public void setRight(Double right) {
            this.right = right;
        }

        public Double getBottom() {
            return bottom;
        }

        public void setBottom(Double bottom) {
            this.bottom = bottom;
        }

        public Double getHeight() {
            return height;
        }

        public void setHeight(Double height) {
            this.height = height;
        }

        public Double getWidth() {
            return width;
        }

        public void setWidth(Double width) {
            this.width = width;
        }
    }

    public String getDisplay() {
        return display;
    }

    public String getText() {
        return text;
    }

    public HashMap<String, String> getCss() {
        return css;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public Area getArea() {
        return area;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setCss(HashMap<String, String> css) {
        this.css = css;
    }

    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setArea(Area area) {
        this.area = area;
    }
}
