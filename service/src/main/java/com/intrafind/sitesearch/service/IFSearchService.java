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
import org.springframework.stereotype.Repository;

@Repository
public class IFSearchService implements Search {
    private static final Search SEARCH_SERVICE_CLIENT = IfinderCoreClient.newHessianClient(Search.class, IfinderCoreClient.IFINDER_CORE + "/search");

    @Override
    public Hits search(String searchQuery, Object... parameters) {
        return SEARCH_SERVICE_CLIENT.search(searchQuery, parameters);
    }
}

