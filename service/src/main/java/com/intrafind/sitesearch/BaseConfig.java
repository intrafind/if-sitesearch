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

package com.intrafind.sitesearch;

import com.google.common.base.Predicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;

@Configuration
public class BaseConfig {

    @Bean
    public Docket hideApi(final ServletContext servletContext) {
        return new Docket(DocumentationType.SWAGGER_2)
                .host(Application.SIS_SERVICE_HOST)
                .apiInfo(new ApiInfo(
                        "Site Search API",
                        "A simple website search solution",
                        "v1",
                        "https://" + Application.SIS_SERVICE_HOST + "/terms",
                        new Contact("IntraFind Software AG", "https://" + Application.SIS_SERVICE_HOST, "feedback@sitesearch.cloud"),
                        "Apache License, Version 2.0",
                        "https://github.com/intrafind/if-sitesearch/blob/master/LICENSE",
                        Collections.emptyList()
                ))
                .pathProvider(new RelativePathProvider(servletContext) {
                    @Override
                    public String getApplicationBasePath() {
                        return "/";
                    }
                })
                .select()
                .paths(Predicates.not(PathSelectors.regex("/legal/.*")))
                .paths(Predicates.not(PathSelectors.regex("/sites/crawl.*")))
                .paths(Predicates.not(PathSelectors.regex("/error")))
                .paths(Predicates.not(PathSelectors.regex("/login.*")))
                .paths(Predicates.not(PathSelectors.regex("/subscriptions/.*")))
                .paths(Predicates.not(PathSelectors.regex("/assignments.*")))
                .paths(Predicates.not(PathSelectors.regex("/pages/.*")))
                .paths(Predicates.not(PathSelectors.regex("/search")))
                .paths(Predicates.not(PathSelectors.regex("/autocomplete")))
                .paths(Predicates.not(PathSelectors.regex("/sites/rss")))
                .paths(Predicates.not(PathSelectors.regex("/sites/.+/pages/.*")))
                .paths(Predicates.not(PathSelectors.regex("/sites/.+/recrawl")))
                .paths(Predicates.not(PathSelectors.regex("/sites/.+/email.*")))
                .paths(Predicates.not(PathSelectors.regex("/sites/.+/xml")))
                .paths(Predicates.not(PathSelectors.regex("/sites/.+/rss")))
                .paths(Predicates.not(PathSelectors.regex("/sites/.+/crawl")))
                .paths(Predicates.not(PathSelectors.regex("/sites/.+/crawling")))
                .paths(Predicates.not(PathSelectors.regex("/authentication-providers.*")))
                .paths(Predicates.not(PathSelectors.regex("/user")))
                .paths(Predicates.not(PathSelectors.regex("/stats")))
                .build();
    }

    public static class TrustAllX509TrustManager implements X509TrustManager {
        private static final Logger LOG = LoggerFactory.getLogger(TrustAllX509TrustManager.class);

        static {
            try {
                final var sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustAllX509TrustManager()}, new SecureRandom());
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((string, sslSession) -> true);
            } catch (final NoSuchAlgorithmException | KeyManagementException e) {
                LOG.error("TrustAllX509TrustManager_ERROR: " + e.getMessage());
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
    }
}
