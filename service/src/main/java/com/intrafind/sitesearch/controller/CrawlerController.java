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

package com.intrafind.sitesearch.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.intrafind.sitesearch.dto.CaptchaVerification;
import com.intrafind.sitesearch.dto.CrawlStatus;
import com.intrafind.sitesearch.dto.CrawlerJobResult;
import com.intrafind.sitesearch.dto.SiteProfile;
import com.intrafind.sitesearch.dto.SitesCrawlStatus;
import com.intrafind.sitesearch.service.CrawlerService;
import com.intrafind.sitesearch.service.SiteCrawler;
import com.intrafind.sitesearch.service.SiteService;
import okhttp3.Request;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static com.intrafind.sitesearch.service.SiteCrawler.JSON_MEDIA_TYPE;

@RestController
@RequestMapping(SiteController.ENDPOINT)
public class CrawlerController {
    private static final Logger LOG = LoggerFactory.getLogger(CrawlerController.class);
    private static final String PROSPECTS_EMAIL_ADDRESS = "Support - Site Search <f518c8ec.intrafind.de@emea.teams.ms>";
    private static final String INVISIBLE_RECAPTCHA_SITE_SECRET = System.getenv("INVISIBLE_RECAPTCHA_SITE_SECRET");
    public static final String DEV_SKIP_FLAG = System.getenv("DEV_SKIP_FLAG");
    private final SiteService siteService;
    private final CrawlerService crawlerService;

    @Autowired
    private CrawlerController(final SiteService siteService, final CrawlerService crawlerService) {
        this.siteService = siteService;
        this.crawlerService = crawlerService;
    }

