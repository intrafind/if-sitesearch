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

package com.intrafind.sitesearch;

import com.intrafind.sitesearch.controller.CrawlerController;
import com.intrafind.sitesearch.dto.Subscription;
import com.intrafind.sitesearch.dto.WooCommerceOrder;
import com.intrafind.sitesearch.service.SiteCrawler;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@SpringBootApplication
@RestController
@EnableSwagger2
public class Application {
    private final static Logger LOG = LoggerFactory.getLogger(Application.class);
    private static final String SERVICE_SECRET = System.getenv("SERVICE_SECRET");
    public static final URI IFINDER_CORE = URI.create("https://sitesearch:" + SERVICE_SECRET + "@" + System.getenv("SIS_SERVICE_HOST") + "/hessian"); // TODO consider trying json endpoint
    private static final String WOO_COMMERCE_CONSUMER_KEY = System.getenv("WOO_COMMERCE_CONSUMER_KEY");
    private static final String WOO_COMMERCE_CONSUMER_SECRET = System.getenv("WOO_COMMERCE_CONSUMER_SECRET");
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static Mac macSha1Algorithm;

    static {
        try {
            macSha1Algorithm = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            final var secretKey = new SecretKeySpec(SERVICE_SECRET.getBytes(), HMAC_SHA1_ALGORITHM);
            macSha1Algorithm.init(secretKey);
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Application#static_ERROR: " + e.getMessage());
        }
    }

    @RequestMapping(path = "/subscriptions/woo-commerce/{subscriptionId}", method = RequestMethod.POST)
    ResponseEntity<Subscription> subscribeViaSite(
            @PathVariable(value = "subscriptionId") String subscriptionId,
            @RequestParam(value = "affiliate", required = false) String affiliate
    ) {
        final var request = new Request.Builder()
                .url("https://sitesearch.cloud/wp-json/wc/v1/orders/"
                        + subscriptionId
                        + "?consumer_key=" + WOO_COMMERCE_CONSUMER_KEY
                        + "&consumer_secret=" + WOO_COMMERCE_CONSUMER_SECRET
                )
                .build();
        try {
            final var response = SiteCrawler.HTTP_CLIENT.newCall(request).execute();
            if (isExistingOrder(response)) {
                final var rawSubscription = response.body().bytes();
                final var order = CrawlerController.MAPPER.readValue(rawSubscription, WooCommerceOrder.class);

                final var subscriptionPlan = order.getLineItems().<WooCommerceOrder.LineItem>get(0).getSku();
                final String siteId = order.getSiteId();
                LOG.info("siteId: " + siteId + " - subscriptionId: " + subscriptionId + " - subscriptionPlan: " + subscriptionPlan + " - affiliate: " + affiliate);
                return ResponseEntity
                        .status(HttpStatus.OK)
                        .body(new Subscription(
                                subscriptionId,
                                subscriptionPlan,
                                order.getPaymentMethod(),
                                siteId,
                                affiliate,
                                new String(rawSubscription))
                        );
            }
        } catch (final IOException e) {
            LOG.error("subscriptionId: " + subscriptionId + " - subscribeViaSite_ERROR: " + e.getMessage());
        }

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
    }

    private boolean isExistingOrder(final Response response) {
        return HttpStatus.OK.value() == response.code() && response.body() != null;
    }

    @RequestMapping(path = "/subscriptions/github", method = RequestMethod.POST)
    ResponseEntity<Object> subscribeViaGitHub(
            @RequestHeader(value = "X-GitHub-Delivery") UUID delivery,
            @RequestHeader(value = "X-GitHub-Event") String event,
            @RequestHeader(value = "X-Hub-Signature") String signature,
            @RequestBody String subscription
    ) {
        final var isAuthenticGitHubEvent = verifySha1Signature(subscription, signature);
        if (isAuthenticGitHubEvent) {
            LOG.info("github-delivery: " + delivery
                    + " - github-event: " + event
                    + " - github-signature: " + signature
                    + " - github-subscription: " + subscription
            );
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(subscription);
        }

        return ResponseEntity
                .unprocessableEntity()
                .build();
    }

    private boolean verifySha1Signature(String subscription, String signature) {
        final var expectedSha1Hash = macSha1Algorithm.doFinal(subscription.getBytes());
        final var expectedSignature = "sha1=" + Hex.encodeHexString(expectedSha1Hash);

        return expectedSignature.toLowerCase().equals(signature);
    }

//    // for testing GitHub OAuth2 requests only // TODO remove altogether later on
//    @RequestMapping(path = "/login/test", method = RequestMethod.POST)
//    ResponseEntity<Object> login(
//            @RequestParam(value = "code", required = false) String code,
//            @RequestParam(value = "access_token", required = false) String token,
//            @RequestParam(value = "client_id", required = false) String id,
//            @RequestParam(value = "client_secret", required = false) String secret,
//            @RequestParam(value = "state", required = false) String state,
//            @RequestParam(value = "redirect_uri", required = false) String redirect_uri,
//            @RequestBody(required = false) Object o
//    ) {
//
//        LOG.info("code: " + code);
//        LOG.info("token: " + token);
//        LOG.info("id: " + id);
//        LOG.info("secret: " + secret);
//        LOG.info("state: " + state);
//        LOG.info("redirect_uri: " + redirect_uri);
//        LOG.info("o: " + o);
//        return ResponseEntity
//                .status(HttpStatus.FORBIDDEN)
//                .body("Error Message");
//    }
//
//    // for testing GitHub OAuth2 requests only // TODO remove altogether later on
//    @RequestMapping(path = "/login/test1", method = RequestMethod.GET)
//    ResponseEntity<Object> login1(
//            @RequestParam(value = "code", required = false) String code,
//            @RequestParam(value = "access_token", required = false) String token,
//            @RequestParam(value = "client_id", required = false) String id,
//            @RequestParam(value = "client_secret", required = false) String secret,
//            @RequestParam(value = "state", required = false) String state,
//            @RequestParam(value = "redirect_uri", required = false) String redirect_uri,
//            @RequestBody(required = false) Object o
//    ) {
//        LOG.info("code: " + code);
//        LOG.info("token: " + token);
//        LOG.info("id: " + id);
//        LOG.info("secret: " + secret);
//        LOG.info("state: " + state);
//        LOG.info("redirect_uri: " + redirect_uri);
//        LOG.info("o: " + o);
//        return ResponseEntity.ok(o);
//    }

    public static void main(final String... args) {
        SpringApplication.run(Application.class, args);
//        final ConfigurableApplicationContext run = SpringApplication.run(Application.class, args);
//        run.addApplicationListener((ApplicationListener<ContextClosedEvent>) event ->
//                LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Close Event triggered"));
    }
}
