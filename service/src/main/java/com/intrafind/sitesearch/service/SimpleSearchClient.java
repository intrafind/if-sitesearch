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

package com.intrafind.sitesearch.service;

import com.intrafind.api.search.Hits;
import com.intrafind.api.search.Search;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
        LOG.warn("SimpleSearchService");
        try {
            final var call = HttpRequest.newBuilder()
                    .uri(URI.create(ELASTICSEARCH_SERVICE + "/site-page/_search"))
                    .header(HttpHeaders.AUTHORIZATION, BASIC_AUTH_HEADER)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString("{" +
                            "    \"query\": {" +
                            "        \"bool\": {" +
                            "            \"must\": {" +
                            "                \"query_string\": {" +
                            "                    \"query\": \"" + searchQuery + "\"" +
                            "                }" +
                            "            }" +
                            "        }" +
                            "    }" +
                            "}"))
                    .build();

            final var response = CLIENT.send(call, HttpResponse.BodyHandlers.ofString());
            LOG.debug("searchQuery: {} - status: {} - body: {}", searchQuery, response.statusCode(), response.body());
            if (HttpStatus.OK.value() != response.statusCode())
                return null;
            final var hits = MAPPER.readValue(response.body(), Hits.class);
            return hits;
        } catch (IOException | InterruptedException e) {
            LOG.warn("documents: {} - exception: {}", e.getMessage());
        }

        return null;
//        return IFSearchService.SEARCH_SERVICE_CLIENT.search(searchQuery, parameters);
    }
}