    public static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);
    private static final String SERVICE_CONFIG_PATH = "config/";
    private static final File DATA_STORE_DIR = new File(SERVICE_CONFIG_PATH + "gmail-api");
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
    }

    private static MimeMessage createEmail(String to, String subject, String bodyText) throws Exception {
        final var props = new Properties();
        final var session = Session.getDefaultInstance(props, null);

        final var email = new MimeMessage(session);
        email.setFrom(new InternetAddress("team@sitesearch.cloud"));
        email.setReplyTo(new InternetAddress[]{new InternetAddress("feedback@sitesearch.cloud")});
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(PROSPECTS_EMAIL_ADDRESS));
        email.setSubject(subject);
        email.setText(bodyText, "UTF-8", "html");
        return email;
    }

    private static com.google.api.services.gmail.model.Message createMessageWithEmail(MimeMessage emailContent) throws Exception {
        final var buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        final var bytes = buffer.toByteArray();
        final var encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        final var message = new com.google.api.services.gmail.model.Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private static com.google.api.services.gmail.model.Message sendMessage(Gmail service, String userId, MimeMessage emailContent) throws Exception {
        var message = createMessageWithEmail(emailContent);
        message = service.users().messages().send(userId, message).execute();

        LOG.debug(message.toPrettyString());
        return message;
    }

    public static Credential authorize() throws IOException {
        final var resourceAsStream = new FileInputStream(new File(SERVICE_CONFIG_PATH + "gmail-api-client_secret.json"));
        final var clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(resourceAsStream));

        final var flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        final var credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        LOG.debug("Credentials saved to: " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    private static Gmail initGmailService() throws IOException {
        final var credential = authorize();
        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName("Site Search")
                .build();
    }

    private static void sendSetupInfoEmail(UUID siteId, UUID siteSecret, URI url, String email, int pageCount) throws Exception {
        final var service = initGmailService();
        LOG.debug("servicePath: " + service.getServicePath());

        final var message = sendMessage(service, "me",
                createEmail(
                        email,
                        "Evaluation Information - Site Search",
                        "Hello," +
                                "<br/><br/>you are just a few steps away from adding Site Search to your website." +
                                "<br/>Below you should find everything you need to evaluate Site Search for your website." +
                                "<dl><dt>Website URL:</dt><dd>" + url +
                                "</dd><dt>Pages crawled:</dt><dd>" + pageCount +
                                "</dd><dt>Site ID:</dt><dd>" + siteId +
                                "</dd><dt>Site Secret:</dt><dd>" + siteSecret +
                                "</dd></dl>" +
                                "<a href='" + "https://sitesearch.cloud/getting-started/?siteId=" + siteId + "&siteSecret=" + siteSecret + "&url=" + url + "'>Try Site Search as it would look like on your site, using this evaluation link.</a>" +
                                "<br/>Please do not hesitate to ask us any questions you should encounter during your 14-day evaluation period!" +
                                "<br/>After the 14-day trial period, you can just continue using Site Search by <a href='https://sitesearch.cloud/pricing'>subscribing to one of our offerings</a>." +
                                "<br/><br/>Using the credentials above, you agree with our <a href='https://sitesearch.cloud/terms'>Terms & Conditions</a>." +
                                "<br/><br/>Cheers," +
                                "<br/>Site Search Team"
                )
        );
    }

    @RequestMapping(path = "crawl", method = RequestMethod.POST)
    ResponseEntity<SitesCrawlStatus> recrawl(
            @RequestParam(value = "serviceSecret") UUID serviceSecret,
            @RequestBody SitesCrawlStatus sitesCrawlStatusUpdate,
            @RequestParam(required = false, value = "allSitesCrawl", defaultValue = "false") boolean allSitesCrawl,
            @RequestParam(required = false, value = "isThrottled", defaultValue = "true") boolean isThrottled,
            @RequestParam(required = false, value = "clearIndex", defaultValue = "false") boolean clearIndex
    ) {
        final var sitesCrawlStatus = crawlSite(serviceSecret, sitesCrawlStatusUpdate, allSitesCrawl, isThrottled, clearIndex);
        return sitesCrawlStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    private Optional<SitesCrawlStatus> crawlSite(UUID serviceSecret, SitesCrawlStatus sitesCrawlStatusUpdate, boolean allSiteCrawl, boolean isThrottled, boolean clearIndex) {
        final var sitesCrawlStatusOverall = new SitesCrawlStatus(new HashSet<>());
        if (SiteService.ADMIN_SITE_SECRET.equals(serviceSecret)) {
            final var halfDayAgo = Instant.now().minus(1, ChronoUnit.HALF_DAYS);
            sitesCrawlStatusUpdate.getSites().stream()
                    .filter(crawlStatus -> Instant.parse(crawlStatus.getCrawled()).isBefore(halfDayAgo) || allSiteCrawl)
                    .forEach(crawlStatus -> {
                        final var fetchedSiteSecret = siteService.fetchSiteSecret(crawlStatus.getSiteId());
                        fetchedSiteSecret.ifPresent(uuid -> {
                            final var siteSecret = uuid;
                            final var siteProfile = siteService.fetchSiteProfile(crawlStatus.getSiteId());
                            siteProfile.ifPresent(profile -> {
                                if (clearIndex && !siteService.clearIndex(profile.getId(), profile.getSecret())) {
                                    return;
                                }
                                final var pageCount = new AtomicLong();
                                profile.getConfigs().forEach(configBundle ->
                                        profile.getConfigs().stream().filter(config -> config.getUrl().equals(configBundle.getUrl())).findAny().ifPresent(config -> {
                                            final CrawlerJobResult crawlerJobResult = crawlerService.crawl(
                                                    configBundle.getUrl().toString(),
                                                    crawlStatus.getSiteId(),
                                                    siteSecret,
                                                    isThrottled,
                                                    configBundle.isSitemapsOnly(),
                                                    configBundle.getPageBodyCssSelector(),
                                                    config.isAllowUrlWithQuery());
                                            pageCount.addAndGet(crawlerJobResult.getPageCount());
                                            final var sitesCrawlStatus = siteService.updateCrawlStatusInShedule(crawlStatus.getSiteId(), pageCount.get());// TODO fix PATCH update instead of a regular PUT
                                            sitesCrawlStatus.ifPresent(element -> sitesCrawlStatusOverall.getSites().addAll(element.getSites()));
                                            siteService.removeOldSiteIndexPages(crawlStatus.getSiteId());
                                            LOG.info("siteId: " + crawlStatus.getSiteId() + " - siteUrl: " + configBundle.getUrl().toString() + " - pageCount: " + crawlerJobResult.getPageCount()); // TODO add pattern to logstash
                                        }));
                                sitesCrawlStatusOverall.getSites().add(new CrawlStatus(profile.getId(), Instant.now(), pageCount.get()));
                            });
                        });
                    });
            return Optional.of(sitesCrawlStatusOverall);
        }
        return Optional.empty();
    }

    @RequestMapping(path = "crawl/status", method = RequestMethod.PUT)
    ResponseEntity<SitesCrawlStatus> updateCrawlStatus(
            @RequestParam(value = "serviceSecret") UUID serviceSecret,
            @RequestBody SitesCrawlStatus sitesCrawlStatusUpdate
    ) {
        final var sitesCrawlStatus = siteService.storeCrawlStatus(serviceSecret, sitesCrawlStatusUpdate);
        return sitesCrawlStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @RequestMapping(path = "crawl/status", method = RequestMethod.GET)
    ResponseEntity<SitesCrawlStatus> fetchCrawlStatus(
            @RequestParam(value = "serviceSecret") UUID serviceSecret
    ) {
        final var sitesCrawlStatus = siteService.fetchCrawlStatus(serviceSecret);
        return sitesCrawlStatus.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @RequestMapping(path = "{siteId}/recrawl", method = RequestMethod.POST)
    ResponseEntity<CrawlerJobResult> recrawl(     // TODO add test that actually checks if obsolete pages are removed, `recrawlMultiSiteConfig` is currently the only test
                                                  @PathVariable(value = "siteId") UUID siteId,
                                                  @RequestParam(value = "siteSecret") UUID siteSecret,
                                                  @RequestParam(value = "clearIndex", required = false, defaultValue = "false") boolean clearIndex
    ) {
        if (!siteService.isAllowedToModify(siteId, siteSecret)) {
            return ResponseEntity.notFound().build();
        }
        final var siteProfile = siteService.fetchSiteProfile(siteId, siteSecret);
        if (siteProfile.isPresent()) {
            if (clearIndex) {
                siteService.clearIndex(siteId, siteSecret);
            }
            final var crawlerJobResult = crawlerService.recrawl(siteId, siteSecret, siteProfile.get());

            final var indexCleanupResultOptional = siteService.removeOldSiteIndexPages(siteId);
            indexCleanupResultOptional.ifPresent(indexCleanupResult -> {
                LOG.info("siteId: " + siteId + " - deletedPageCount: " + indexCleanupResult.getPageCount()); // TODO consolidate with the main&final LOG.info, add to logstash
            });

            LOG.info("siteId: " + siteId + " - siteSecret: " + siteSecret + " - pageCount: " + crawlerJobResult.getPageCount()); // TODO remove siteSecret from logs
            return ResponseEntity.ok(crawlerJobResult);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(path = "{siteId}/crawl", method = RequestMethod.POST)
    ResponseEntity<CrawlerJobResult> crawl(
            @PathVariable(value = "siteId") UUID siteId,
            @RequestParam(value = "siteSecret") UUID siteSecret,
            @RequestParam(value = "url") URI url,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "token") String captchaToken,
            @RequestParam(value = "sitemapsOnly", required = false, defaultValue = "false") boolean sitemapsOnly,
            @RequestParam(value = "allowUrlWithQuery", required = false, defaultValue = "false") boolean allowUrlWithQuery,
            @RequestParam(value = "pageBodyCssSelector", required = false, defaultValue = SiteProfile.Config.DEFAULT_PAGE_BODY_CSS_SELECTOR) String pageBodyCssSelector
    ) {
        if (!siteService.isAllowedToModify(siteId, siteSecret)) {
            return ResponseEntity.notFound().build();
        }

        boolean captchaPassed = false;
        try {
            final var request = new Request.Builder()
                    .url("https://www.google.com/recaptcha/api/siteverify?secret=" + INVISIBLE_RECAPTCHA_SITE_SECRET + "&response=" + captchaToken)
                    .post(okhttp3.RequestBody.create("", JSON_MEDIA_TYPE))
                    .build();
            final var response = SiteCrawler.HTTP_CLIENT.newCall(request).execute();
            final var captchaVerification = MAPPER.readValue(Objects.requireNonNull(response.body()).charStream(), CaptchaVerification.class);

            // temporarily allow pseudo-abuse-protection, using a fixed token
            if (captchaVerification.getSuccess() || "true".equals(DEV_SKIP_FLAG) || "1a46b7c0-8684-11e8-8f10-d74554b855dc".equals(captchaToken)) {
                captchaPassed = true;
            }
        } catch (final IOException e) {
            LOG.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        if (captchaPassed) {
            final var crawlerJobResult = crawlerService.crawl(url.toString(), siteId, siteSecret, true, sitemapsOnly, pageBodyCssSelector, allowUrlWithQuery);
            final var emailAddress = determineEmailAddress(email);
            try {
                sendSetupInfoEmail(siteId, siteSecret, url, emailAddress, crawlerJobResult.getPageCount());
            } catch (Exception e) {
                LOG.error("EMAIL_FAILURE_EVALUATION: " + e.getMessage());
            }
            LOG.info("siteId: " + siteId + " - siteSecret: " + siteSecret + " - siteUrl: " + url + " - pageCount: " + crawlerJobResult.getPageCount() + " - email: " + email); // TODO remove siteSecret from logs
            return ResponseEntity.ok(crawlerJobResult);
        } else {
            return ResponseEntity.unprocessableEntity().build();
        }
    }

    private String determineEmailAddress(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return PROSPECTS_EMAIL_ADDRESS;
        } else {
            return email;
        }
    }
}
