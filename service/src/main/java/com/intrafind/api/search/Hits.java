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

package com.intrafind.api.search;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intrafind.api.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Hits implements Serializable {
    @JsonAlias(value = {"documents", "hits"})
    private final List<Document> documents = new ArrayList<>();

    private static final String KEY_TOTAL_HITS = "totalHits";

    private final Document metaData = new Document("meta");

    private Hits() {
    }

    public Hits(final long totalHits) {
        this.getMetaData().set(KEY_TOTAL_HITS, totalHits);
    }

    public List<Document> getDocuments() {
        return this.documents;
    }

    public Document getMetaData() {
        return this.metaData;
    }

    public long getTotalHits() {
        return Long.parseLong(Objects.requireNonNull(this.metaData.get(KEY_TOTAL_HITS)));
    }

    @Override
    public String toString() {
        return this.metaData + "/" + this.documents.toString();
    }
}