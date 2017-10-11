/*
 * Copyright 2017 IntraFind Software AG. All rights reserved.
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
import com.intrafind.sitesearch.Application;
import com.intrafind.sitesearch.dto.FoundPage;
import com.intrafind.sitesearch.dto.Hits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SearchService {
    static final Search SEARCH_SERVICE = IfinderCoreClient.newHessianClient(Search.class, Application.IFINDER_CORE + "/search");

    private static final Logger LOG = LoggerFactory.getLogger(SearchService.class);
    static final String QUERY_SEPARATOR = ",";
    private static final String HIT_TEASER_PREFIX = "hit.teaser.";

    public Hits search(String query, UUID siteId) {
        com.intrafind.api.search.Hits hits = SEARCH_SERVICE.search(
//                query + " AND " + Fields.TENANT + ":" + siteId,
                query,
                Search.FILTER_QUERY, Fields.TENANT + ":" + siteId,
// TODO introduce filter on tenant
//                Search.RETURN_FIELDS, Fields.BODY + QUERY_SEPARATOR + Fields.TITLE + QUERY_SEPARATOR + Fields.URL + QUERY_SEPARATOR + Fields.TENANT,

                Search.RETURN_TEASER_FIELDS, Fields.BODY + QUERY_SEPARATOR + Fields.TITLE + QUERY_SEPARATOR + Fields.URL,
                Search.RETURN_TEASER_COUNT, 1,
                Search.RETURN_TEASER_SIZE, 100,
                Search.RETURN_TEASER_TAG_PRE, "<span class=\"if-teaser-highlight\">",
                Search.RETURN_TEASER_TAG_POST, "</span>",

//                Search.HITS_LIST_SIZE, 1_000 // page size
                Search.HITS_LIST_SIZE, 100 // page size
        );

        LOG.info("query: " + query);
        List<FoundPage> siteDocuments = new ArrayList<>();
        hits.getDocuments().forEach(document -> {
            LOG.warn("MARKUP IS HTML-ESCAPED: " + document.get(HIT_TEASER_PREFIX + Fields.BODY));
            LOG.warn("MARKUP IS UNTOUCHED, AS DESIRED: " + document.get(Fields.BODY));
            FoundPage site = new FoundPage(
                    document.get(HIT_TEASER_PREFIX + Fields.TITLE),
//                    document.get(Fields.TITLE),
                    document.get(HIT_TEASER_PREFIX + Fields.BODY),
//                    document.get(Fields.BODY),
                    document.get(HIT_TEASER_PREFIX + Fields.URL)
//                    document.get(Fields.URL)
            );
            // TODO remove tenant INFO as it is not relevant here, consider separate DTO
            siteDocuments.add(site);
        });

        return new Hits(query, siteDocuments);
    }
}