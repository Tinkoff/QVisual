function getComputedCss(element) {
    var css = {};
    var style = window.getComputedStyle(element, null);

    if (style) {
        for (var i = 0; i < style.length; i++) {
                var name = style[i];
                css[name] = style.getPropertyValue(name);
        }
    }

    return css;
}

function getAttributes(element) {
    var attributes = {};

    if (element.hasAttributes()) {
        var attributesValues = element.attributes;

        for (var i = 0; i < attributesValues.length; i++) {
            var attributeItem = attributesValues.item(i);
            attributes[attributeItem.name] = attributeItem.value;
        }
    }

    return attributes;
}

function getText(element) {
    return element.innerText || element.textContent;
}

function getArea(element) {
    var box = element.getBoundingClientRect();
    var body = document.body;
    var docElem = document.documentElement;
    var scrollTop = window.pageYOffset || docElem.scrollTop || body.scrollTop;
    var scrollLeft = window.pageXOffset || docElem.scrollLeft || body.scrollLeft;
    var clientTop = docElem.clientTop || body.clientTop || 0;
    var clientLeft = docElem.clientLeft || body.clientLeft || 0;
    var top = box.top + scrollTop - clientTop;
    var left = box.left + scrollLeft - clientLeft;

    var area = {};
    area.left = left;
    area.top = top;
    area.width = box.width;
    area.height = box.height;
    area.right = left + area.width;
    area.bottom = top + area.height;

    return area;
}

function convertNodesListToArray(nodesList) {
    return Array.prototype.slice.call(nodesList);
}

function getElements() {
    var elements = {};

    locators.split("%%").forEach(function (l) {
        var locator = l;
        var index = "";

        if (locator.match(/%/)) {
            let splitLocator = locator.split("%");
            locator = splitLocator[0];
            index = splitLocator[1];
        }

        var innerDoc = document;
        var regexXpath = /iframe\[@id\=\'(.*?)\'\](.*)/;
        var regexCss = /iframe#(.*?) (.*)/;
        var regex = "";

        if (regexXpath.test(locator)) {
            regex = regexXpath;
        } else if (regexCss.test(locator)) {
            regex = regexCss;
        }

        if (regex !== "") {
            var iframeId = locator.match(regex)[1];
            var iframe = document.getElementById(iframeId);
            if (iframe) {
            	innerDoc = iframe.contentDocument || iframe.contentWindow.document;

            	locator = locator.match(regex)[2];
            }
        }

        if (locator.startsWith("//")) {
            var query = innerDoc.evaluate(locator, innerDoc, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
            if (query !== undefined && query.snapshotLength > 0) {
                var results = Array(query.snapshotLength).fill(0).map(function (e, i) {return query.snapshotItem(i);});

                if (index == "") {
                    for (j = 0; j < query.snapshotLength; j++) {
                        if (query.snapshotLength == 1) {
                            elements[locator] = results[j];
                        } else {
                            elements[locator + "%" + j] = results[j];
                        }
                    }
                } else {
                    elements[l] = results[index];
                }
            } else {
                elements[l] = "";
            }
        } else {
            var results = innerDoc.querySelectorAll(locator);
            if (results !== undefined && results.length > 0) {
                if (index == "") {
                    for (j = 0; j < results.length; j++) {
                        if (results.length == 1) {
                            elements[locator] = results.item(j);
                        } else {
                            elements[locator + "%" + j] = results.item(j);
                        }
                    }
                } else {
                    elements[l] = results.item(index);
                }
            } else {
                 elements[l] = "";
            }
        }
    });

    return elements;
}

function saveElementProperties() {
    var properties = {};

    var elements = getElements(locators);
    for (const [name, element] of Object.entries(elements)) {
        var elementProperties = {};

        if (element !== "") {
            var css = getComputedCss(element);
            elementProperties.display = css.display;
            elementProperties.area = getArea(element);
            elementProperties.css = css;
            elementProperties.attributes = getAttributes(element);
            elementProperties.text = getText(element);

            properties[name] = elementProperties;
        } else {
            elementProperties.display = "not found";

            properties[name] = elementProperties;
        }
    }

    return JSON.stringify(properties);
}

return saveElementProperties();