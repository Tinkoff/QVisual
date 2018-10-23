function hideElements() {
    selectors.split("%%").forEach(function (selector) {
        var results;
        if (selector.startsWith("//")) {
            var query = document.evaluate(selector, document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
            results = Array(query.snapshotLength).fill(0).map(function (element, index) {
                                return query.snapshotItem(index);});
        } else {
            results = document.querySelectorAll(selector);
        }

        if (results !== undefined && results.length > 0) {
            for (var i = 0; i < results.length; i++) {
                if (displayNone) {
                    results[i].style.display = "none";
                } else {
                    results[i].style.visibility = "hidden";
                }
            }
        }
    });
}

return hideElements();