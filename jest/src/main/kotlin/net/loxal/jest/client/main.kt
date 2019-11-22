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

package net.loxal.jest.client

import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window

private fun main() {
    window.onload = {
        js("IFS.eventbus.addEventListener(IFS.jQuery.ifs.constants.events.SEARCHBAR_RENDERED_INITIALLY, function () { jest.net.loxal.jest.client.initHelper(); }); ")
        window.setTimeout({
            if (!isInitialized)
                initHelper()
        }, 2_000)
    }
}

fun initHelper() {
    Stats()
}

private var isInitialized: Boolean = false
private val sisHookInit = document.currentScript

class Stats {
    private var analyticsTrackingId: String? = (sisHookInit as HTMLScriptElement).getAttribute("data-analyticsTrackingId")
    private val sisSearchbar: HTMLInputElement = document.getElementById("ifs-sb-searchfield") as HTMLInputElement

    private val isDebugView = window.location.search.contains("debug-view")
    private fun log(msg: Any?) {
        if (isDebugView) {
            println(msg)
        }
    }

    init {
        log("Site Search Analytics Initalized")
        isInitialized = true
        sisSearchbar.addEventListener("keydown", { event: Event ->
            val keyboardEvent = event as KeyboardEvent
            if (keyboardEvent.key === "Enter") {
                if (!sisSearchbar.value.isBlank()) {
                    pushAnalytics()
                }
            }
        })
    }

    private fun pushAnalytics() {
        log("analyticsTrackingId: $analyticsTrackingId")
        if (!analyticsTrackingId.isNullOrBlank()) {
            val xhr = XMLHttpRequest()
            val searchQueryParam = "query"
            val analyticsEndpoint = "https://www.google-analytics.com/r/collect?v=1&cid=0&tid="
            xhr.open("GET", "$analyticsEndpoint$analyticsTrackingId&dl=%3F$searchQueryParam%3D${sisSearchbar.value}")
            xhr.send()
            xhr.onload = {
                log("Search term pushed to analytics: ${sisSearchbar.value}")
            }
            xhr.onerror = {
                log("error: ${xhr.response}")
            }
        }
    }
}


