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

package com.intrafind.sitesearch.service;

import com.intrafind.api.Document;
import com.intrafind.api.Fields;
import com.intrafind.api.index.Index;
import com.intrafind.api.search.Hits;
import com.intrafind.api.search.Search;
import com.intrafind.sitesearch.Application;
import com.intrafind.sitesearch.BaseConfig;
import com.intrafind.sitesearch.dto.CrawlStatus;
import com.intrafind.sitesearch.dto.FetchedPage;
import com.intrafind.sitesearch.dto.IndexCleanupResult;
import com.intrafind.sitesearch.dto.SiteCreation;
import com.intrafind.sitesearch.dto.SiteIndexSummary;
import com.intrafind.sitesearch.dto.SitePage;
import com.intrafind.sitesearch.dto.SiteProfile;
import com.intrafind.sitesearch.dto.SiteProfileUpdate;
import com.intrafind.sitesearch.dto.SitesCrawlStatus;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class SiteService {
    private static final Logger LOG = LoggerFactory.getLogger(SiteService.class);
    private final Search searchService;
    private final Index indexService;
    /**
     * This env initialization is just broken on Windows.
     */
    public static final UUID ADMIN_SITE_SECRET = UUID.fromString(System.getenv("ADMIN_SITE_SECRET"));

    static final String SITE_CONFIGURATION_DOCUMENT_PREFIX = "site-configuration-";
    static final String CRAWL_STATUS_SINGLETON_DOCUMENT = "crawl-status";
    /**
     * Field is updated whenever a document is (re-)indexed.
     */
    private static final String PAGE_TIMESTAMP = "timestamp";
    public static final String PAGE_LABELS = "_store.sisLabels";
    static final String PAGE_THUMBNAIL_META_NAME = "thumbnail";
    static final String PAGE_LABELS_META_NAME = "sis-labels";
    public static final String PAGE_THUMBNAIL = "_store.thumbnail";
    private static final String PAGE_TIMESTAMP_NEW = "_store.timestamp";

    @Autowired
    public SiteService(final Search searchService, Index indexService) {
        this.searchService = searchService;
        this.indexService = indexService;
    }

    public Optional<FetchedPage> indexExistingPage(String id, UUID siteId, UUID siteSecret, SitePage page) {
        if (siteId != null && siteSecret != null) { // credentials are provided as a tuple only
            final var fetchedSiteSecret = fetchSiteSecret(siteId);
            if (fetchedSiteSecret.isEmpty()) { // site does not exist
                return Optional.empty();
            } else if (siteSecret.equals(fetchedSiteSecret.get())) { // authorized
                return indexDocument(id, siteId, page);
            } else { // unauthorized
                return Optional.empty();
            }
        } else if (siteId == null ^ siteSecret == null) { // it does not make any sense if only one of the parameters is set
            return Optional.empty();
        } else { // consider request as first-usage-ownership-granting request, create new index
            return indexDocument(SitePage.hashPageId(UUID.randomUUID(), page.getUrl()), UUID.randomUUID(), page);
        }
    }

    public boolean isAllowedToModify(UUID siteId, UUID siteSecret) {
        final var fetchedSiteSecret = fetchSiteSecret(siteId);
        return fetchedSiteSecret.isPresent() && siteSecret.equals(fetchedSiteSecret.get());
    }

    private Optional<FetchedPage> indexDocument(final String id, final UUID siteId, final SitePage page) {
        final var doc = new Document(id);
        doc.set(Fields.BODY, page.getBody());
        doc.set(Fields.TITLE, page.getTitle());
        doc.set(Fields.URL, page.getUrl());
        doc.set(Fields.TENANT, siteId);
        doc.set(PAGE_LABELS, page.getSisLabels() == null ? Collections.emptyList() : page.getSisLabels()); // TODO implement tests, expose via API for both indexing & search
        doc.set(PAGE_THUMBNAIL, page.getThumbnail() == null ? "" : page.getThumbnail());
        final var now = Instant.now();
        doc.set(PAGE_TIMESTAMP, now);
        doc.set(PAGE_TIMESTAMP_NEW, now);
        indexService.index(doc);

        LOG.info("siteId: " + siteId + " - bodySize: " + page.getBody().length() + " - titleSize: " + page.getTitle().length() + " - URL: " + page.getUrl());

        return fetchById(id);
    }

    public Optional<SiteProfile> fetchSiteProfile(UUID siteId, UUID siteSecret) {
        if (ADMIN_SITE_SECRET.equals(siteSecret)) {
            return fetchSiteProfile(siteId);
        } else {
            final var actualSiteSecret = fetchSiteSecret(siteId);
            if (actualSiteSecret.isPresent() && actualSiteSecret.get().equals(siteSecret)) {
                return fetchSiteProfile(siteId);
            } else {
                return Optional.empty();
            }
        }
    }

    public Optional<SiteProfile> fetchSiteProfile(UUID siteId) {
        final var siteProfile = indexService.fetch(Index.ALL, SITE_CONFIGURATION_DOCUMENT_PREFIX + siteId).stream().findAny();
        return siteProfile.map(document -> {
            final Set<URI> urls;
            if (document.getAll("urls") == null) {
                urls = Collections.emptySet();
            } else {
                urls = document.getAll("urls").stream().map(URI::create).collect(Collectors.toSet());
            }

            final String email;
            if (document.get("email") == null) {
                email = "";
            } else {
                email = document.get("email");
            }

            final var configs = new ArrayList<SiteProfile.Config>();
            urls.forEach(configUrl -> {
                final var config = document.getAll(configUrl.toString());
                if (config != null && config.size() > 1) {
                    final boolean allowUrlWithQuery;
                    if (config.size() > 2) {
                        allowUrlWithQuery = Boolean.valueOf(config.get(2));
                    } else {
                        allowUrlWithQuery = false;
                    }
                    configs.add(new SiteProfile.Config(configUrl, config.get(0), Boolean.valueOf(config.get(1)), allowUrlWithQuery));
                } else {
                    configs.add(new SiteProfile.Config(configUrl, SiteProfile.Config.DEFAULT_PAGE_BODY_CSS_SELECTOR, false, false));
                }
            });

            return new SiteProfile(siteId, UUID.fromString(Objects.requireNonNull(document.get("secret"))), email, configs);
        });
    }

    public Optional<UUID> fetchSiteSecret(UUID siteId) {
        final var siteConfiguration = indexService.fetch(Index.ALL, SITE_CONFIGURATION_DOCUMENT_PREFIX + siteId).stream().findAny();
        return siteConfiguration.map(document -> UUID.fromString(Objects.requireNonNull(document.get("secret"))));
    }

    public Optional<List<String>> fetchAllDocuments(final UUID siteId) {
        final var documentWithSiteSecret = searchService.search(
                Fields.TENANT + ":" + siteId.toString(),
                Search.RETURN_FIELDS, Fields.TENANT,
                Search.HITS_LIST_SIZE, 10_000
        );

        if (documentWithSiteSecret.getDocuments().isEmpty()) {
            return Optional.empty();
        } else {
            final List<String> documents = documentWithSiteSecret.getDocuments().stream()
                    .map(Document::getId).collect(Collectors.toList());
            return Optional.of(documents);
        }
    }

    private void initSite(UUID siteId, UUID siteSecret) {
        final var siteConfiguration = new Document(SITE_CONFIGURATION_DOCUMENT_PREFIX + siteId);
        siteConfiguration.set("secret", siteSecret);
        indexService.index(siteConfiguration);
    }

    private void storeSite(UUID siteId, UUID siteSecret, String email, final List<SiteProfile.Config> configs) {
        final var siteConfiguration = indexService.fetch(Index.ALL, SITE_CONFIGURATION_DOCUMENT_PREFIX + siteId).stream().findAny();
        final Document siteConfigDoc;
        siteConfigDoc = siteConfiguration.orElseGet(() -> new Document(SITE_CONFIGURATION_DOCUMENT_PREFIX + siteId));
        siteConfigDoc.set("secret", siteSecret);
        siteConfigDoc.set("email", email);
        siteConfigDoc.set("urls", configs.stream().map(config -> config.getUrl().toString()).collect(Collectors.toList()));
        configs.forEach(config -> siteConfigDoc.set(config.getUrl().toString(),
                Arrays.asList(config.getPageBodyCssSelector(), Boolean.toString(config.isSitemapsOnly()), Boolean.toString(config.isAllowUrlWithQuery()))));
        indexService.index(siteConfigDoc);
    }

    private void storeCrawlStatus(SitesCrawlStatus sitesCrawlStatus) {
        final Document crawlStatus = new Document(CRAWL_STATUS_SINGLETON_DOCUMENT);
        sitesCrawlStatus.getSites()
                .forEach(siteCrawlStatus ->
                        crawlStatus.set(siteCrawlStatus.getSiteId().toString(), Arrays.asList(siteCrawlStatus.getCrawled(), siteCrawlStatus.getPageCount())));

        indexService.index(crawlStatus);
    }

    public Optional<SitesCrawlStatus> updateCrawlStatusInShedule(UUID siteId, long pageCount) {
        final var fetchSitesCrawlStatus = fetchSitesCrawlStatus();
        fetchSitesCrawlStatus.ifPresent(sitesCrawlStatus -> {
            sitesCrawlStatus.getSites().forEach(crawlStatus -> {
                if (siteId.equals(crawlStatus.getSiteId())) {
                    crawlStatus.setCrawled(Instant.now().toString());
                    crawlStatus.setPageCount(pageCount);
                }
            });

            final Document updatedCrawlStatusDoc = new Document(CRAWL_STATUS_SINGLETON_DOCUMENT);
            sitesCrawlStatus.getSites()
                    .forEach(updatedCrawlStatus -> updatedCrawlStatusDoc.set(updatedCrawlStatus.getSiteId().toString(), Arrays.asList(updatedCrawlStatus.getCrawled(), updatedCrawlStatus.getPageCount())));
            indexService.index(updatedCrawlStatusDoc);
        });
        return fetchSitesCrawlStatus;
    }

    public Optional<FetchedPage> fetchById(String id) {
        final Optional<Document> found = indexService.fetch(Index.ALL, id).stream().findAny();

        if (found.isPresent()) {
            final Document foundDocument = found.get();
            final FetchedPage representationOfFoundDocument = new FetchedPage(
                    UUID.fromString(Objects.requireNonNull(foundDocument.get(Fields.TENANT))),
                    foundDocument.getId(),
                    foundDocument.get(Fields.TITLE),
                    foundDocument.get(Fields.BODY),
                    foundDocument.get(Fields.URL),
                    foundDocument.get(PAGE_TIMESTAMP),
                    foundDocument.getAll(PAGE_LABELS),
                    foundDocument.get(PAGE_THUMBNAIL)
            );
            return Optional.of(representationOfFoundDocument);
        } else {
            return Optional.empty();
        }
    }

    public Optional<SiteIndexSummary> indexFeed(URI feedUrl, UUID siteId, UUID siteSecret, Boolean stripHtmlTags, Boolean isGeneric, boolean clearIndex) {
        if (siteId != null && siteSecret != null) { // credentials are provided as a tuple only
            final Optional<UUID> fetchedSiteSecret = fetchSiteSecret(siteId);
            if (fetchedSiteSecret.isEmpty()) { // tenant does not exist
                return Optional.empty();
            } else if (siteSecret.equals(fetchedSiteSecret.get())) { // authorized
                if (clearIndex) {
                    final Optional<List<String>> allPages = fetchAllDocuments(siteId);
                    allPages.ifPresent(pages -> indexService.delete(pages.toArray(new String[]{})));
                }
                if (isGeneric) {
                    return updateIndexGenerically(feedUrl, siteId, siteSecret, stripHtmlTags);
                } else {
                    return updateIndex(feedUrl, siteId, siteSecret, stripHtmlTags);
                }
            } else { // unauthorized
                return Optional.empty();
            }
        } else if (siteId == null ^ siteSecret == null) { // it does not make any sense if only one of the parameters is set
            return Optional.empty();
        } else { // consider request as first-usage-ownership-granting request (early binding), create new index
            UUID newSiteId = UUID.randomUUID();
            UUID newSiteSecret = UUID.randomUUID();
            initSite(newSiteId, newSiteSecret);
            if (isGeneric) {
                return Optional.empty();
            } else {
                return updateIndex(feedUrl, newSiteId, newSiteSecret, stripHtmlTags);
            }
        }
    }

    public SiteCreation createSite() {
        final var siteId = UUID.randomUUID();
        final var siteSecret = UUID.randomUUID();
        initSite(siteId, siteSecret);
        return new SiteCreation(siteId, siteSecret);
    }

    public SiteCreation createSite(String email, List<SiteProfile.Config> configs) {
        final var siteId = UUID.randomUUID();
        final var siteSecret = UUID.randomUUID();
        storeSite(siteId, siteSecret, email, configs);
        return new SiteCreation(siteId, siteSecret);
    }

    static {
        new BaseConfig.TrustAllX509TrustManager();
    }

    private Optional<SiteIndexSummary> readXml(NodeList nodeList, AtomicInteger successfullyIndexed, List<String> documents, List<String> failedToIndex, UUID siteId) {
        String title = null;
        String body = null;
        String url = null;
        SitePage toIndex;
        for (int count = 0; count < nodeList.getLength(); count++) {
            Node tempNode = nodeList.item(count);
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
                switch (tempNode.getNodeName()) {
                    case "title":
                        title = tempNode.getTextContent();
                        break;
                    case "body":
                        body = tempNode.getTextContent();
                        break;
                    case "url":
                        url = tempNode.getTextContent();
                        break;
                    default:
                        break;
                }

                if (tempNode.hasChildNodes()) {
                    toIndex = new SitePage(
                            title,
                            body,
                            url,
                            "");

                    if (toIndex.getTitle() != null && toIndex.getBody() != null && toIndex.getUrl() != null) {
                        final String pageId = SitePage.hashPageId(siteId, url);

                        Optional<FetchedPage> indexed = indexDocument(pageId, siteId, toIndex);
                        if (indexed.isPresent()) {
                            successfullyIndexed.incrementAndGet();
                            documents.add(pageId);
                        } else {
                            failedToIndex.add(url);
                            LOG.warn("index-failed:" + url);
                        }
                    }
                    readXml(tempNode.getChildNodes(), successfullyIndexed, documents, failedToIndex, siteId);
                }

                if ("Document".equals(tempNode.getNodeName())) {
                    title = null;
                    body = null;
                    url = null;
                    toIndex = null;
                }
            }
        }

        return Optional.of(new SiteIndexSummary(siteId, null, successfullyIndexed.get(), documents, failedToIndex));
    }

    private Optional<SiteIndexSummary> updateIndexGenerically(URI feedUrl, UUID siteId, UUID siteSecret, Boolean stripHtmlTags) {
        LOG.info("URL-received: " + feedUrl);
        final AtomicInteger successfullyIndexed = new AtomicInteger(0);
        final List<String> documents = new ArrayList<>();
        List<String> failedToIndex = new ArrayList<>();

        try {
            DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(feedUrl.toURL().openStream());

            if (doc.hasChildNodes()) {
                return readXml(doc.getChildNodes(), successfullyIndexed, documents, failedToIndex, siteId);
            }

            return Optional.empty();
        } catch (ParserConfigurationException | IOException | SAXException e) {
            LOG.warn(e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<SiteIndexSummary> updateIndex(URI feedUrl, UUID siteId, UUID siteSecret, Boolean stripHtmlTags) {
        LOG.info("URL-received: " + feedUrl);
        final AtomicInteger successfullyIndexed = new AtomicInteger();
        final List<String> documents = new ArrayList<>();
        List<String> failedToIndex = new ArrayList<>();
        try {
            final SyndFeed feed = new SyndFeedInput().build(new XmlReader(feedUrl.toURL()));

            feed.getEntries().forEach(entry -> {
                final String body;
                if (stripHtmlTags) {
                    body = entry.getDescription().getValue()
                            .replaceAll("<[^>]*>", "");
                } else {
                    body = entry.getDescription().getValue();
                }

                final String url = entry.getLink();
                final SitePage toIndex = new SitePage(
                        entry.getTitle(),
                        body,
                        url,
                        "");
                final String pageId = SitePage.hashPageId(siteId, url);
                Optional<FetchedPage> indexed = indexDocument(pageId, siteId, toIndex);
                if (indexed.isPresent()) {
                    successfullyIndexed.incrementAndGet();
                    documents.add(pageId);
                } else {
                    failedToIndex.add(entry.getLink());
                    LOG.warn("unsuccessfully-indexed:" + entry.getLink());
                }
            });

            return Optional.of(new SiteIndexSummary(siteId, siteSecret, successfullyIndexed.get(), documents, failedToIndex));
        } catch (FeedException | IOException e) {
            LOG.warn(e.getMessage());
            return Optional.empty();
        }
    }


    public boolean clearSite(UUID siteId, UUID siteSecret) {
        if (isAllowedToModify(siteId, siteSecret)) {
            final Optional<List<String>> pages = fetchAllDocuments(siteId);
            if (pages.isPresent()) {
                String[] documents = new String[pages.get().size()];
                documents = pages.get().toArray(documents);
                indexService.delete(documents);
                return true;
            }
        }
        return false;
    }

    public boolean delete(UUID siteId, UUID siteSecret, String pageId) {
        if (isAllowedToModify(siteId, siteSecret)) {
            // just assume everything works... right?
            indexService.delete(pageId);
            return true;
        } else {
            return false;
        }
    }

    private void deleteWithoutFurtherChecks(final String... pageIds) {
        indexService.delete(pageIds);
    }

    public boolean clearIndex(UUID siteId, UUID siteSecret) {
        try {
            final Request request = new Request.Builder()
                    .url("https://api." + Application.SIS_DOMAIN + "/sites/" + siteId + "?siteSecret=" + siteSecret)
                    .delete()
                    .build();
            final Response response = SiteCrawler.HTTP_CLIENT.newCall(request).execute();
            if (response.code() == 204 || response.code() == 200) {
                return true;
            } else {
                LOG.error("CLEAR_INDEX_RESULT: " + response.code());
                return false;
            }
        } catch (IOException e) {
            LOG.error("CLEAR_INDEX_RESULT_FAILURE: " + e.getMessage());
            return false;
        }
    }

    public Optional<SitesCrawlStatus> storeCrawlStatus(UUID serviceSecret, SitesCrawlStatus sitesCrawlStatus) {
        if (ADMIN_SITE_SECRET.equals(serviceSecret)) {
            storeCrawlStatus(sitesCrawlStatus);
            return Optional.of(sitesCrawlStatus);
        } else {
            return Optional.empty();
        }
    }

    public Optional<SitesCrawlStatus> fetchCrawlStatus(UUID serviceSecret) {
        if (ADMIN_SITE_SECRET.equals(serviceSecret)) {
            return fetchSitesCrawlStatus();
        }
        return Optional.empty();
    }

    private Optional<SitesCrawlStatus> fetchSitesCrawlStatus() {
        final Set<CrawlStatus> sitesCrawlStatus = new HashSet<>();
        final Optional<Document> crawlStatus = indexService.fetch(Index.ALL, CRAWL_STATUS_SINGLETON_DOCUMENT).stream().findAny();
        crawlStatus.ifPresent(document -> document.getFields().forEach((uuidKey, crawledTimestamp) -> {
            if (!uuidKey.startsWith("_")) {
                long pageCount;
                if (crawledTimestamp.size() == 2) {
                    pageCount = crawledTimestamp.get(1) == null ? -1 : Long.parseLong(crawledTimestamp.get(1));
                } else {
                    pageCount = -1;
                }
                sitesCrawlStatus.add(new CrawlStatus(UUID.fromString(uuidKey), Instant.parse(crawledTimestamp.get(0)), pageCount));
            }
        }));
        return Optional.of(new SitesCrawlStatus(sitesCrawlStatus));
    }

    public Optional<SiteProfile> updateSiteProfile(UUID siteId, UUID siteSecret, SiteProfileUpdate siteProfileUpdate) {
        final Optional<UUID> fetchedSiteSecret = fetchSiteSecret(siteId);
        if (fetchedSiteSecret.isPresent()) {
            if (fetchedSiteSecret.get().equals(siteSecret)) {
                storeSite(siteId, siteProfileUpdate.getSecret(), siteProfileUpdate.getEmail(), siteProfileUpdate.getConfigs());
                return fetchSiteProfile(siteId);
            }
        }
        return Optional.empty();
    }

    public Optional<IndexCleanupResult> removeOldSiteIndexPages(final UUID siteId) {
        final var obsoletePageThreshold = Instant.now().minus(4, ChronoUnit.HALF_DAYS);
        final Hits documents = searchService.search(
                Fields.TENANT + ":" + siteId.toString(),
                Search.RETURN_FIELDS, Fields.TENANT + SearchService.QUERY_SEPARATOR + Fields.URL + SearchService.QUERY_SEPARATOR + PAGE_TIMESTAMP,
                Search.HITS_LIST_SIZE, 10_000
        );

        if (documents.getDocuments().isEmpty()) {
            return Optional.empty();
        } else {
            final Map<String, String> pagesToDelete = documents.getDocuments().stream()
                    .filter(document -> Instant.parse(Objects.requireNonNull(document.get(PAGE_TIMESTAMP))).isBefore(obsoletePageThreshold)) // TODO pre-filter it in the service call to search-service
                    .collect(Collectors.toMap(Document::getId, value -> value.get(Fields.URL)));
            final var numberOfPagesToDelete = pagesToDelete.entrySet().size();
            deleteWithoutFurtherChecks(pagesToDelete.keySet().toArray(new String[0])); // TODO finish THIS!!!!!!
            return Optional.of(new IndexCleanupResult(numberOfPagesToDelete, new ArrayList<>(pagesToDelete.values())));
        }
    }
}