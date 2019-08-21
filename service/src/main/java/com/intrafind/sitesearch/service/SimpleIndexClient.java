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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrafind.api.Document;
import com.intrafind.api.index.Index;
import com.intrafind.sitesearch.Application;
import com.intrafind.sitesearch.dto.CrawlStatus;
import com.intrafind.sitesearch.dto.SitesCrawlStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Should serve as a persistence client that works on a different index than the search client.
 */
@Profile("oss")
@Primary
@Repository
public class SimpleIndexClient implements Index {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleIndexClient.class);
    private static final byte[] credentials = ("sitesearch:" + Application.SERVICE_SECRET).getBytes();
    static final String ELASTICSEARCH_SERVICE = "https://es." + Application.OSS_SIS_DOMAIN;
    static final HttpClient CLIENT = HttpClient.newHttpClient();
    static final String BASIC_AUTH_HEADER = "Basic " + Base64.getEncoder().encodeToString(credentials);
    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SVC_SINGLETONS = "svc-singletons";

    @Override
    public void index(final Document... documents) {
        if (documents == null || documents.length > 1)
            throw new IllegalArgumentException(Arrays.toString(documents));

        final var docId = documents[0].getId();
        final var indexType = getIndexType(docId);

        if (indexType.equals(SVC_SINGLETONS)) {
            final var sitesCrawlStatus = new SitesCrawlStatus(new HashSet<>(Collections.emptyList()));
            documents[0].getFields().forEach((siteId, status) ->
                    sitesCrawlStatus.getSites().add(new CrawlStatus(UUID.fromString(siteId),
                            Instant.parse(status.get(0)), Long.parseLong(status.get(1)),
                            null
                    )));

            try {
                final var call = HttpRequest.newBuilder()
                        .uri(URI.create(ELASTICSEARCH_SERVICE + "/" + indexType + "/_doc/" + docId))
                        .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .PUT(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(sitesCrawlStatus)))
                        .build();
                final var response = CLIENT.send(call, HttpResponse.BodyHandlers.ofString());
                LOG.debug("documents: {} - status: {} - body: {}", documents, response.statusCode(), response.body());
            } catch (IOException | InterruptedException e) {
                LOG.warn("documents: {} - exception: {}", documents, e.getMessage());
            }
            return;
        }

        try {
            final var call = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_SERVICE + "/" + indexType + "/_doc/" + docId))
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .PUT(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(documents[0].getFields())))
                    .build();
            final var response = CLIENT.send(call, HttpResponse.BodyHandlers.ofString());
            LOG.debug("documents: {} - status: {} - body: {}", documents, response.statusCode(), response.body());
        } catch (IOException | InterruptedException e) {
            LOG.warn("documents: {} - exception: {}", documents, e.getMessage());
        }
    }

    @Override
    public List<Document> fetch(String[] options, String... documents) {
        if (documents == null || documents.length == 0) return Collections.emptyList();
        if (documents.length > 1)
            throw new IllegalArgumentException(Arrays.toString(documents));

        final var docId = documents[0];
        final var indexType = getIndexType(docId);

        if (indexType.equals(SVC_SINGLETONS)) {
            final var call = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_SERVICE + "/" + indexType + "/_doc/" + docId))
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .GET()
                    .build();
            try {
                final var response = CLIENT.send(call, HttpResponse.BodyHandlers.ofString());
                LOG.debug("documents: {} - status: {} - body: {}", documents, response.statusCode(), response.body());
                if (HttpStatus.OK.value() != response.statusCode())
                    return Collections.emptyList();
                final var sitesCrawlStatus = MAPPER.readValue(MAPPER.writeValueAsString(MAPPER.readValue(response.body(), Map.class).get("_source")), SitesCrawlStatus.class);
                final var crawledSites = new Document(SVC_SINGLETONS);
                sitesCrawlStatus.getSites().forEach(crawlStatus ->
                        crawledSites.add(crawlStatus.getSiteId().toString(), crawlStatus.getCrawled(), crawlStatus.getPageCount()));
                return Collections.singletonList(crawledSites);
            } catch (IOException | InterruptedException e) {
                LOG.warn("documents: {} - exception: {}", documents, e.getMessage());
            }
            return Collections.emptyList();
        }

        final var call = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_SERVICE + "/" + indexType + "/_doc/" + docId))
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();
        try {
            final var response = CLIENT.send(call, HttpResponse.BodyHandlers.ofString());
            LOG.debug("documents: {} - status: {} - body: {}", documents, response.statusCode(), response.body());
            if (HttpStatus.OK.value() != response.statusCode())
                return Collections.emptyList();
            final var doc = MAPPER.readValue(response.body(), Document.class);
            return Collections.singletonList(doc);
        } catch (IOException | InterruptedException e) {
            LOG.warn("documents: {} - exception: {}", documents, e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    public void delete(String... documents) {
        if (documents == null || documents.length == 0) return;

        final var docId = documents[0];
        final var indexType = getIndexType(docId);

        final var call = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_SERVICE + "/" + indexType + "/_delete_by_query?refresh=false"))
                .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\": {\"terms\": {\"_id\":" +
                        Arrays.toString(documents)
                                .replace(", ", "\",\"")
                                .replace("[", "[\"")
                                .replace("]", "\"]")
                        + "}}}"))
                .build();
        try {
            final var response = CLIENT.send(call, HttpResponse.BodyHandlers.ofString());
            LOG.debug("documents: {} - status: {} - body: {}", documents, response.statusCode(), response.body());
        } catch (IOException | InterruptedException e) {
            LOG.warn("documents: {} - exception: {}", documents, e.getMessage());
        }
    }

    private String getIndexType(String document) {
        final String indexType;
        if (document.startsWith(SiteService.SITE_CONFIGURATION_DOCUMENT_PREFIX)) {
            indexType = "site-profile";
        } else if (document.equals(SiteService.CRAWL_STATUS_SINGLETON_DOCUMENT)) {
            indexType = SVC_SINGLETONS;
        } else {
            indexType = "site-page";
        }
        return indexType;
    }
}
