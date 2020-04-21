/*
 * Copyright 2020 IntraFind Software AG. All rights reserved.
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

import com.intrafind.sitesearch.dto.Subscription;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SubscriptionTest {
    private final static Logger LOG = LoggerFactory.getLogger(SubscriptionTest.class);
    @Autowired
    private TestRestTemplate caller;

    /**
     * Use https://www.+Application.SIS_DOMAIN/pricing?affiliate=sis-integration_test to create an affiliate ID cookie.
     */
    @Ignore("WooCommerce has been canceled. Hence this test started to fail.")
    @Test
    public void subscribeViaSite() {
        final var orderId = "699";
        final var affiliateId = "sis-integration_test";
        final var response = caller.exchange(
                "/subscriptions/woo-commerce/" + orderId + "?affiliate=" + affiliateId,
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Subscription.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final var subscription = response.getBody();
        assertNotNull(subscription);
        assertNotNull(orderId, subscription.getId());
        assertNotNull(subscription.getSiteId());
        assertNotNull(subscription.getPlan());
        assertEquals(affiliateId, subscription.getAffiliate());
        assertNotNull(subscription.getPaymentMethod());
        assertNotNull(subscription.getRawSubscription());
    }

    @Test
    public void subscribeViaGitHub() {
        final var gitHubEventPayload = "{\"github-data\": \"Here is some salt to make a rainbow attack harder.\"}";

        final var gitHubWebhookHeaders = new HttpHeaders();
        gitHubWebhookHeaders.add("X-Hub-Signature", "sha1=778a6bcb65bc5ff6d62ed91c4be70058d7f99a6a");
        gitHubWebhookHeaders.add("X-GitHub-Delivery", UUID.randomUUID().toString());
        gitHubWebhookHeaders.add("X-GitHub-Event", "ping");

        final var gitHubEvent = new HttpEntity<>(gitHubEventPayload, gitHubWebhookHeaders);
        final var response = caller.exchange(
                "/subscriptions/github",
                HttpMethod.POST,
                gitHubEvent,
                Object.class
        );
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final var subscription = response.getBody();
        assertNotNull(subscription);
    }
}
