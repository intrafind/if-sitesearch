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
    id("me.champeau.gradle.jmh") version "0.4.8"
    id("io.morethan.jmhreport") version "0.9.0"
    id("org.springframework.boot") version "2.1.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.7.RELEASE"
    id("com.google.cloud.tools.jib") version "1.2.0"
}

idea {
    module {
        inheritOutputDirs = true
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_12
    targetCompatibility = JavaVersion.VERSION_12
}

repositories {
    jcenter()
}

dependencies {
    val springBootVersion = "2.1.5.RELEASE"
    val swaggerVersion = "2.9.2"
    val tikaVersion = "1.21"

    runtimeOnly("org.apache.tika:tika-parsers:$tikaVersion")
    compile("org.apache.tika:tika:$tikaVersion")
    compile("org.springframework.security.oauth.boot:spring-security-oauth2-autoconfigure:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-security:$springBootVersion")

    compile("edu.uci.ics:crawler4j:4.4.0")
    compile("com.github.crawler-commons:crawler-commons:1.0")

    compile("com.rometools:rome:1.12.0")

    compile("com.caucho:hessian:4.0.60")

    compile("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    compile("org.springframework.boot:spring-boot-starter-undertow:$springBootVersion") {
        exclude(module = "undertow-websockets-jsr")
    }

    compile("io.springfox:springfox-swagger2:$swaggerVersion")
    compile("io.springfox:springfox-swagger-ui:$swaggerVersion")

    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.21")
    testCompile("org.openjdk.jmh:jmh-core:1.21")
    testCompile("org.springframework.boot:spring-boot-starter-test:$springBootVersion")

    compile("org.codehaus.groovy:groovy-templates:3.0.0-beta-1")
    runtimeClasspath("org.springframework.boot:spring-boot-devtools:$springBootVersion")

    compile("com.squareup.okhttp3:okhttp:3.14.2")
    compile("org.jsoup:jsoup:1.12.1")

    compile("org.mnode.mstor:mstor:1.0.0")
    compile("com.google.oauth-client:google-oauth-client-jetty:1.28.0")
    compile("com.google.apis:google-api-services-gmail:v1-rev103-1.23.0")
}

jib {
    from.image = "openjdk:13-alpine"
    to.image = "docker-registry.intrafind.net/intrafind/sis-sitesearch:latest"
    container.mainClass = "com.intrafind.sitesearch.Application"
}

jmh {
    include = listOf("LoadTest")
    warmupIterations = 1
    warmupForks = 0
    fork = 1
    iterations = 3
    resultFormat = "JSON"
    threads = 40
    failOnError = true
}
