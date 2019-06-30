/*
 * Copyright 2019 IntraFind Software AG. All rights reserved.
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

package com.intrafind.sitesearch.controller;

import com.intrafind.sitesearch.service.CrawlerService;
import com.intrafind.sitesearch.service.SiteCrawler;
import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

public class CrawlerControllerFactory<T extends WebCrawler> implements CrawlController.WebCrawlerFactory {
    private final static Logger LOG = LoggerFactory.getLogger(CrawlerControllerFactory.class);
    private final UUID siteId;
    private final UUID siteSecret;
    private final URI url;
    private final boolean allowUrlWithQuery;
    private final String pageBodyCssSelector;
    private final BaseRobotRules robotRules;

    public CrawlerControllerFactory(final UUID siteId, final UUID siteSecret, final URI url, final String pageBodyCssSelector, final boolean allowUrlWithQuery) {
        this.siteId = siteId;
        this.siteSecret = siteSecret;
        this.url = url;
        this.pageBodyCssSelector = pageBodyCssSelector;
        this.robotRules = initRobotRules();
        this.allowUrlWithQuery = allowUrlWithQuery;
    }

    private byte[] fetchRobotsTxt() {
        var robotsTxtContent = new byte[]{};
        try {
            final var request = new Request.Builder()
                    .url(url + "/robots.txt")
                    .build();
            final var robotsResponse = SiteCrawler.HTTP_CLIENT.newCall(request).execute();
            if (HttpStatus.OK.value() == robotsResponse.code()) {
                if (robotsResponse.body() != null) {
                    robotsTxtContent = Objects.requireNonNull(robotsResponse.body()).bytes();
                }
            }
            robotsResponse.close();
        } catch (final IOException e) {
            LOG.warn("siteId: " + siteId + " - robots.txt is missing - exception: " + e.getMessage());
        }

        if (robotsTxtContent.length == 0) {
            LOG.warn("siteId: " + siteId + " - robots.txt is missing.");
        }
        return robotsTxtContent;
    }

    private BaseRobotRules initRobotRules() {
        final var simpleRobotRulesParser = new SimpleRobotRulesParser();
        return simpleRobotRulesParser.parseContent(
                url.toString(),
                fetchRobotsTxt(),
                "text/plain",
                CrawlerService.SITE_SEARCH_USER_AGENT
        );
    }

    @Override
    public SiteCrawler newInstance() {
        return new SiteCrawler(siteId, siteSecret, url, pageBodyCssSelector, robotRules, allowUrlWithQuery);
    }
}