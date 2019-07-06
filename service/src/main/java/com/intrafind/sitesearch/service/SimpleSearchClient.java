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

import com.intrafind.api.search.Hits;
import com.intrafind.api.search.Search;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import static com.intrafind.sitesearch.service.SimpleIndexClient.BASIC_AUTH_HEADER;
import static com.intrafind.sitesearch.service.SimpleIndexClient.CLIENT;
import static com.intrafind.sitesearch.service.SimpleIndexClient.ELASTICSEARCH_SERVICE;
import static com.intrafind.sitesearch.service.SimpleIndexClient.MAPPER;

/**
 * Should serve as a persistence client that works on a different index than the search client.
 */
@Profile("oss")
@Primary
@Repository
public class SimpleSearchClient implements Search {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleSearchClient.class);

    @Override
    public Hits search(String searchQuery, Object... parameters) {
        final int pageSize;
        if (parameters[3].toString().equals("10000")) {
            pageSize = 10_000;
        } else if (parameters[5].toString().equals("10")) {
            pageSize = 10;
        } else {
            pageSize = 50;
        }

        final UUID siteId;
        if (searchQuery.startsWith("_raw.tenant")) {
            siteId = UUID.fromString(searchQuery.substring(12));
        } else if (parameters[1].toString().startsWith("_raw.tenant")) {
            siteId = UUID.fromString(parameters[1].toString().substring(12));
        } else {
            siteId = UUID.fromString(parameters[1].toString().substring(12));
        }

        try {
            final var call = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_SERVICE + "/site-page/_search"))
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(buildSearchQuery(searchQuery, siteId, pageSize)))
                    .build();

            final var response = CLIENT.send(call, HttpResponse.BodyHandlers.ofString());
            LOG.debug("searchQuery: {} - status: {} - body: {}", searchQuery, response.statusCode(), response.body());
            final Hits hits = MAPPER.readValue(MAPPER.writeValueAsString(MAPPER.readValue(response.body(), Map.class).get("hits")), Hits.class);
            toHighlighted(hits);
            return hits;
        } catch (IOException | InterruptedException e) {
            LOG.warn("searchQuery: {} - exception: {}", searchQuery, e.getMessage());
        }

        return null;
    }

    private void toHighlighted(Hits hits) {
        hits.getDocuments().forEach(document -> {
            document.set("hit.teaser._str.title", document.getHighlight().get("_str.title") == null ? document.getFields().get("_str.title") : document.getHighlight().get("_str.title"));
            document.set("hit.teaser._str.body", document.getHighlight().get("_str.body") == null ? document.getFields().get("_str.body") : document.getHighlight().get("_str.body"));
            document.set("hit.teaser._str.url", document.getHighlight().get("_str.url") == null ? document.getFields().get("_str.url") : document.getHighlight().get("_str.url"));
        });
    }

    private String buildSearchQuery(final String searchQuery, final UUID siteId, final int pageSize) {
        return "{\"query\":{\"bool\":{\"must\":{\"query_string\": {" +
                "\"fields\": [\"_str.body\",\"_str.title\", \"_str.url\"]," +
                "\"query\": \"" + searchQuery + "\"}}," +
                "\"filter\":{\"match\":{\"_raw.tenant\":\"" + siteId + "\"}}}}," +    // TODO check if siteId/TENANT is considered
                "\"highlight\":{" +
                "    \"pre_tags\":[\"<span class=\\\"if-teaser-highlight\\\">\"]," +
                "    \"post_tags\":[\"</span>\"]," +
                "    \"number_of_fragments\":1," +
                "    \"fragment_size\":150," +
                "    \"fields\":{" +
                "        \"_str.body\":{}," +
                "        \"_str.title\":{}," +
                "        \"_str.url\":{}" +
                "    }}," +
                "\"size\":" + pageSize + "}";
    }
}
