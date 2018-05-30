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

package com.intrafind.sitesearch.jmh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intrafind.sitesearch.controller.AutocompleteController;
import com.intrafind.sitesearch.controller.SearchController;
import com.intrafind.sitesearch.controller.SiteController;
import com.intrafind.sitesearch.dto.Autocomplete;
import com.intrafind.sitesearch.dto.Hits;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@State(Scope.Benchmark)
public class LoadTest {
    static final String[] LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras viverra enim vitae malesuada placerat. Nam auctor pellentesque libero, et venenatis enim molestie vel. Duis est metus, congue quis orci id, tincidunt mattis turpis. In fringilla ultricies sapien ultrices accumsan. Sed mattis tellus lacus, quis scelerisque turpis hendrerit et. In iaculis malesuada ipsum, ac rhoncus mauris auctor quis. Proin varius, ex vestibulum condimentum lacinia, ligula est finibus ligula, id consectetur nisi enim ut velit. Sed aliquet gravida justo ac condimentum. In malesuada sed elit vitae vestibulum. Mauris vitae congue lacus. Quisque vitae tincidunt orci. Donec viverra enim a lacinia pulvinar. Sed vel ullamcorper est. Vestibulum vel urna at nisl tincidunt blandit. Donec purus leo, interdum in diam in, posuere varius tellus. Quisque eleifend nulla at nulla vestibulum ullamcorper. Praesent interdum vehicula cursus. Morbi vitae nunc et urna rhoncus semper aliquam nec velit. Quisque aliquet et velit ut mollis. Sed mattis eleifend tristique. Praesent pharetra, eros eget viverra tempus, nisi turpis molestie metus, nec tristique nulla dolor a mauris. Nullam cursus finibus erat, in pretium urna fermentum ac. In hac habitasse platea dictumst. Cras id velit id nisi euismod eleifend. Duis vehicula gravida bibendum. Cras rhoncus, massa et accumsan euismod, metus arcu rutrum orci, eu porttitor lacus tellus sed quam. Morbi tincidunt est sit amet sem convallis porta in nec nisi. Sed ex enim, fringilla nec diam in, luctus pulvinar enim. Suspendisse potenti. Quisque ut pellentesque erat. In tincidunt metus id sem fringilla sagittis. Interdum et malesuada fames ac ante ipsum primis in faucibus. Proin erat nunc, pharetra sit amet iaculis nec, malesuada eu dui. Nullam sagittis ut arcu vitae convallis. Mauris molestie gravida lectus, eu commodo quam bibendum aliquam. Donec laoreet sed dolor eu consectetur."
            .split("\\s");
    static final String LOAD_TARGET = "https://api.sitesearch.cloud";
    static final OkHttpClient CALLER = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .pingInterval(1, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .build();
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final Random PSEUDO_ENTROPY = new Random();
    static final Map<String, Integer> SEARCH_QUERIES = new HashMap<>();
    static final Map<String, Integer> AUTOCOMPLETE_QUERIES = new HashMap<>();
    static final Map<UUID, Map<String, Integer>> SEARCH_DATA = new HashMap<>();
    static final Map<UUID, Map<String, Integer>> AUTOCOMPLETE_DATA = new HashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(LoadTest.class);
    private static final UUID LOAD_SITE_ID = UUID.fromString("563714f1-96c0-4500-b366-4fc7e734fa1d");

    static {
        SEARCH_QUERIES.put("hypothek", 40);
        SEARCH_QUERIES.put("swiss", 30);
        SEARCH_QUERIES.put("migros", 40);
        SEARCH_QUERIES.put("investieren", 40);
        SEARCH_QUERIES.put("\uD83E\uDD84", -1);

        AUTOCOMPLETE_QUERIES.put("hyp", 0);
        AUTOCOMPLETE_QUERIES.put("swi", 6);
        AUTOCOMPLETE_QUERIES.put("mig", 1);
        AUTOCOMPLETE_QUERIES.put("inv", 8);
        AUTOCOMPLETE_QUERIES.put("bank", 1);
        AUTOCOMPLETE_QUERIES.put("fond", 1);
        AUTOCOMPLETE_QUERIES.put("welt", 4);
        AUTOCOMPLETE_QUERIES.put("\uD83E\uDD84", -1);

        SEARCH_DATA.put(LOAD_SITE_ID, SEARCH_QUERIES); // https://www.migrosbank.ch/de, https://blog.migrosbank.ch/de
        AUTOCOMPLETE_DATA.put(LOAD_SITE_ID, AUTOCOMPLETE_QUERIES); // https://www.migrosbank.ch/de, https://blog.migrosbank.ch/de
    }

    public static void main(String... args) throws Exception {
        final Options options = new OptionsBuilder()
                .warmupIterations(1)
                .measurementIterations(5)
//                .include(".*")
//                .include(LoadIndex2Users.class.getSimpleName())
                .include(LoadTest.class.getSimpleName())
                .forks(1)
                .threads(150)
                .mode(Mode.Throughput)
                .resultFormat(ResultFormatType.JSON)
                .result("build/jmh-result.json")
                .shouldFailOnError(true)
                .build();

        new Runner(options).run();
    }

    @Benchmark
    public void staticFiles() throws IOException {
        final Request request = new Request.Builder()
                .url(LOAD_TARGET)
                .build();
        final Response response = CALLER.newCall(request).execute();
        assertEquals(HttpStatus.OK.value(), response.code());
        assertNotNull(response.body());
        response.close();
    }

    @Benchmark
    public void search() throws IOException {
        final int randomSiteIndex = PSEUDO_ENTROPY.nextInt(SEARCH_DATA.size());
        final UUID randomSiteId = (UUID) SEARCH_DATA.keySet().toArray()[randomSiteIndex];
        final Map<String, Integer> randomSite = SEARCH_DATA.get(randomSiteId);
        final int randomQueryIndex = PSEUDO_ENTROPY.nextInt(SEARCH_QUERIES.size());
        final String randomQuery = (String) randomSite.keySet().toArray()[randomQueryIndex];
        final int queryHits = randomSite.get(randomQuery);

        final Request request = new Request.Builder()
                .url(LOAD_TARGET + SiteController.ENDPOINT + "/" + randomSiteId + SearchController.ENDPOINT + "?query=" + randomQuery)
                .build();
        final Response response = CALLER.newCall(request).execute();
        assertEquals(HttpStatus.OK.value(), response.code());
        if (queryHits == 0) {
            assertNotNull(response.body());
        } else {
            final Hits result = MAPPER.readValue(response.body().charStream(), Hits.class);
            assertTrue(queryHits < result.getResults().size());
            assertEquals(randomQuery, result.getQuery());
        }
        response.close();
    }

    @Benchmark
    public void autocomplete() throws IOException {
        final int randomSiteIndex = PSEUDO_ENTROPY.nextInt(SEARCH_DATA.size());
        final UUID randomSiteId = (UUID) AUTOCOMPLETE_DATA.keySet().toArray()[randomSiteIndex];
        final Map<String, Integer> randomSite = AUTOCOMPLETE_DATA.get(randomSiteId);
        final int randomQueryIndex = PSEUDO_ENTROPY.nextInt(AUTOCOMPLETE_QUERIES.size());
        final String randomQuery = (String) randomSite.keySet().toArray()[randomQueryIndex];
        final int queryHits = randomSite.get(randomQuery);

        final Request request = new Request.Builder()
                .url(LOAD_TARGET + SiteController.ENDPOINT + "/" + randomSiteId + AutocompleteController.ENDPOINT + "?query=" + randomQuery)
                .build();
        final Response response = CALLER.newCall(request).execute();
        assertEquals(HttpStatus.OK.value(), response.code());
        final Autocomplete result = MAPPER.readValue(response.body().charStream(), Autocomplete.class);
        assertTrue(queryHits + " - " + result.getResults().size(), queryHits <= result.getResults().size());
        response.close();
    }
}
