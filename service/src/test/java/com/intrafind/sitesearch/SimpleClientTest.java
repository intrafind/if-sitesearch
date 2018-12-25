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

import com.intrafind.sitesearch.controller.SiteController;
import com.intrafind.sitesearch.dto.FetchedPage;
import com.intrafind.sitesearch.integration.SiteTest;
import com.intrafind.sitesearch.service.SimpleIndexClient;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = "oss")
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

    private static final UUID SITE_ID = UUID.fromString("b7fde685-33f4-4a79-9ac3-ee3b75b83fa3");
    private static final UUID SITE_SECRET = UUID.fromString("56158b15-0d87-49bf-837d-89085a4ec88d");
    private static final String PAGE_ID = "b020c11f8b9827d09ae028a9745201d8a533f1493f16b1a8c7331e9bc4d988a6";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();


    private void updatePage() {
//    http://www.baeldung.com/spring-5-webclient
//        WebClient client1 = WebClient.create();
//        WebClient client2 = WebClient.create("https://sitesearch:" + Application.SERVICE_SECRET + "@logs.sitesearch.cloud");

        final var response = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/pages?siteSecret=" + SITE_SECRET,
                HttpMethod.PUT, new HttpEntity<>(SiteTest.buildPage()), FetchedPage.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PAGE_ID, response.getBody().getId());
        assertEquals(SITE_ID, response.getBody().getSiteId());
        assertEquals(SiteTest.buildPage().getUrl(), response.getBody().getUrl());
        assertEquals(SiteTest.buildPage().getSisLabels(), response.getBody().getSisLabels());
        assertEquals(SiteTest.buildPage().getTitle(), response.getBody().getTitle());
        assertEquals("", response.getBody().getThumbnail());
    }

    @Test
    public void crudPage() throws Exception {
        updatePage();
        fetchPage();
        deletePage();
    }

    @Test
    public void crudSiteProfile() throws Exception {
        updateSiteProfile();
//        fetchPage();
//        deletePage();
    }

    private void updateSiteProfile() throws Exception {
        final var response = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/profile?siteSecret=" + SITE_SECRET,
                HttpMethod.PUT, new HttpEntity<>(SimpleIndexClient.MAPPER.readValue("'{\n" +
                        "    \"id\": [\"b7fde685-33f4-4a79-9ac3-ee3b75b83fa3\"],\n" +
                        "    \"secret\": [\"56158b15-0d87-49bf-837d-89085a4ec88d\"],\n" +
                        "    \"email\": [\"user@example.com\"]\n" +
                        "    }'", Object.class)), Object.class);
//                HttpMethod.PUT, new HttpEntity<>(SiteTest.SITE_PROFILE_CREATION), SiteProfile.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
//        assertEquals(PAGE_ID, response.getBody().getId());
//        assertEquals(SITE_ID, response.getBody().getSiteId());
//        assertEquals(SiteTest.buildPage().getUrl(), response.getBody().getUrl());
//        assertEquals(SiteTest.buildPage().getSisLabels(), response.getBody().getSisLabels());
//        assertEquals(SiteTest.buildPage().getTitle(), response.getBody().getTitle());
//        assertEquals("", response.getBody().getThumbnail());
    }

    private void fetchPage() {
        final var response = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/pages?url=" + SiteTest.buildPage().getUrl(),
                HttpMethod.GET, HttpEntity.EMPTY, FetchedPage.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PAGE_ID, response.getBody().getId());
        assertEquals(SITE_ID, response.getBody().getSiteId());
        assertEquals(SiteTest.buildPage().getUrl(), response.getBody().getUrl());
        assertEquals(SiteTest.buildPage().getSisLabels(), response.getBody().getSisLabels());
        assertEquals(SiteTest.buildPage().getTitle(), response.getBody().getTitle());
        assertEquals("", response.getBody().getThumbnail());
    }

    private void deletePage() {
        final var deletedPage = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/pages?url=" + SiteTest.buildPage().getUrl() + "&siteSecret=" + SITE_SECRET,
                HttpMethod.DELETE, HttpEntity.EMPTY, Object.class);
        assertEquals(HttpStatus.NO_CONTENT, deletedPage.getStatusCode());
        assertNull(deletedPage.getBody());

        final var fetchDeleted = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/pages?url=" + SiteTest.buildPage().getUrl(),
                HttpMethod.GET, HttpEntity.EMPTY, FetchedPage.class);
        assertEquals(HttpStatus.NOT_FOUND, fetchDeleted.getStatusCode());
        assertNull(fetchDeleted.getBody());
    }
}
