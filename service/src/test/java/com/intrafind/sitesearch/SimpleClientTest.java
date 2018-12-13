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

package com.intrafind.sitesearch;

import com.intrafind.sitesearch.service.SimpleAutocompleteClient;
import com.intrafind.sitesearch.service.SimpleIndexClient;
import com.intrafind.sitesearch.service.SimpleSearchClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimpleClientTest {
    private final static Logger LOG = LoggerFactory.getLogger(SimpleClientTest.class);
    @Autowired
    private TestRestTemplate caller;

    //    http://www.baeldung.com/spring-5-webclient
    private WebTestClient webTestClient = WebTestClient
            .bindToServer()
            .baseUrl("http://sitesearch:" + Application.SERVICE_SECRET + "@localhost:8080")
            .build();

    private WebClient webClient = WebClient
            .builder()
            .baseUrl("http://sitesearch:" + Application.SERVICE_SECRET + "@localhost:8080")
            .build();

    @Test
    public void test() throws Exception {
        final var simpleIndex = new SimpleIndexClient();
        final var simpleAutocomplete = new SimpleAutocompleteClient();
        final var simpleSearch = new SimpleSearchClient();

        //    http://www.baeldung.com/spring-5-webclient
        WebClient client1 = WebClient.create();
        WebClient client2 = WebClient.create("https://sitesearch:" + Application.SERVICE_SECRET + "@logs.sitesearch.cloud");

//        --add-modules java.net.http
        final HttpClient httpClient = HttpClient.newHttpClient();
        final byte[] credentials = ("sitesearch:" + Application.SERVICE_SECRET).getBytes();
        final String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString(credentials);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("https://logs.sitesearch.cloud"))
                .header("authorization", basicAuthHeader)
                .GET()
                .build();

        final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.OK.value(), httpResponse.statusCode());
    }
}
