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

import com.intrafind.sitesearch.controller.AutocompleteController;
import com.intrafind.sitesearch.dto.Autocomplete;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import static com.intrafind.api.Document.IS_OSS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AutocompleteTest {
    private static final Logger LOG = LoggerFactory.getLogger(AutocompleteTest.class);
    @Autowired
    private TestRestTemplate caller;

    @Test
    public void referenceDeprecated() {
        final var actual = caller.getForEntity(AutocompleteController.ENDPOINT + "?query=Knowledge&siteId=" + SearchTest.SEARCH_SITE_ID, Autocomplete.class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertNotNull(actual.getBody());
        assertTrue(1 <= actual.getBody().getResults().size());
        if (!IS_OSS) // TODO this is NOT an OSS_WORKAROUND, CHECK if the content is not empty!
            assertEquals("knowledge graph", actual.getBody().getResults().get(0).toLowerCase());
    }

    @Test
    public void reference() {
        final var actual = caller.getForEntity("/sites/" + SearchTest.SEARCH_SITE_ID + "/autocomplete?query=Knowledge", Autocomplete.class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertNotNull(actual.getBody());
        assertTrue(1 <= actual.getBody().getResults().size());
        if (!IS_OSS) // TODO this is NOT an OSS_WORKAROUND, CHECK if the content is not empty!
            assertEquals("knowledge graph", actual.getBody().getResults().get(0).toLowerCase());
    }

    @Test
    public void complexPositiveDeprecated() {
        final var actual = caller.getForEntity(AutocompleteController.ENDPOINT + "?query=ifinder&siteId=" + SearchTest.SEARCH_SITE_ID, Autocomplete.class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertNotNull(actual.getBody());
        assertTrue(1 <= actual.getBody().getResults().size());
        actual.getBody().getResults().forEach(term -> {
            assertTrue(term.toLowerCase().contains("ifinder"));
        });
    }

    @Test
    public void complexPositive() {
        final var actual = caller.getForEntity("/sites/" + SearchTest.SEARCH_SITE_ID + "/autocomplete?query=ifinder", Autocomplete.class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertNotNull(actual.getBody());
        assertTrue(1 <= actual.getBody().getResults().size());
        actual.getBody().getResults().forEach(term ->
                assertTrue(term.toLowerCase().contains("ifinder"))
        );
    }

    @Test
    public void nonExistingDeprecated() {
        final var actual = caller.getForEntity(AutocompleteController.ENDPOINT + "?query=not_found&siteId=" + SearchTest.SEARCH_SITE_ID, Autocomplete.class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertNotNull(actual.getBody());
        assertTrue(actual.getBody().getResults().isEmpty());
    }

    @Test
    public void nonExisting() {
        final var actual = caller.getForEntity("/sites/" + SearchTest.SEARCH_SITE_ID + "/autocomplete?query=not_found", Autocomplete.class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertNotNull(actual.getBody());
        assertTrue(actual.getBody().getResults().isEmpty());
    }

    @Test
    public void withoutSiteIdDeprecated() {
        final var actual = caller.getForEntity(AutocompleteController.ENDPOINT + "?query=not_found", Autocomplete.class);

        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertNotNull(actual.getBody());
    }

    @Test
    public void withoutSiteId() {
        final var actual = caller.getForEntity("/sites/" + "/autocomplete?query=not_found", Autocomplete.class);
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());

        final var response = caller.getForEntity("/sites" + "?query=not_found", Autocomplete.class);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(actual.getBody());
    }

    @Test
    public void withoutQuery() {
        final var actual = caller.getForEntity("/sites/" + SearchTest.SEARCH_SITE_ID + "/autocomplete?query=", Autocomplete.class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(0, actual.getBody().getResults().size());
    }

    @Test
    public void withInvalidSiteId() {
        final var actual = caller.getForEntity("/sites/invalid-siteId/autocomplete?query=not_found", Autocomplete.class);

        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertNotNull(actual.getBody());
    }
}


