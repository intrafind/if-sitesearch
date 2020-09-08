/*
 * Copyright 2018 IntraFind Software AG. All rights reserved.
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

function SiSgetCookieValueForKey(cookieKey) {
	var keyWithExtension = cookieKey + "=";
	var cookiePairs = document.cookie.split(';');
	for (var index = 0; index < cookiePairs.length; index++) {
		var cookiePairTarget = cookiePairs[index];
		while (cookiePairTarget.charAt(0) === " ")
			cookiePairTarget = cookiePairTarget.substring(1, cookiePairTarget.length);
		if (cookiePairTarget.indexOf(keyWithExtension) === 0)
			return cookiePairTarget.substring(keyWithExtension.length, cookiePairTarget.length);
	}
	return "";
}

var SiSinjectSearchbar = function () {

    var sisDefaultWordPressSearchbarSelectorBase64 = SiSgetCookieValueForKey("sisDefaultWordPressSearchbarSelector");
    var sisDefaultWordPressSearchbarSelector = atob(sisDefaultWordPressSearchbarSelectorBase64);
    console.warn("Site Search selector: " + sisDefaultWordPressSearchbarSelector);
    var defaultWordPressSearchbar = document.querySelector(sisDefaultWordPressSearchbarSelector);
    var hiddenSiSsearchbar = document.querySelector("#sitesearch-searchbar");
    hiddenSiSsearchbar.style.display = "block";
	if(defaultWordPressSearchbar != null) {
		defaultWordPressSearchbar.innerHTML = "";

		defaultWordPressSearchbar.appendChild(hiddenSiSsearchbar);
	}
    IFS.jQuery.ifs.shared.clientOptions.siteId = SiSgetCookieValueForKey("sis-siteId");
};

window.addEventListener("DOMContentLoaded", function () {
	IFS.initClient({customConfig: {overwrite: {"appLang": "en"}},configurl: "https://cdn.sitesearch.cloud/searchbar/2018-07-18/config/sitesearch.json",siteId:SiSgetCookieValueForKey("sis-siteId"),"freemium":true});
    console.warn("DOMContentLoaded - before: " + IFS.jQuery.ifs.shared.clientOptions.siteId);
    SiSinjectSearchbar();
    console.warn("DOMContentLoaded - after: " + IFS.jQuery.ifs.shared.clientOptions.siteId);
});
