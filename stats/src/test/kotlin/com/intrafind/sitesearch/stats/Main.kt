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

package com.intrafind.sitesearch.stats

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Main {

    @Test
    fun testResult() {
        assertEquals(10, result())
//        val stats = Stats()
//        initHelper()
//        assertEquals(Stats(), initHelper())
//        assertEquals("12",Stats().analyticsTrackingId)
//        assertEquals("12",Stats().analyticsTrackingId)
    }

    @Test
    fun testNeptun() {
        assertEquals(11, neptun())
    }

    @Test
    fun testSaturn() {
        assertEquals(12, saturn())
    }

    @Ignore
    @Test
    fun testFailure() {
        assertEquals(20, result())
    }
}

class TestTestTest {
    @Test
    fun emptyTest() {
        // Will not run
    }
}
