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

package com.intrafind.sitesearch.dashboard

import com.intrafind.sitesearch.dashboard.SiteSearch.Companion.crawlerFinishedEvent
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.events.Event
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window

private suspend fun main(args: Array<String>) {
    window.addEventListener("DOMContentLoaded", {
        init()
    })

    document.addEventListener(crawlerFinishedEvent, {
        recrawl.disabled = false
        recrawl.textContent = "Recrawl & Reindex Site"
    })
}

private var siteId: String = ""
private var siteSecret: String = ""
private val serviceUrl: String = window.location.origin

private lateinit var updateSiteProfile: HTMLButtonElement
private lateinit var pageBodyCssSelector: HTMLInputElement
private lateinit var allowUrlWithQuery: HTMLInputElement
private lateinit var sitemapsOnly: HTMLInputElement
private lateinit var url: HTMLInputElement
private lateinit var pageCountContainer: HTMLDivElement
private lateinit var pageCount: HTMLParagraphElement
private lateinit var siteIdElement: HTMLDivElement
private lateinit var siteSecretElement: HTMLDivElement
private lateinit var recrawl: HTMLButtonElement
private lateinit var profile: SiteProfile

private fun init() {
    updateSiteProfile = document.getElementById("updateSiteProfile") as HTMLButtonElement
    pageBodyCssSelector = document.getElementById("pageBodyCssSelector") as HTMLInputElement
    allowUrlWithQuery = document.getElementById("allowUrlWithQuery") as HTMLInputElement
    sitemapsOnly = document.getElementById("sitemapsOnly") as HTMLInputElement
    url = document.getElementById("url") as HTMLInputElement
    pageCountContainer = document.getElementById("pageCountContainer") as HTMLDivElement
    pageCount = document.getElementById("pageCount") as HTMLParagraphElement
    siteIdElement = document.getElementById("siteId") as HTMLDivElement
    siteSecretElement = document.getElementById("siteSecret") as HTMLDivElement
    recrawl = document.getElementById("recrawl") as HTMLButtonElement

    applyQueryParameters()
}

private fun applyQueryParameters() {
    siteId = window.location.search.substring(window.location.search.indexOf("siteId=") + 7, 44)
    val siteSecretIndex = window.location.search.indexOf("siteSecret=") + 11
    siteSecret = window.location.search.substring(siteSecretIndex, siteSecretIndex + 44)
    fetchProfile()

    siteIdElement.textContent = siteId
    siteSecretElement.textContent = siteSecret
}

fun updateSiteProfile() {
    val xhr = XMLHttpRequest()
    xhr.open("PUT", "$serviceUrl/sites/$siteId/profile?siteSecret=$siteSecret")
    val siteProfileConfigs = listOf(SiteProfileConfig(
            url = url.value,
            sitemapsOnly = sitemapsOnly.checked,
//            allowUrlWithQuery = allowUrlWithQuery.checked, // TODO enable this and test it
            pageBodyCssSelector = pageBodyCssSelector.value
    ))
    val siteProfile = SiteProfile(id = siteId, secret = siteSecret, email = profile.email, configs = siteProfileConfigs)
    console.warn(siteProfile)
    console.warn(siteProfileConfigs)
    xhr.setRequestHeader("content-type", "application/json")
    xhr.send(JSON.stringify(siteProfile))
    xhr.onload = {
        profile = JSON.parse(xhr.responseText)
        showConfiguration()
    }
}

external fun encodeURIComponent(str: String): String
fun recrawl() {
    recrawl.disabled = true
    recrawl.textContent = "Crawling... please give us a minute or two."

//    val profileConfig: SiteProfileConfig = profile.configs.asDynamic()[0]
//    console.warn(profileConfig)
    val xhr = XMLHttpRequest()
    xhr.open("POST", "$serviceUrl/sites/$siteId/recrawl?siteSecret=$siteSecret&clearIndex=false")
    xhr.send()

    xhr.onload = {
        console.warn(xhr.responseText)
        if (xhr.status.equals(200)) {
            document.dispatchEvent(Event(crawlerFinishedEvent))
            val pageCount = JSON.parse<dynamic>(xhr.responseText).pageCount as Int
            showPageCount(pageCount)
        } else {
            console.error("FAILED")
        }
    }
}

private fun showPageCount(pagesRecrawled: Int) {
    pageCount.textContent = "$pagesRecrawled"
    pageCountContainer.classList.remove("sis-recrawl-container")
}

private fun fetchProfile() {
    val xhr = XMLHttpRequest()
    xhr.open("GET", "$serviceUrl/sites/$siteId/profile?siteSecret=$siteSecret")
    xhr.send()
    xhr.onload = {
        profile = JSON.parse(xhr.responseText)
        showConfiguration()
        ""
    }
}

private fun showConfiguration() {
    url.value = profile.configs.asDynamic()[0].url
    sitemapsOnly.checked = profile.configs.asDynamic()[0].sitemapsOnly
    allowUrlWithQuery.checked = profile.configs.asDynamic()[0].allowUrlWithQuery
    pageBodyCssSelector.value = profile.configs.asDynamic()[0].pageBodyCssSelector

    for (config in profile.configs.asDynamic()) {
        console.warn(config)
        console.warn(config.url)
        console.warn(config.sitemapsOnly)
        console.warn(config.pageBodyCssSelector)
        console.warn(config.allowUrlWithQuery)
    }

    console.warn(document.getElementById("profileConfig"))

    if (profile.configs.asDynamic().length > 1) {
        updateSiteProfile.disabled = true // TODO remove this precaution, once multi-configuration updates are supported in the UI
    }
}

class SiteSearch {
    companion object {
        const val crawlerFinishedEvent = "sis.crawlerFinished"
    }
}

data class SiteProfileConfig(val url: String = "", val pageBodyCssSelector: String = "body", val sitemapsOnly: Boolean = false, val allowUrlWithQuery: Boolean = false)

data class SiteProfile(val id: String = "", val secret: String = "", val configs: List<SiteProfileConfig> = emptyList(), val email: String = "")