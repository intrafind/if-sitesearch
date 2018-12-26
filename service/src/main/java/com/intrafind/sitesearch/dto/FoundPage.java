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

package com.intrafind.sitesearch.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FoundPage {
    private String thumbnail = "";
    private String title;
    private String body;
    private String url;
    private String urlRaw;
    private List<String> sisLabels = new ArrayList<>();

    private FoundPage() {
    }

    public FoundPage(String title, String body, String url, String urlRaw, List<String> sisLabels, String thumbnail) {
        this.title = title;
        this.body = body;
        this.url = url;
        this.urlRaw = urlRaw;
        this.sisLabels = sisLabels == null ? Collections.emptyList() : sisLabels;
        this.thumbnail = thumbnail == null ? "" : thumbnail;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public List<String> getSisLabels() {
        return sisLabels;
    }

    public String getUrlRaw() {
        return urlRaw;
    }

    public void setUrlRaw(String urlRaw) {
        this.urlRaw = urlRaw;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FoundPage site = (FoundPage) o;
        return Objects.equals(title, site.title) &&
                Objects.equals(body, site.body) &&
                Objects.equals(url, site.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, body, url);
    }
}
