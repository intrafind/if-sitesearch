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


var SiSregisterSite = function () {
    var status = document.getElementById("sis-status");
    status.textContent = "Crawling... please give us a minute or two.";

    var siteCreation = new XMLHttpRequest();
    siteCreation.open("POST", "https://api.sitesearch.cloud/sites");
    siteCreation.onload = function () {
        function setupCrawling() {
            console.warn(siteCreation.responseText);
            var newSite = JSON.parse(siteCreation.responseText);

            var siteIdElement = document.getElementById("sis-siteId");
            var siteSecretElement = document.getElementById("sis-siteSecret");

            siteIdElement.value = newSite.siteId;
            siteSecretElement.value = newSite.siteSecret;

            var siteUrlElement = document.getElementById("sis-url");
            var siteUrl = siteUrlElement.value;
            console.warn(siteUrl);
            IFS.jQuery.ifs.shared.clientOptions.siteId = newSite.siteId;
            return {newSite, siteUrl};
        }

        function SiScrawlSite() {
            var {newSite, siteUrl} = setupCrawling();
            var siteCrawl = new XMLHttpRequest();
            siteCrawl.open("POST", "https://api.sitesearch.cloud/sites/" + newSite.siteId + "/crawl?siteSecret=" + newSite.siteSecret + "&url=" + siteUrl + "&token=1a46b7c0-8684-11e8-8f10-d74554b855dc&email=&sitemapsOnly=false&pageBodyCssSelector=body");
            siteCrawl.onload = function () {
                function showStatusAndSaveSetup() {
                    console.warn(siteCrawl.responseText);
                    var pageCount = JSON.parse(siteCrawl.responseText).pageCount;
                    status.textContent = "Pages crawled: " + pageCount;
                    console.info("Pages crawled: " + pageCount);

                    var saveSiteSearchSetup = document.getElementById("sis-save-setup");
                    saveSiteSearchSetup.click();
                }

                showStatusAndSaveSetup();
            };
            siteCrawl.send();
        }

        SiScrawlSite();
    };
    siteCreation.send();
};