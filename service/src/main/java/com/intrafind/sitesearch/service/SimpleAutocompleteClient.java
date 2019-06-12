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
import org.springframework.stereotype.Repository;

/**
 * Should serve as a persistence client that works on a different index than the search client.
 */
@Profile("oss")
@Primary
@Repository
public class SimpleAutocompleteClient implements AutocompleteClient {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleAutocompleteClient.class);

    private final Search search;

    public SimpleAutocompleteClient(Search search) {
        this.search = search;
    }

    @Override
    public Hits search(String searchQuery, Object... parameters) {
//        https://www.elastic.co/guide/en/elasticsearch/reference/7.x/search-as-you-type.html
        final var hits = search.search(searchQuery, parameters);
        hits.getDocuments().forEach(document -> {
            hits.getMetaData().add("autocomplete.terms", document.get("_str.url"));
        });
        return hits;
    }
}
