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

package com.intrafind.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intrafind.sitesearch.dto.FoundPage;
import com.intrafind.sitesearch.service.SiteService;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class Document implements Serializable {
    @JsonAlias(value = {"id", "_id"})
    private final String id;
    @JsonAlias(value = {"fields", "_source"})
    private final Map<String, List<String>> fields = new TreeMap<>();

    protected Document() {
        this.id = null;
    }

    public Document(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Document ID cannot be null.");
        } else {
            this.id = id;
        }
    }

    public String getId() {
        return this.id;
    }

    private final Map<String, List<String>> highlight = new TreeMap<>();

    public Map<String, List<String>> getFields() {
        return this.fields;
    }

    public String get(final String key) {
        final var values = this.getAll(key);
        return values != null && !values.isEmpty() ? values.get(0) : "";
    }

    public List<String> getAll(final String key) {
        return this.getFields().get(key);
    }

    public Document set(final String key, final Object... values) {
        this.del(key);
        return this.add(key, values);
    }

    public Document set(final String key, final Iterable<?> values) {
        this.del(key);
        return this.add(key, values);
    }

    public Document add(final String key, final Object... values) {
        if (values != null) {
            for (final var value : values) {
                if (value != null) {
                    final var trimmedValue = value.toString().trim();
                    if (!trimmedValue.isEmpty()) {
                        final var field = this.getFields().computeIfAbsent(key, k -> new ArrayList<>());
                        field.add(trimmedValue);
                    }
                }
            }
        }

        return this;
    }

    public Document add(final String key, final Iterable<?> values) {
        if (values != null) {
            for (final var value : values) {
                this.add(key, value);
            }
        }

        return this;
    }

    public Document del(final String key) {
        this.getFields().remove(key);
        return this;
    }

    private static final String HIT_TEASER_PREFIX = "hit.teaser.";

    public Map<String, List<String>> getHighlight() {
        return highlight;
    }

    public FoundPage toFoundPage() {
        return new FoundPage(
                this.get(HIT_TEASER_PREFIX + Fields.TITLE),
                this.get(HIT_TEASER_PREFIX + Fields.BODY),
                this.get(HIT_TEASER_PREFIX + Fields.URL),
                this.get(Fields.URL),
                this.getAll(SiteService.PAGE_LABELS),
                this.get(SiteService.PAGE_THUMBNAIL)
        );
    }

    @Override
    public int hashCode() {
        return Objects.requireNonNull(this.getId()).hashCode();
    }

    @Override
    public String toString() {
        return this.getId() + ":" + this.getFields();
    }
}