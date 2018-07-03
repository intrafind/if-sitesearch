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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrafind.sitesearch.dto.SitePage;
import crawlercommons.robots.BaseRobotRules;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class SiteCrawler extends WebCrawler {

    public static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");
    private final static Logger LOG = LoggerFactory.getLogger(SiteCrawler.class);
    private static final Pattern BLACKLIST = Pattern.compile(".*(\\.(css|js|gif|jpg|jpeg|png|mp3|mp4|zip|gz|xml|svg))$");
    //    private static final Pattern WHITELIST= Pattern.compile(".*(\\.(html|htm|txt|pdf))$");
    static final Map<UUID, AtomicInteger> PAGE_COUNT = new HashMap<>();
    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SIS_API_SERVICE_URL = System.getenv("SIS_API_SERVICE_URL");

    private UUID siteId;
    private UUID siteSecret;
    private URI url;
    private String pageBodyCssSelector;
    private boolean containsQuery;
    private BaseRobotRules robotRules;

    private SiteCrawler() {
    }

    public SiteCrawler(final UUID siteId, final UUID siteSecret, final URI url, final String pageBodyCssSelector, final BaseRobotRules robotRules) {
        this.siteId = siteId;
        this.siteSecret = siteSecret;
        this.url = url;
        this.pageBodyCssSelector = pageBodyCssSelector;
        this.containsQuery = siteId.equals(UUID.fromString("c7d080ff-6eec-496e-a70e-db5ec81948ab")); // mh

        this.robotRules = robotRules;
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL webUrl) {
        final String href = webUrl.getURL().toLowerCase();
        final boolean isCrawled = !BLACKLIST.matcher(href).matches()
                && href.startsWith(url.toString())
                && isAllowedForRobot(webUrl.getURL())
                && (containsQuery || noQueryParameter(webUrl));
        if (isCrawled && href.endsWith("pdf")) {
            indexPdf(href);
        }
        return isCrawled;
    }

    private void indexPdf(final String href) {
        final var bodyContentHandler = new BodyContentHandler();
        final var metadata = new Metadata();
        final var parseContext = new ParseContext();
        final var pdfParser = new PDFParser();
        try {
            final var url = new URL(href);
            final var urlStream = url.openStream();
            pdfParser.parse(urlStream, bodyContentHandler, metadata, parseContext);

            final var body = bodyContentHandler.toString();
            final var title = extractPdfTitle(metadata);

            final var sitePage = new SitePage(
                    title,
                    body,
                    href,
                    ""
            );

            indexPage(sitePage);
            countPage(href);
            LOG.warn("siteId: " + siteId + " - IS_PDF-increment: " + href);
        } catch (final IOException | TikaException | SAXException e) {
            LOG.warn("indexPdf_ERROR - url: " + href);
        }
    }

    private String extractPdfTitle(final Metadata metadata) {
        final String title;
        if (!StringUtils.isEmpty(metadata.get("title"))) {
            title = metadata.get("title");
        } else if (!StringUtils.isEmpty(metadata.get("pdf:docinfo:title"))) {
            title = metadata.get("pdf:docinfo:title");
        } else if (!StringUtils.isEmpty(metadata.get("dc:title"))) {
            title = metadata.get("dc:title");
        } else {
            title = "";
        }
        return title;
    }

    private boolean isAllowedForRobot(final String url) {
        return robotRules.isAllowed(url);
    }

    private boolean noQueryParameter(final WebURL url) {
        return URI.create(url.getURL()).getQuery() == null || URI.create(url.getURL()).getQuery().isEmpty();
    }

    @Override
    public void visit(final Page page) {
        final var url = page.getWebURL().getURL();

        if (page.getParseData() instanceof HtmlParseData) {
            final var htmlParseData = (HtmlParseData) page.getParseData();
            if (isNoindexPage(htmlParseData)) {
                return;
            }
            final var htmlStrippedBody = extractTextFromMixedHtml(htmlParseData.getHtml(), pageBodyCssSelector);
            final var title = htmlParseData.getTitle();
            final String thumbnail;
            if (htmlParseData.getMetaTags().get(SiteService.PAGE_THUMBNAIL_META_NAME) != null && htmlParseData.getMetaTags().get(SiteService.PAGE_THUMBNAIL_META_NAME).length() < 100_000) {
                thumbnail = htmlParseData.getMetaTags().get(SiteService.PAGE_THUMBNAIL_META_NAME);
            } else {
                thumbnail = "";
            }

            final var sitePage = new SitePage(
                    title,
                    htmlStrippedBody,
                    url,
                    thumbnail
            );

            indexPage(sitePage);
            countPage(url);
        } else {
            LOG.warn("visit_ERROR - siteId: " + siteId + " - url: " + url);
        }
    }

    private void countPage(String url) {
        if (PAGE_COUNT.get(siteId) == null) {
            PAGE_COUNT.put(siteId, new AtomicInteger());
        }
        final var currentPageCount = PAGE_COUNT.get(siteId).incrementAndGet();
        LOG.info("siteId: " + siteId + " - pageCount: " + currentPageCount);

        this.getMyController().getCrawlersLocalData().add(url);
    }

    private void indexPage(final SitePage sitePage) {
        try {
            final var request = new Request.Builder()
                    .url(SIS_API_SERVICE_URL + "/sites/" + siteId + "/pages?siteSecret=" + siteSecret) // TODO move this to a config property to switch between production and override with local
                    .put(RequestBody.create(JSON_MEDIA_TYPE, MAPPER.writeValueAsBytes(sitePage)))
                    .build();
            HTTP_CLIENT.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    call.cancel();
                    LOG.warn("siteId: " + siteId + " - URL: " + sitePage.getUrl() + " - exception: " + e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    LOG.debug("siteId: " + siteId + " - URL: " + sitePage.getUrl() + " - responseCode: " + response.code());
                    response.close();
                }
            });
        } catch (final IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private boolean isNoindexPage(HtmlParseData htmlParseData) {
        return htmlParseData.getMetaTags().get("robots") != null && htmlParseData.getMetaTags().get("robots").contains("noindex");
    }

    private String extractTextFromMixedHtml(String body, String pageBodyCssSelector) {
        final Document docPage = Jsoup.parse(body);
        final Element selectedBodyFragment = docPage.body().selectFirst(pageBodyCssSelector);
        if (selectedBodyFragment == null) {
            return docPage.body().text();
        }
        final String extractedPageBody = selectedBodyFragment.text();
        if (extractedPageBody.isEmpty()) {
            return docPage.body().text();
        }
        return extractedPageBody;
    }
}