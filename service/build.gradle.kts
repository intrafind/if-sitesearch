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

// Migrate to Kotlin https://guides.gradle.org/migrating-build-logic-from-groovy-to-kotlin/
plugins {
    java
    idea
    id("me.champeau.gradle.jmh") version "0.5.0-rc-1"
    id("io.morethan.jmhreport") version "0.9.0"
    id("org.springframework.boot") version "2.1.9.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    id("com.google.cloud.tools.jib") version "1.6.1"
}

idea {
    module {
        inheritOutputDirs = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    val springBootVersion = "2.1.9.RELEASE"
    val swaggerVersion = "2.9.2"
    val tikaVersion = "1.22"

    runtimeOnly("org.apache.tika:tika-parsers:$tikaVersion")
    implementation("org.apache.tika:tika:$tikaVersion")
    implementation("org.springframework.security.oauth.boot:spring-security-oauth2-autoconfigure:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-security:$springBootVersion")

    implementation("edu.uci.ics:crawler4j:4.4.0")
    implementation("com.github.crawler-commons:crawler-commons:1.0")

    implementation("com.rometools:rome:1.12.2")

    implementation("com.caucho:hessian:4.0.63")

    implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-undertow:$springBootVersion") {
        exclude(module = "undertow-websockets-jsr")
    }

    implementation("io.springfox:springfox-swagger2:$swaggerVersion")
    implementation("io.springfox:springfox-swagger-ui:$swaggerVersion")

    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.21")
    testImplementation("org.openjdk.jmh:jmh-core:1.21")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")

    implementation("org.codehaus.groovy:groovy-templates:3.0.0-rc-1")
    runtimeOnly("org.springframework.boot:spring-boot-devtools:$springBootVersion")

    implementation("com.squareup.okhttp3:okhttp:4.2.2")
    implementation("org.jsoup:jsoup:1.12.1")

    implementation("org.mnode.mstor:mstor:1.0.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.30.4")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20190602-1.30.1")
}

jib {
    from.image = "openjdk:13-slim-buster"
    container.mainClass = "com.intrafind.sitesearch.Application"
    to.image = "docker-registry.intrafind.net/intrafind/sis-sitesearch:tmp"
    to.auth.username = "sitesearch"
    to.auth.password = System.getenv("PASSWORD")
}

jmh {
    include = listOf("LoadTest")
    warmupIterations = 1
    warmupForks = 0
    fork = 1
    iterations = 3
    resultFormat = "JSON"
    threads = 20
    failOnError = true
}
