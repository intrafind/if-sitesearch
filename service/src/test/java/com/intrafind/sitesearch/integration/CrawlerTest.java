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

package com.intrafind.sitesearch.integration;

import com.intrafind.sitesearch.Application;
import com.intrafind.sitesearch.controller.SiteController;
import com.intrafind.sitesearch.dto.CrawlStatus;
import com.intrafind.sitesearch.dto.CrawlerJobResult;
import com.intrafind.sitesearch.dto.FetchedPage;
import com.intrafind.sitesearch.dto.SitesCrawlStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CrawlerTest {
    static final UUID CRAWL_SITE_ID = UUID.fromString("a2e8d60b-0696-47ea-bc48-982598ee35bd");
    private static final UUID CRAWL_SITE_SECRET = UUID.fromString("04a0afc6-d89a-45c9-8ba8-41d393d8d2f8");
    private static final Logger LOG = LoggerFactory.getLogger(CrawlerTest.class);
    static final String TEST_EMAIL_ADDRESS = "Test - Site Search <0a4292be.intrafind.de@emea.teams.ms>";
    private static final int API_SITE_PAGE_COUNT = 9;

    @Autowired
    private TestRestTemplate caller;

    @Test
    public void crawlExampleViaHttp() {
        final ResponseEntity<CrawlerJobResult> request = caller
                .postForEntity(SiteController.ENDPOINT + "/" + CRAWL_SITE_ID + "/crawl?siteSecret=" + CRAWL_SITE_SECRET
                                + "&url=http://example.com&token=" + UUID.randomUUID()
                                + "&email=" + TEST_EMAIL_ADDRESS,
                        RequestEntity.EMPTY, CrawlerJobResult.class);

        assertEquals(HttpStatus.OK, request.getStatusCode());
        assertNotNull(request.getBody());
        assertEquals(1, request.getBody().getPageCount());
        assertEquals(1, request.getBody().getUrls().size());
        assertTrue(request.getBody().getUrls().get(0).contains("example.com"));
    }

    @Test
    public void crawlUsingSitemapsOnly() {
        final ResponseEntity<CrawlerJobResult> request = caller
                .postForEntity(SiteController.ENDPOINT + "/" + CRAWL_SITE_ID + "/crawl?siteSecret=" + CRAWL_SITE_SECRET
                                + "&url=https://www.sitemaps.org&token=" + UUID.randomUUID()
                                + "&email=" + TEST_EMAIL_ADDRESS
                                + "&sitemapsOnly=true",
                        RequestEntity.EMPTY, CrawlerJobResult.class);

        assertEquals(HttpStatus.OK, request.getStatusCode());
        assertNotNull(request.getBody());
        assertEquals(84, request.getBody().getPageCount());
    }

    @Test
    public void crawlSiteSearchViaHttps() {
        final ResponseEntity<CrawlerJobResult> request = caller
                .postForEntity(SiteController.ENDPOINT + "/" + CRAWL_SITE_ID + "/crawl?siteSecret=" + CRAWL_SITE_SECRET
                                + "&url=" + "https://" + Application.SIS_DOMAIN + "&token=" + UUID.randomUUID()
                                + "&email=" + TEST_EMAIL_ADDRESS,
                        RequestEntity.EMPTY, CrawlerJobResult.class);

        assertEquals(HttpStatus.OK, request.getStatusCode());
        assertNotNull(request.getBody());
        assertTrue(12 < request.getBody().getPageCount());
    }

    @Test
    public void crawlHttps() {
        final ResponseEntity<CrawlerJobResult> request = caller
                .postForEntity(SiteController.ENDPOINT + "/" + CRAWL_SITE_ID + "/crawl?siteSecret=" + CRAWL_SITE_SECRET
                                + "&url=https://api." + Application.SIS_DOMAIN + "&token=" + UUID.randomUUID()
                                + "&email=" + TEST_EMAIL_ADDRESS,
                        RequestEntity.EMPTY, CrawlerJobResult.class);

        assertEquals(HttpStatus.OK, request.getStatusCode());
        assertNotNull(request.getBody());
        assertEquals(API_SITE_PAGE_COUNT, request.getBody().getPageCount());
        assertEquals(API_SITE_PAGE_COUNT, request.getBody().getUrls().size());
        assertTrue(request.getBody().getUrls().get(0).contains("api." + Application.SIS_DOMAIN));
    }

    @Test
    public void recrawlMultiSiteConfig() {
        final var siteId = UUID.fromString("a9ede989-9d94-41d1-8571-a008318b01db");
        final ResponseEntity<CrawlerJobResult> request = caller
                .postForEntity(SiteController.ENDPOINT + "/" + siteId + "/recrawl?siteSecret=fbdc4e70-0141-4127-b95b-f9fd2d5e1b93"
                                + "&clearIndex=false",
                        RequestEntity.EMPTY, CrawlerJobResult.class);

        assertEquals(HttpStatus.OK, request.getStatusCode());
        assertNotNull(request.getBody());
        assertEquals(API_SITE_PAGE_COUNT, request.getBody().getPageCount());
        assertEquals(API_SITE_PAGE_COUNT, request.getBody().getUrls().size());
        assertTrue(request.getBody().getUrls().contains("https://api." + Application.SIS_DOMAIN + "/index.html"));
        assertTrue(request.getBody().getUrls().contains("https://dev." + Application.SIS_DOMAIN + "/"));

        assertThumnailInPagePayload(siteId);
    }

    @Test
    public void crawlSiteWithSitemap() {
        final ResponseEntity<CrawlerJobResult> request = caller
                .postForEntity(SiteController.ENDPOINT + "/" + CRAWL_SITE_ID + "/crawl?siteSecret=" + CRAWL_SITE_SECRET
                                + "&url=https://api." + Application.SIS_DOMAIN + "&token=" + UUID.randomUUID()
                                + "&email=" + TEST_EMAIL_ADDRESS
                                + "&sitemapsOnly=true",
                        RequestEntity.EMPTY, CrawlerJobResult.class);

        assertEquals(HttpStatus.OK, request.getStatusCode());
        assertNotNull(request.getBody());
        assertEquals(1, request.getBody().getPageCount());

        assertThumnailInPagePayload(CRAWL_SITE_ID);
    }

    private void assertThumnailInPagePayload(final UUID siteId) {
        final String pageWithThumbnail = "https://api." + Application.SIS_DOMAIN + "/index.html";
        final ResponseEntity<FetchedPage> fetchThumbnailPage = caller.exchange(SiteController.ENDPOINT
                        + "/" + siteId + "/pages?url=" + pageWithThumbnail,
                HttpMethod.GET, HttpEntity.EMPTY, FetchedPage.class);
        assertEquals(HttpStatus.OK, fetchThumbnailPage.getStatusCode());
        assertEquals(pageWithThumbnail, Objects.requireNonNull(fetchThumbnailPage.getBody()).getUrl());
        assertEquals("https://api." + Application.SIS_DOMAIN + "/theme/logo.png", fetchThumbnailPage.getBody().getThumbnail());
        assertEquals(Arrays.asList("Portal", "Start", "homepage"), fetchThumbnailPage.getBody().getSisLabels());
        assertNotEquals(Arrays.asList("Portal", "Start", "Homepage"), fetchThumbnailPage.getBody().getSisLabels());
    }

    @Test
    public void considerNoindexWhileCrawlingTwoSiteConfig() {
        final var siteId = UUID.fromString("f771eb6b-80d6-4e9f-a660-22c9972a8e06");
        final var siteToCrawl = new SitesCrawlStatus(new HashSet<>(Collections.singletonList(new CrawlStatus(siteId, Instant.now(), -1))));
        final ResponseEntity<SitesCrawlStatus> request = caller
                .postForEntity(SiteController.ENDPOINT + "/crawl?serviceSecret=" + SiteTest.ADMIN_SITE_SECRET
                                + "&clearIndex=true&isThrottled=true&allSitesCrawl=true",
                        new HttpEntity<>(siteToCrawl), SitesCrawlStatus.class);

        assertEquals(HttpStatus.OK, request.getStatusCode());
        Objects.requireNonNull(request.getBody()).getSites().stream().filter(crawlStatus -> crawlStatus.getSiteId().equals(siteId)).forEach(crawlStatus -> {
            assertTrue(400 < crawlStatus.getPageCount());
        });
    }

    // TODO test if sitemapsOnly site profile flag is respected
    // TODO test if pageBodyCssSelector site profile info is respected
    @Test
    public void recrawl() {
        final SitesCrawlStatus freshlyCrawledSiteStatus = new SitesCrawlStatus(new HashSet<>(Collections.singletonList(new CrawlStatus(CRAWL_SITE_ID, Instant.now(), -1))));

        // not authenticated crawl
        final ResponseEntity<SitesCrawlStatus> recrawlNotAuthenticated = caller
                .postForEntity(SiteController.ENDPOINT + "/crawl?serviceSecret=" + UUID.randomUUID(),
                        new HttpEntity<>(freshlyCrawledSiteStatus), SitesCrawlStatus.class);
        assertEquals(HttpStatus.BAD_REQUEST, recrawlNotAuthenticated.getStatusCode());

        // fetch recrawled sites set
        final ResponseEntity<SitesCrawlStatus> currentlyRecrawled = caller
                .getForEntity(SiteController.ENDPOINT + "/crawl/status?serviceSecret=" + SiteTest.ADMIN_SITE_SECRET,
                        SitesCrawlStatus.class);
        assertEquals(HttpStatus.OK, currentlyRecrawled.getStatusCode());
        assertTrue(1 < Objects.requireNonNull(currentlyRecrawled.getBody()).getSites().size());

        // crawl freshly crawled site
        final ResponseEntity<SitesCrawlStatus> recrawlFreshSite = caller
                .postForEntity(SiteController.ENDPOINT + "/crawl?allSitesCrawl=true&serviceSecret=" + SiteTest.ADMIN_SITE_SECRET,
                        new HttpEntity<>(freshlyCrawledSiteStatus), SitesCrawlStatus.class);
        assertEquals(HttpStatus.OK, recrawlFreshSite.getStatusCode());
        final SitesCrawlStatus freshCrawlStatus = recrawlFreshSite.getBody();
        assertTrue(1 < Objects.requireNonNull(freshCrawlStatus).getSites().size());
        assertTrue(containsUpdatedSiteId(freshCrawlStatus));
        // TODO use findSearchSiteCrawlStatus where appropriate to validate only the test site ID
        assertTrue(Instant.parse(
                new ArrayList<>(freshlyCrawledSiteStatus.getSites()).get(0).getCrawled())
                .isBefore(Instant.parse(
                        Objects.requireNonNull(getCrawlStatusWithUpdatedSiteId(freshCrawlStatus)).getCrawled())));

        // crawl all sites
        final ResponseEntity<SitesCrawlStatus> recrawl = caller
                .postForEntity(SiteController.ENDPOINT + "/crawl?allSitesCrawl=true&serviceSecret=" + SiteTest.ADMIN_SITE_SECRET,
                        new HttpEntity<>(freshlyCrawledSiteStatus), SitesCrawlStatus.class);

        assertEquals(HttpStatus.OK, recrawl.getStatusCode());
        final SitesCrawlStatus sitesCrawlStatus = recrawl.getBody();
        assertTrue(1 < Objects.requireNonNull(sitesCrawlStatus).getSites().size());
        assertTrue(containsUpdatedSiteId(sitesCrawlStatus));
        assertTrue(Instant.now().isAfter(Instant.parse(Objects.requireNonNull(getCrawlStatusWithUpdatedSiteId(sitesCrawlStatus)).getCrawled())));

        // crawl stale site
        final SitesCrawlStatus staleSiteStatus = new SitesCrawlStatus(new HashSet<>(Collections.singletonList(new CrawlStatus(CRAWL_SITE_ID, Instant.now().minus(1, ChronoUnit.DAYS), -1))));
        final ResponseEntity<SitesCrawlStatus> recrawlStaleSite = caller
                .postForEntity(SiteController.ENDPOINT + "/crawl?allSitesCrawl=true&serviceSecret=" + SiteTest.ADMIN_SITE_SECRET,
                        new HttpEntity<>(staleSiteStatus), SitesCrawlStatus.class);
        assertEquals(HttpStatus.OK, recrawlStaleSite.getStatusCode());
        final SitesCrawlStatus staleCrawlStatus = recrawlStaleSite.getBody();
        assertTrue(1 < Objects.requireNonNull(staleCrawlStatus).getSites().size());
        assertTrue(containsUpdatedSiteId(staleCrawlStatus));
        assertTrue(Instant.parse(
                new ArrayList<>(staleSiteStatus.getSites()).get(0).getCrawled())
                .isBefore(Instant.parse(
                        Objects.requireNonNull(getCrawlStatusWithUpdatedSiteId(staleCrawlStatus)).getCrawled())));
    }

    private CrawlStatus getCrawlStatusWithUpdatedSiteId(SitesCrawlStatus freshCrawlStatus) {
        for (CrawlStatus crawlStatus : freshCrawlStatus.getSites()) {
            if (CRAWL_SITE_ID.equals(crawlStatus.getSiteId())) {
                return crawlStatus;
            }
        }
        return null;
    }

    private boolean containsUpdatedSiteId(SitesCrawlStatus freshCrawlStatus) {
        boolean freshCrawlStatusCheck = false;
        for (CrawlStatus crawlStatus : freshCrawlStatus.getSites()) {
            if (CRAWL_SITE_ID.equals(crawlStatus.getSiteId()) && API_SITE_PAGE_COUNT == crawlStatus.getPageCount()) {
                freshCrawlStatusCheck = true;
            }
        }
        return freshCrawlStatusCheck;
    }
}


