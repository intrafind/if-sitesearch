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

import com.intrafind.api.Document;
import com.intrafind.api.index.Index;
import com.intrafind.sitesearch.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Should serve as a persistence client that works on a different index than the search client.
 */
@Profile("oss")
@Primary
@Repository
public class SimpleIndexClient implements Index {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleIndexClient.class);
    private static final String ELASTICSEARCH_SERVICE = "https://elasticsearch.sitesearch.cloud";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final byte[] credentials = ("sitesearch:" + Application.SERVICE_SECRET).getBytes();
    private static final String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials);

    @Override
    public void index(Document... documents) {
        LOG.warn("SimpleIndexService#index");
        IFIndexService.INDEX_SERVICE.index(documents);
    }

    @Override
    public List<Document> fetch(String[] options, String... documents) {
        LOG.warn("SimpleIndexService#fetch");
        return IFIndexService.INDEX_SERVICE.fetch(options, documents);
    }

    @Override
    public void delete(String... documents) {
        LOG.warn("SimpleIndexService#delete: " + documents + " | " + documents.toString() + " | " + Arrays.toString(documents) + " | \n" + "{\"query\": {\"terms\": {\"_id\": " + Arrays.toString(documents) + "}}}");

        final var call = HttpRequest.newBuilder()
                .uri(URI.create(ELASTICSEARCH_SERVICE + "/site-profile/_delete_by_query"))
                .version(HttpClient.Version.HTTP_2)
                .header(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\": {\"terms\": {\"_id\": [\"" + documents[0] + "\"]}}}"))
                .build();
        try {
            final HttpResponse<String> httpResponse = httpClient.send(call, HttpResponse.BodyHandlers.ofString());
            LOG.debug("documents: {} - status: {} - body: {}", documents, httpResponse.statusCode(), httpResponse.body());
        } catch (IOException | InterruptedException e) {
            LOG.warn("documents: {} - exception: {}", e.getMessage());
        }

//        IFIndexService.INDEX_SERVICE.delete(documents);
    }
}
