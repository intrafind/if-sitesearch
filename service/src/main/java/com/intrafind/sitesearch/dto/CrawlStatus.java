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

package com.intrafind.sitesearch.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CrawlStatus {
    private UUID siteId;
    private Instant crawled;
    private long pageCount;
    private SiteProfile siteProfile;

    private CrawlStatus() {
    }

    public CrawlStatus(UUID siteId, Instant crawled, long pageCount, SiteProfile siteProfile) {
        this(siteId, crawled, pageCount);
        this.siteProfile = siteProfile;
    }

    public CrawlStatus(UUID siteId, Instant crawled, long pageCount) {
        this.siteId = siteId;
        this.crawled = crawled;
        this.pageCount = pageCount;
    }

    public SiteProfile getSiteProfile() {
        return siteProfile;
    }

    public UUID getSiteId() {
        return siteId;
    }

    public long getPageCount() {
        return pageCount;
    }

    public String getCrawled() {
        return crawled.toString();
    }

    public void setCrawled(String crawled) {
        this.crawled = Instant.parse(crawled);
    }

    public void setPageCount(long pageCount) {
        this.pageCount = pageCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrawlStatus that = (CrawlStatus) o;
        return Objects.equals(siteId, that.siteId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(siteId);
    }
}
