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
import com.intrafind.sitesearch.SmokeTest;
import com.intrafind.sitesearch.controller.PageController;
import com.intrafind.sitesearch.controller.SiteController;
import com.intrafind.sitesearch.dto.CrawlStatus;
import com.intrafind.sitesearch.dto.FetchedPage;
import com.intrafind.sitesearch.dto.SiteCreation;
import com.intrafind.sitesearch.dto.SiteIndexSummary;
import com.intrafind.sitesearch.dto.SitePage;
import com.intrafind.sitesearch.dto.SiteProfile;
import com.intrafind.sitesearch.dto.SiteProfileUpdate;
import com.intrafind.sitesearch.dto.SitesCrawlStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SiteTest {
    private final static Logger LOG = LoggerFactory.getLogger(SiteTest.class);
    static final UUID ADMIN_SITE_SECRET = UUID.fromString(System.getenv("ADMIN_SITE_SECRET"));
    @Autowired
    private TestRestTemplate caller;

    public static SitePage buildPage() {
        final var url = "https://api." + Application.SIS_DOMAIN;
        return new SitePage(
                "Cloud Solution",
                "Site Search is IntraFind's on-demand solution for site search.",
                url,
                Arrays.asList("mars", "Venus")
        );
    }

    @Before
    public void init() {
    }

    private static final List<SiteProfile.Config> configs = Arrays.asList(
            new SiteProfile.Config(URI.create("https://subdomain.example.com"), SiteProfile.Config.DEFAULT_PAGE_BODY_CSS_SELECTOR, true, false),
            new SiteProfile.Config(URI.create("https://example.com"), SiteProfile.Config.DEFAULT_PAGE_BODY_CSS_SELECTOR, false, true)
    );

    private FetchedPage createNewPage(UUID siteId, UUID siteSecret) {
        final var simple = buildPage();
        final var newlyCreatedPage = caller.exchange(SiteController.ENDPOINT + "/" + siteId + "/pages?siteSecret=" + siteSecret, HttpMethod.PUT, new HttpEntity<>(simple), FetchedPage.class);
        assertEquals(HttpStatus.OK, newlyCreatedPage.getStatusCode());
        assertNotNull(newlyCreatedPage.getBody());
        assertNotNull(newlyCreatedPage.getBody().getBody());
        assertFalse(newlyCreatedPage.getBody().getBody().isEmpty());
        assertNotNull(newlyCreatedPage.getBody().getTitle());
        assertFalse(newlyCreatedPage.getBody().getTitle().isEmpty());
        assertNotNull(newlyCreatedPage.getBody().getUrl());
        assertFalse(newlyCreatedPage.getBody().getUrl().isEmpty());
        assertFalse(newlyCreatedPage.getBody().getSisLabels().isEmpty());
        assertEquals(Arrays.asList("mars", "Venus"), newlyCreatedPage.getBody().getSisLabels());

        return newlyCreatedPage.getBody();
    }

    private static final SiteProfileUpdate SITE_PROFILE_CREATION = new SiteProfileUpdate(
            configs,
            CrawlerTest.TEST_EMAIL_ADDRESS
    );

    private CrawlStatus findSearchSiteCrawlStatus(SitesCrawlStatus sitesCrawlStatus) {
        return sitesCrawlStatus.getSites().stream().filter(siteStatus -> siteStatus.getSiteId().equals(CrawlerTest.CRAWL_SITE_ID)).findAny().get();
    }

    @Test
    public void fetchAndUpdateCrawlStatus() {
        final var crawlStatus = caller.exchange(SiteController.ENDPOINT + "/crawl/status?serviceSecret=" +
                ADMIN_SITE_SECRET, HttpMethod.GET, HttpEntity.EMPTY, SitesCrawlStatus.class);
        assertEquals(HttpStatus.OK, crawlStatus.getStatusCode());
        final var initSize = Objects.requireNonNull(crawlStatus.getBody()).getSites().size();
        assertTrue(1 <= initSize);
        assertNotNull(findSearchSiteCrawlStatus(crawlStatus.getBody()).getSiteId());
        assertTrue(Instant.now().isAfter(Instant.parse(findSearchSiteCrawlStatus(crawlStatus.getBody()).getCrawled())));

        // update crawl status of a specific site
        final var now = Instant.now();
        final var updatedCrawlStatus = crawlStatus.getBody();
        final var searchSiteCrawlStatus = findSearchSiteCrawlStatus(updatedCrawlStatus);
        assertNotEquals(now.toString(), searchSiteCrawlStatus.getCrawled());
        searchSiteCrawlStatus.setCrawled(now.toString());
        updatedCrawlStatus.getSites().add(searchSiteCrawlStatus);
        final var crawlStatusUpdate = caller.exchange(SiteController.ENDPOINT + "/crawl/status?serviceSecret=" +
                ADMIN_SITE_SECRET, HttpMethod.PUT, new HttpEntity<>(updatedCrawlStatus), SitesCrawlStatus.class);
        assertEquals(HttpStatus.OK, crawlStatus.getStatusCode());
        assertNotNull(findSearchSiteCrawlStatus(Objects.requireNonNull(crawlStatusUpdate.getBody())).getSiteId());
        assertEquals(now, Instant.parse(findSearchSiteCrawlStatus(crawlStatusUpdate.getBody()).getCrawled()));
        assertTrue(Instant.now().isAfter(Instant.parse(findSearchSiteCrawlStatus(crawlStatusUpdate.getBody()).getCrawled())));

        // verify crawl status of a specific site
        final var crawlStatusUpdated = caller.exchange(SiteController.ENDPOINT + "/crawl/status?serviceSecret=" +
                ADMIN_SITE_SECRET, HttpMethod.GET, HttpEntity.EMPTY, SitesCrawlStatus.class);
        assertEquals(HttpStatus.OK, crawlStatusUpdated.getStatusCode());
        assertEquals(initSize, Objects.requireNonNull(crawlStatusUpdated.getBody()).getSites().size());
        assertNotNull(findSearchSiteCrawlStatus(crawlStatusUpdated.getBody()).getSiteId());
        assertEquals(now, Instant.parse(findSearchSiteCrawlStatus(crawlStatusUpdated.getBody()).getCrawled()));
        assertTrue(Instant.now().isAfter(Instant.parse(findSearchSiteCrawlStatus(crawlStatusUpdated.getBody()).getCrawled())));
    }

    private SiteCreation createNewSite(SiteProfileUpdate siteProfileCreation) {
        final var actual = caller.exchange(SiteController.ENDPOINT, HttpMethod.POST, new HttpEntity<>(siteProfileCreation), SiteCreation.class);

        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertNotNull(actual.getBody());
        assertNotNull(actual.getBody().getSiteId());
        assertNotNull(actual.getBody().getSiteSecret());
        assertEquals(SmokeTest.SITES_API + actual.getBody().getSiteId(), Objects.requireNonNull(actual.getHeaders().get(HttpHeaders.LOCATION)).get(0));

        return actual.getBody();
    }

    @Test
    public void createNewSiteWithProfile() {
        final var createdSiteProfile = createNewSite(SITE_PROFILE_CREATION);

        final var actual = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() +
                "/profile?siteSecret=" + createdSiteProfile.getSiteSecret(), HttpMethod.GET, HttpEntity.EMPTY, SiteProfile.class);
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(createdSiteProfile.getSiteId(), Objects.requireNonNull(actual.getBody()).getId());
        assertEquals(createdSiteProfile.getSiteSecret(), actual.getBody().getSecret());
        assertEquals(CrawlerTest.TEST_EMAIL_ADDRESS, actual.getBody().getEmail());
        assertEquals(configs, actual.getBody().getConfigs());

        var siteProfileWithAdminSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() +
                "/profile?siteSecret=" + ADMIN_SITE_SECRET, HttpMethod.GET, HttpEntity.EMPTY, SiteProfile.class);
        assertEquals(HttpStatus.OK, siteProfileWithAdminSecret.getStatusCode());

        var siteProfileWithInvalidSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() +
                "/profile?siteSecret=" + UUID.randomUUID(), HttpMethod.GET, HttpEntity.EMPTY, SiteProfile.class);
        assertEquals(HttpStatus.NOT_FOUND, siteProfileWithInvalidSecret.getStatusCode());

        // update site profile
        final var updateSiteProfileConfigs = new ArrayList<>(configs);

        final var siteProfileUpdate = new SiteProfileUpdate(createdSiteProfile.getSiteSecret(), "update." + CrawlerTest.TEST_EMAIL_ADDRESS, configs);
        final var updatedSite = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() + "/profile?siteSecret=" + createdSiteProfile.getSiteSecret(),
                HttpMethod.PUT, new HttpEntity<>(siteProfileUpdate), SiteProfileUpdate.class);
        assertEquals(createdSiteProfile.getSiteSecret(), Objects.requireNonNull(updatedSite.getBody()).getSecret());
        assertEquals("update." + CrawlerTest.TEST_EMAIL_ADDRESS, updatedSite.getBody().getEmail());
        assertEquals(configs, updatedSite.getBody().getConfigs());
        assertEquals(2, updatedSite.getBody().getConfigs().size());
        assertEquals(SiteProfile.Config.DEFAULT_PAGE_BODY_CSS_SELECTOR, updatedSite.getBody().getConfigs().get(0).getPageBodyCssSelector());
        assertTrue(updatedSite.getBody().getConfigs().stream().anyMatch(SiteProfile.Config::isAllowUrlWithQuery));
        assertFalse(updatedSite.getBody().getConfigs().get(0).isAllowUrlWithQuery());
        assertTrue(updatedSite.getBody().getConfigs().get(1).isAllowUrlWithQuery());
        assertTrue(updatedSite.getBody().getConfigs().get(0).isSitemapsOnly());
        assertFalse(updatedSite.getBody().getConfigs().get(1).isSitemapsOnly());

        // assure site profile is impossible with wrong site secret
        final var updatedSiteWithInvalidSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() + "/profile?siteSecret=" + UUID.randomUUID(),
                HttpMethod.PUT, new HttpEntity<>(siteProfileUpdate), SiteProfileUpdate.class);
        assertEquals(HttpStatus.NOT_FOUND, updatedSiteWithInvalidSecret.getStatusCode());

        final var updatedSiteWithAdminSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() + "/profile?siteSecret=" + ADMIN_SITE_SECRET,
                HttpMethod.PUT, new HttpEntity<>(siteProfileUpdate), SiteProfileUpdate.class);
        assertEquals(HttpStatus.NOT_FOUND, updatedSiteWithAdminSecret.getStatusCode());

        // update site profile's secret
        final var newSiteSecret = UUID.randomUUID();
        final var siteProfileUpdateWithSecret = new SiteProfileUpdate(newSiteSecret, "update." + CrawlerTest.TEST_EMAIL_ADDRESS, updateSiteProfileConfigs);
        final var updatedSiteWithSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() + "/profile?siteSecret=" + createdSiteProfile.getSiteSecret(),
                HttpMethod.PUT, new HttpEntity<>(siteProfileUpdateWithSecret), SiteProfileUpdate.class);
        assertEquals(newSiteSecret, updatedSiteWithSecret.getBody().getSecret());
        assertEquals("update." + CrawlerTest.TEST_EMAIL_ADDRESS, updatedSiteWithSecret.getBody().getEmail());
        assertEquals(new HashSet<>(updateSiteProfileConfigs), new HashSet<>(updatedSiteWithSecret.getBody().getConfigs()));
        assertEquals(updateSiteProfileConfigs.size(), updatedSiteWithSecret.getBody().getConfigs().size());
        assertEquals(newSiteSecret, updatedSiteWithSecret.getBody().getSecret());

        // fetching profile with update site secret works
        final var fetchSiteProfileWithNewSiteSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() + "/profile?siteSecret=" + newSiteSecret,
                HttpMethod.GET, new HttpEntity<>(siteProfileUpdateWithSecret), SiteProfileUpdate.class);
        assertEquals(HttpStatus.OK, fetchSiteProfileWithNewSiteSecret.getStatusCode());
        assertEquals(newSiteSecret, fetchSiteProfileWithNewSiteSecret.getBody().getSecret());

        // fetching profile with old site secret does not work
        final var fetchSiteProfileWithOldSiteSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSiteProfile.getSiteId() + "/profile?siteSecret=" + createdSiteProfile.getSiteSecret(),
                HttpMethod.GET, new HttpEntity<>(siteProfileUpdateWithSecret), SiteProfileUpdate.class);
        assertEquals(HttpStatus.NOT_FOUND, fetchSiteProfileWithOldSiteSecret.getStatusCode());
    }

    @Test
    public void updateSiteViaUrl() throws Exception {
        final var newSite = createNewSite(null);
        final var newPage = createNewPage(newSite.getSiteId(), newSite.getSiteSecret());
        final var updatedBodyContent = "Updated via Hash(siteId, URL)";
        newPage.setBody(updatedBodyContent);

        TimeUnit.MILLISECONDS.sleep(8_000);

        // update
        final var updatedSite = caller.exchange(SiteController.ENDPOINT
                        + "/" + newSite.getSiteId() + "/pages?siteSecret=" + newSite.getSiteSecret(),
                HttpMethod.PUT, new HttpEntity<>(newPage), FetchedPage.class);
        assertEquals(HttpStatus.OK, updatedSite.getStatusCode());
        assertEquals(newPage.getId(), updatedSite.getBody().getId());
        assertEquals(updatedBodyContent, updatedSite.getBody().getBody());

        // fetch & check updated site
        assertEquals(SitePage.hashPageId(newPage.getSiteId(), newPage.getUrl()), newPage.getId());
        final var fetchedUpdatedSite = caller.exchange(PageController.ENDPOINT
                        + "/" + SitePage.hashPageId(newSite.getSiteId(), newPage.getUrl()),
                HttpMethod.GET, HttpEntity.EMPTY, FetchedPage.class);

        assertEquals(HttpStatus.OK, fetchedUpdatedSite.getStatusCode());
        assertEquals(newPage.getId(), fetchedUpdatedSite.getBody().getId());
        assertEquals(updatedBodyContent, fetchedUpdatedSite.getBody().getBody());

        // delete using an invalid siteSecret
        final var invalidSiteSecret = UUID.randomUUID();
        final var deletionWithInvalidSiteSecret = caller.exchange(SiteController.ENDPOINT + "/" + newSite.getSiteId() + "/pages?siteSecret=" + invalidSiteSecret + "&url=" + newPage.getUrl(), HttpMethod.DELETE, HttpEntity.EMPTY, ResponseEntity.class);
        assertEquals(HttpStatus.NOT_FOUND, deletionWithInvalidSiteSecret.getStatusCode());
        assertNull(deletionWithInvalidSiteSecret.getBody());

        // fetch via URL
        final var fetchViaUrl = caller.exchange(SiteController.ENDPOINT
                        + "/" + newSite.getSiteId() + "/pages?url=" + newPage.getUrl(),
                HttpMethod.GET, HttpEntity.EMPTY, FetchedPage.class);
        assertEquals(HttpStatus.OK, fetchViaUrl.getStatusCode());
        assertEquals(newPage.getId(), fetchViaUrl.getBody().getId());
        assertEquals(updatedBodyContent, fetchViaUrl.getBody().getBody());
        assertEquals(newPage.getUrl(), fetchViaUrl.getBody().getUrl());

        // delete using a valid siteSecret
        final var deletion = caller.exchange(SiteController.ENDPOINT + "/" + newSite.getSiteId() + "/pages?siteSecret=" + newSite.getSiteSecret() + "&url=" + newPage.getUrl(),
                HttpMethod.DELETE, HttpEntity.EMPTY, ResponseEntity.class);
        assertEquals(HttpStatus.NO_CONTENT, deletion.getStatusCode());
        assertNull(deletion.getBody());

        TimeUnit.MILLISECONDS.sleep(2_000);
        // fetch via URL an already deleted page
        final var fetchViaUrlForNonExistingPage = caller.exchange(SiteController.ENDPOINT
                        + "/" + newSite.getSiteId() + "/pages?url=" + newPage.getUrl(),
                HttpMethod.GET, HttpEntity.EMPTY, SitePage.class);
        assertEquals(HttpStatus.NOT_FOUND, fetchViaUrlForNonExistingPage.getStatusCode());
    }

    @Test
    public void fetchUpdatedById() throws Exception {
        final var newSiteYing = createNewSite(null);
        final var ying = createNewPage(newSiteYing.getSiteId(), newSiteYing.getSiteSecret());
        final var newSiteYang = createNewSite(null);
        final var yang = createNewPage(newSiteYang.getSiteId(), newSiteYang.getSiteSecret());
        TimeUnit.MILLISECONDS.sleep(8_000);

        final var actualYing = caller.exchange(SiteController.ENDPOINT + "/"
                + newSiteYing.getSiteId() + "/pages/" + ying.getId() + "?siteSecret=" + newSiteYing.getSiteSecret(), HttpMethod.PUT, new HttpEntity<>(ying), FetchedPage.class);
        assertEquals(HttpStatus.OK, actualYing.getStatusCode());
        assertEquals(ying, actualYing.getBody());
        final var actualYang = caller.exchange(SiteController.ENDPOINT + "/"
                + newSiteYang.getSiteId() + "/pages/" + yang.getId() + "?siteSecret=" + newSiteYang.getSiteSecret(), HttpMethod.PUT, new HttpEntity<>(yang), FetchedPage.class);
        assertEquals(HttpStatus.OK, actualYang.getStatusCode());
        assertEquals(yang, actualYang.getBody());

        final var actualYingFetched = caller.getForEntity(PageController.ENDPOINT + "/" + ying.getId(), FetchedPage.class);
        assertEquals(HttpStatus.OK, actualYingFetched.getStatusCode());
        assertEquals(ying, actualYingFetched.getBody());

        final var actualYangFetched = caller.getForEntity(PageController.ENDPOINT + "/" + yang.getId(), FetchedPage.class);
        assertEquals(HttpStatus.OK, actualYangFetched.getStatusCode());
        assertEquals(yang, actualYangFetched.getBody());

        final var fetchedYing = actualYingFetched.getBody();
        final var fetchedYang = actualYangFetched.getBody();
        assertEquals(fetchedYing, fetchedYang);
        assertNotEquals(fetchedYing.getId(), fetchedYang.getId());
        assertNotEquals(fetchedYing.getSiteId(), fetchedYang.getSiteId());
    }

    @Test
    public void updatedSite() throws Exception {
        final var createdSite = createNewSite(null);
        final var createdPage = createNewPage(createdSite.getSiteId(), createdSite.getSiteSecret());

        TimeUnit.MILLISECONDS.sleep(8_000);

        final var pageChecksum = 923522;
        final var updateWithSiteIdOnly = caller.exchange(SiteController.ENDPOINT + "/" + createdSite.getSiteId()
                + "/pages/" + createdPage.getId(), HttpMethod.PUT, new HttpEntity<>(createdPage), SitePage.class);
        assertEquals("only valid siteId is provided", HttpStatus.BAD_REQUEST, updateWithSiteIdOnly.getStatusCode());
        assertEquals(pageChecksum, updateWithSiteIdOnly.getBody().hashCode());

        final var updateWithSiteSecretOnly = caller.exchange(SiteController.ENDPOINT + "/" + createdPage.getSiteId()
                + "/pages/" + createdPage.getId(), HttpMethod.PUT, new HttpEntity<>(createdPage), SitePage.class);
        assertEquals("only valid siteSecret is provided", HttpStatus.BAD_REQUEST, updateWithSiteSecretOnly.getStatusCode());
        assertEquals(pageChecksum, updateWithSiteSecretOnly.getBody().hashCode());

        final var updateWithWrongSiteSecret = caller.exchange(SiteController.ENDPOINT + "/" + createdSite.getSiteId()
                        + "/pages/" + createdPage.getId() + "?siteSecret=" + UUID.randomUUID(),
                HttpMethod.PUT, new HttpEntity<>(createdPage), SitePage.class);
        assertEquals("siteSecret is invalid", HttpStatus.NOT_FOUND, updateWithWrongSiteSecret.getStatusCode());
        assertNull(updateWithWrongSiteSecret.getBody());

        createdPage.setTitle("updated title");
        createdPage.setBody("updated body");
        createdPage.setUrl("https://example.com/updated");
        final var beforePageUpdate = Instant.now();
        final var updated = caller.exchange(SiteController.ENDPOINT + "/" + createdSite.getSiteId()
                        + "/pages/" + createdPage.getId() + "?siteSecret=" + createdSite.getSiteSecret(),
                HttpMethod.PUT, new HttpEntity<>(createdPage), FetchedPage.class);
        assertEquals(HttpStatus.OK, updated.getStatusCode());
        assertEquals(createdPage, updated.getBody());
        assertEquals("updated body", updated.getBody().getBody());
        assertEquals(Arrays.asList("mars", "Venus"), updated.getBody().getSisLabels());
        // assert correct timestamp update
        assertTrue(beforePageUpdate.isBefore(Instant.parse(updated.getBody().getTimestamp())));
        assertTrue(Instant.now().isAfter(Instant.parse(updated.getBody().getTimestamp())));

        final var updateWithInvalidPageId = caller.exchange(SiteController.ENDPOINT + "/" + createdSite.getSiteId()
                        + "/pages/" + "invalidSomething" + "?siteSecret=" + createdSite.getSiteSecret(),
                HttpMethod.PUT, new HttpEntity<>(createdPage), SitePage.class);
        assertEquals(HttpStatus.BAD_REQUEST, updateWithInvalidPageId.getStatusCode());
        assertNull(updateWithInvalidPageId.getBody());
    }

    @Test
    public void importFeedAndClearSite() throws Exception {
        final var exchange = caller.exchange(SiteController.ENDPOINT + "/rss?feedUrl=http://intrafind.de/share/enterprise-search-blog.xml",
                HttpMethod.POST, HttpEntity.EMPTY, SiteIndexSummary.class);
        final var creation = validateTenantSummary(exchange, 25);

        TimeUnit.MILLISECONDS.sleep(13_000);
        validateUpdatedSites(creation);

        final var clearSite = caller.exchange(SiteController.ENDPOINT + "/" + creation.getSiteId() + "?siteSecret=" + creation.getSiteSecret(),
                HttpMethod.DELETE, HttpEntity.EMPTY, Object.class);
        assertNull(clearSite.getBody());
        assertEquals(HttpStatus.OK, clearSite.getStatusCode());
        TimeUnit.MILLISECONDS.sleep(18_000);
        assureClearedSite(creation);
    }

    private void assureClearedSite(SiteIndexSummary siteIndexSummary) {
        siteIndexSummary.getDocuments().forEach(docId -> {
            final var fetchedById = caller.exchange(
                    PageController.ENDPOINT + "/" + docId, HttpMethod.GET, HttpEntity.EMPTY, FetchedPage.class);
            assertEquals(HttpStatus.NOT_FOUND, fetchedById.getStatusCode());
            assertNull(fetchedById.getBody());
        });
    }

    private void validateUpdatedSites(SiteIndexSummary siteIndexSummary) {
        siteIndexSummary.getDocuments().forEach(documentId -> {
            final var fetchedById = caller.exchange(
                    PageController.ENDPOINT + "/" + documentId, HttpMethod.GET, HttpEntity.EMPTY, FetchedPage.class);
            assertEquals(HttpStatus.OK, fetchedById.getStatusCode());
            assertEquals(siteIndexSummary.getSiteId(), fetchedById.getBody().getSiteId());
            assertFalse(fetchedById.getBody().getBody().isEmpty());
            assertNotNull(fetchedById.getBody().getUrl());
        });
    }

    @Test
    public void importFeedStrippingHtml() throws Exception {
        // create index with stripped HTML tags
        final var initialIndexCreation = caller.exchange(
                SiteController.ENDPOINT + "/rss?feedUrl=http://intrafind.de/share/enterprise-search-blog.xml&stripHtmlTags=true",
                HttpMethod.POST, HttpEntity.EMPTY, SiteIndexSummary.class);
        TimeUnit.MILLISECONDS.sleep(8_000);
        final var siteIndexSummaryCreation = validateTenantSummary(initialIndexCreation, 25);

        final var siteIdFromCreation = siteIndexSummaryCreation.getSiteId();
        assertNotNull(siteIdFromCreation);
        final var siteSecretFromCreation = siteIndexSummaryCreation.getSiteSecret();
        assertNotNull(siteSecretFromCreation);
    }

    @Test
    public void importFeedAndUpdate() throws Exception {
        // create index
        final var initialIndexCreation = caller.exchange(
                SiteController.ENDPOINT + "/rss?feedUrl=https://raw.githubusercontent.com/intrafind/if-sitesearch/master/service/src/test/resources/steem-blockchain-rss-feed-init.xml",
                HttpMethod.POST, HttpEntity.EMPTY, SiteIndexSummary.class);
        TimeUnit.MILLISECONDS.sleep(8_000);
        final var siteIndexSummaryCreation = validateTenantSummary(initialIndexCreation, 10);

        final var siteIdFromCreation = siteIndexSummaryCreation.getSiteId();
        final var siteSecretFromCreation = siteIndexSummaryCreation.getSiteSecret();

        final var updateWithoutSecret = caller.exchange(
                SiteController.ENDPOINT + "/" + siteIdFromCreation + "/rss?feedUrl=http://intrafind.de/share/enterprise-search-blog.xml",
                HttpMethod.PUT, HttpEntity.EMPTY, SiteIndexSummary.class);
        assertEquals(HttpStatus.BAD_REQUEST, updateWithoutSecret.getStatusCode());

        final var updateWithInvalidSecret = caller.exchange(
                SiteController.ENDPOINT + "/" + siteIdFromCreation + "/rss?feedUrl=http://intrafind.de/share/enterprise-search-blog.xml"
                        + "&siteSecret=" + UUID.randomUUID(),
                HttpMethod.PUT, HttpEntity.EMPTY, SiteIndexSummary.class);
        assertEquals(HttpStatus.BAD_REQUEST, updateWithInvalidSecret.getStatusCode());


        // update index
        final var anotherFeedReplacement = caller.exchange(
                SiteController.ENDPOINT + "/" + siteIdFromCreation + "/rss?feedUrl=http://intrafind.de/share/enterprise-search-blog.xml"
                        + "&siteSecret=" + siteSecretFromCreation,
                HttpMethod.PUT, HttpEntity.EMPTY, SiteIndexSummary.class);
        final var siteIndexSummaryUpdate = validateTenantSummary(anotherFeedReplacement, 25);

        validateUpdatedSites(siteIndexSummaryUpdate);

        tryDeletionOfSites(siteIdFromCreation, siteSecretFromCreation);
    }

    private void tryDeletionOfSites(UUID siteIdFromCreation, UUID siteSecretFromCreation) {
        final var fetchAll = caller.exchange(SiteController.ENDPOINT + "/" + siteIdFromCreation, HttpMethod.GET, HttpEntity.EMPTY, List.class);
        assertEquals(HttpStatus.OK, fetchAll.getStatusCode());
        @SuppressWarnings("unchecked") final List<String> pages = fetchAll.getBody();
        assertTrue(1 < pages.size());
        int siteCountBeforeDeletion = pages.size();

        for (final String pageId : pages) {
            // delete using an invalid siteSecret
            final var invalidSiteSecret = UUID.randomUUID();
            final var deletionWithInvalidSiteSecret = caller.exchange(SiteController.ENDPOINT + "/" + siteIdFromCreation + "/pages/" + pageId + "?siteSecret=" + invalidSiteSecret, HttpMethod.DELETE, HttpEntity.EMPTY, ResponseEntity.class);
            assertEquals(HttpStatus.NOT_FOUND, deletionWithInvalidSiteSecret.getStatusCode());
            assertNull(deletionWithInvalidSiteSecret.getBody());

            // delete using a valid siteSecret
            final var deletion = caller.exchange(SiteController.ENDPOINT + "/" + siteIdFromCreation + "/pages/" + pageId + "?siteSecret=" + siteSecretFromCreation, HttpMethod.DELETE, HttpEntity.EMPTY, ResponseEntity.class);
            assertEquals(HttpStatus.NO_CONTENT, deletion.getStatusCode());
            assertNull(deletion.getBody());
        }

        pages.forEach(pageId -> {
            LOG.info("pageId: " + pageId);
            final ResponseEntity<ResponseEntity> deletion = caller.exchange(SiteController.ENDPOINT + "/" + siteIdFromCreation + "/pages/" + pageId + "?siteSecret=" + siteSecretFromCreation, HttpMethod.DELETE, HttpEntity.EMPTY, ResponseEntity.class);
            assertEquals(HttpStatus.NO_CONTENT, deletion.getStatusCode());
            assertNull(deletion.getBody());
        });
        LOG.info("siteCountBeforeDeletion: " + siteCountBeforeDeletion);
    }

    private SiteIndexSummary validateTenantSummary(ResponseEntity<SiteIndexSummary> anotherFeedReplacement, int indexEntriesCount) {
        assertEquals(HttpStatus.OK, anotherFeedReplacement.getStatusCode());
        final var siteIndexSummaryUpdate = anotherFeedReplacement.getBody();
        assertNotNull(siteIndexSummaryUpdate.getSiteId());
        assertNotNull(siteIndexSummaryUpdate.getSiteSecret());
        assertEquals(indexEntriesCount, siteIndexSummaryUpdate.getSuccessCount());
        assertEquals(indexEntriesCount, siteIndexSummaryUpdate.getDocuments().size());
        assertTrue(siteIndexSummaryUpdate.getFailed().isEmpty());
        return siteIndexSummaryUpdate;
    }

    @Test
    public void indexIntrafindDe() throws Exception {
        final var newSite = createNewSite(null);
        final var enIndexDocuments = new ArrayList<String>();
        enIndexDocuments.add("en/2b4c27b0-6636-4a13-a911-4f495f99b604.xml");
        enIndexDocuments.add("en/32d2557e-7f03-48d9-ad60-bf7c0b70c487.xml");
        enIndexDocuments.add("en/534706ba-da98-4b45-b920-8ec0486d79fb.xml");
        enIndexDocuments.add("en/79f4cd25-39d1-42ad-8b2a-9247aabd7d13.xml");

        // create index without clearance
        final var siteIndexSummary = indexCrawlerPage(
                enIndexDocuments.get(0),
                newSite.getSiteId(),
                newSite.getSiteSecret(),
                false
        );

        validateUpdatedSites(siteIndexSummary);
        TimeUnit.MILLISECONDS.sleep(13_000);

        final var allPages = caller.exchange(SiteController.ENDPOINT + "/" + newSite.getSiteId(),
                HttpMethod.GET, HttpEntity.EMPTY, List.class);
        final var pageIds = allPages.getBody();
        assertEquals(siteIndexSummary.getDocuments().size(), pageIds.size());

        // update index without clearance
        final var siteIndexSummaryAfterUpdate = indexCrawlerPage(
                enIndexDocuments.get(1),
                newSite.getSiteId(),
                newSite.getSiteSecret(),
                false
        );
        validateUpdatedSites(siteIndexSummaryAfterUpdate);
        TimeUnit.MILLISECONDS.sleep(13_000);

        final var allPagesAfterUpdate = caller.exchange(SiteController.ENDPOINT + "/" + newSite.getSiteId(),
                HttpMethod.GET, HttpEntity.EMPTY, List.class);
        final var allPageIdsAfterUpdate = allPagesAfterUpdate.getBody();
        assertEquals(siteIndexSummary.getDocuments().size() + siteIndexSummaryAfterUpdate.getDocuments().size(), allPageIdsAfterUpdate.size());

        // create index with clearance
        final var siteIndexSummaryAfterClearance = indexCrawlerPage(enIndexDocuments.get(2),
                newSite.getSiteId(),
                newSite.getSiteSecret(), true
        );
        validateUpdatedSites(siteIndexSummaryAfterClearance);
        TimeUnit.MILLISECONDS.sleep(13_000);

        final var allPagesAfterClearance = caller.exchange(SiteController.ENDPOINT + "/" + newSite.getSiteId(),
                HttpMethod.GET, HttpEntity.EMPTY, List.class);
        @SuppressWarnings("unchecked") final var allPageIdsAfterClearance = allPagesAfterClearance.getBody();
        assertEquals(siteIndexSummaryAfterClearance.getDocuments().size(), allPageIdsAfterClearance.size());
    }

    private SiteIndexSummary indexCrawlerPage(String indexedDocumentsPage, UUID siteId, UUID siteSecret, Boolean clearIndex) {
        final var response = caller.exchange(SiteController.ENDPOINT + "/" + siteId + "/xml" +
                        "?xmlUrl=https://raw.githubusercontent.com/intrafind/if-sitesearch/master/service/src/test/resources/intrafind-de/" +
                        indexedDocumentsPage + "&siteSecret=" + siteSecret + "&clearIndex=" + clearIndex,
                HttpMethod.PUT, HttpEntity.EMPTY, SiteIndexSummary.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getFailed().isEmpty());
        assertFalse(response.getBody().getDocuments().isEmpty());
        assertTrue(response.getBody().getSuccessCount() > 0);

        return response.getBody();
    }
}