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

package com.intrafind.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Document implements Serializable {
    private final String id;
    private final Map<String, List<String>> fields = new TreeMap<>();

    //    private Document() {
//        this.id = "";
//    }
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

    public Map<String, List<String>> getFields() {
        return this.fields;
    }

    public String get(final String key) {
        final var values = this.getAll(key);
        return values != null && !values.isEmpty() ? values.get(0) : null;
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

    @Override
    public int hashCode() {
        return this.getId().hashCode();
    }

    @Override
    public String toString() {
        return this.getId() + ":" + this.getFields();
    }
}