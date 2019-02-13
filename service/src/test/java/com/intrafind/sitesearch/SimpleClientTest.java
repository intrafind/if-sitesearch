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

package com.intrafind.sitesearch;

import com.intrafind.api.Document;
import com.intrafind.sitesearch.controller.SiteController;
import com.intrafind.sitesearch.dto.FetchedPage;
import com.intrafind.sitesearch.dto.FoundPage;
import com.intrafind.sitesearch.dto.Hits;
import com.intrafind.sitesearch.dto.SiteProfile;
import com.intrafind.sitesearch.integration.SiteTest;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@FixMethodOrder(MethodSorters.JVM)
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("oss")
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
//        WebClient client2 = WebClient.create("https://sitesearch:" + Application.SERVICE_SECRET + "@logs."+Application.SIS_DOMAIN);

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
    public void crudSiteProfile() throws Exception {
        updateSiteProfile();
        fetchSiteProfile();
    }

    @Test
    public void crudPage() throws Exception {
        updatePage();
//        suggest();
        TimeUnit.MILLISECONDS.sleep(1_000);
        search();
        fetchPage();
        TimeUnit.MILLISECONDS.sleep(1_000);
        deletePage();
    }

    private static final String EMAIL = "user@examaple.com";

    private void search() {
        final var searchQuery = ",.solution,.";
        final var search = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/search?query=" + searchQuery,
                HttpMethod.GET, HttpEntity.EMPTY, Hits.class);

        assertEquals(HttpStatus.OK, search.getStatusCode());
        assertNotNull(search.getBody());
        assertEquals(searchQuery, search.getBody().getQuery());
        assertEquals(1, search.getBody().getResults().size());
        final FoundPage foundPage = search.getBody().getResults().get(0);
        assertEquals(SiteTest.buildPage().getSisLabels(), Arrays.asList("mars", "Venus"));
        if (Document.IS_OSS) { // works once oss switch is enabled via SPRING_PROFILES_ACTIVE=oss
            assertEquals(SiteTest.buildPage().getTitle(), foundPage.getTitle());
            assertEquals(SiteTest.buildPage().getBody(), foundPage.getBody());
            assertEquals(SiteTest.buildPage().getUrl(), foundPage.getUrl());
        }
    }

    private void updateSiteProfile() {
        final var siteProfile = new SiteProfile(SITE_ID, SITE_SECRET, EMAIL, Collections.emptyList());
        final var createdProfile = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/profile?siteSecret=" + SITE_SECRET,
                HttpMethod.PUT, new HttpEntity<>(siteProfile), SiteProfile.class);

        assertEquals(HttpStatus.OK, createdProfile.getStatusCode());
        assertNotNull(createdProfile.getBody());
        assertEquals(EMAIL, createdProfile.getBody().getEmail());
        assertEquals(SITE_ID, createdProfile.getBody().getId());
        assertEquals(SITE_SECRET, createdProfile.getBody().getSecret());
        assertEquals(Collections.emptyList(), createdProfile.getBody().getConfigs());
    }

    private void fetchSiteProfile() {
        final var createdProfile = caller.exchange(SiteController.ENDPOINT + "/" + SITE_ID + "/profile?siteSecret=" + SITE_SECRET,
                HttpMethod.GET, HttpEntity.EMPTY, SiteProfile.class);

        assertEquals(HttpStatus.OK, createdProfile.getStatusCode());
        assertNotNull(createdProfile.getBody());
        assertEquals(EMAIL, createdProfile.getBody().getEmail());
        assertEquals(SITE_ID, createdProfile.getBody().getId());
        assertEquals(SITE_SECRET, createdProfile.getBody().getSecret());
        assertEquals(Collections.emptyList(), createdProfile.getBody().getConfigs());
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
