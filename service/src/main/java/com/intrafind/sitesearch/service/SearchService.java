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

import com.intrafind.api.Fields;
import com.intrafind.api.search.Search;
import com.intrafind.sitesearch.dto.FoundPage;
import com.intrafind.sitesearch.dto.Hits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.UUID;

@Service
public class SearchService {
    static final String QUERY_SEPARATOR = ",";

    private final Search searchService;

    @Autowired
    public SearchService(final Search searchService) {
        this.searchService = searchService;
    }

    public Hits search(final String query, final UUID siteId) {
        final var hits = searchService.search(
                query, Search.FILTER_QUERY, Fields.TENANT + ":" + siteId,
//  TODO optimize by not retrieving all fields              Search.RETURN_FIELDS, Fields.BODY + QUERY_SEPARATOR + Fields.TITLE + QUERY_SEPARATOR + Fields.URL + QUERY_SEPARATOR + Fields.TENANT + QUERY_SEPARATOR + QUERY_SEPARATOR +SiteService.PAGE_THUMBNAIL,

                Search.RETURN_TEASER_FIELDS, Fields.BODY + QUERY_SEPARATOR + Fields.TITLE + QUERY_SEPARATOR + Fields.URL,
                Search.RETURN_TEASER_COUNT, 1,
                Search.RETURN_TEASER_SIZE, 150,
                Search.RETURN_TEASER_TAG_PRE, "<span class=\"if-teaser-highlight\">",
                Search.RETURN_TEASER_TAG_POST, "</span>",

                Search.HITS_LIST_SIZE, 50 // max total results
        );

        final var siteDocuments = new ArrayList<FoundPage>();
        hits.getDocuments().forEach(document -> {
            siteDocuments.add(document.toFoundPage());
        });

        return new Hits(query, siteDocuments);
    }
}