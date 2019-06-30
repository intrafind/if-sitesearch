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
import org.w3c.dom.DocumentFragment
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLOListElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLTemplateElement
import org.w3c.dom.asList
import org.w3c.dom.events.Event
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.clear

private fun main() {
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
private lateinit var pageCountContainer: HTMLDivElement
private lateinit var pageCount: HTMLParagraphElement
private lateinit var siteIdElement: HTMLDivElement
private lateinit var siteSecretElement: HTMLDivElement
private lateinit var recrawl: HTMLButtonElement
private lateinit var profile: SiteProfile

private fun init() {
    updateSiteProfile = document.getElementById("updateSiteProfile") as HTMLButtonElement
    pageCountContainer = document.getElementById("pageCountContainer") as HTMLDivElement
    pageCount = document.getElementById("pageCount") as HTMLParagraphElement
    siteIdElement = document.getElementById("siteId") as HTMLDivElement
    siteSecretElement = document.getElementById("siteSecret") as HTMLDivElement
    recrawl = document.getElementById("recrawl") as HTMLButtonElement
    profileConfigs = document.getElementById("profileConfigs") as HTMLOListElement
    configTemplate = (document.getElementById("profileConfig") as HTMLTemplateElement).content

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
    val req = XMLHttpRequest()
    req.open("PUT", "$serviceUrl/sites/$siteId/profile?siteSecret=$siteSecret")
    profile.configs.clear()
    for (profileConfig in profileConfigs.querySelectorAll("li").asList()) {
        profileConfig as HTMLLIElement
        val url = (profileConfig.querySelector("input[name=url]") as HTMLInputElement).value
        val pageBodyCssSelector = (profileConfig.querySelector("input[name=pageBodyCssSelector]") as HTMLInputElement).value
        val sitemapsOnly = (profileConfig.querySelector("input[name=sitemapsOnly]") as HTMLInputElement).checked
        val allowUrlWithQuery = (profileConfig.querySelector("input[name=allowUrlWithQuery]") as HTMLInputElement).checked

        profile.configs.add(SiteProfileConfig(
                url = url,
                pageBodyCssSelector = pageBodyCssSelector,
                sitemapsOnly = sitemapsOnly,
                allowUrlWithQuery = allowUrlWithQuery
        ))
    }
    req.setRequestHeader("content-type", "application/json")
    req.send(JSON.stringify(profile))
    req.onload = {
        profile = JSON.parse(req.responseText)
        fetchProfile()
    }
}

external fun encodeURIComponent(str: String): String
fun recrawl() {
    recrawl.disabled = true
    recrawl.textContent = "Crawling... please give us a minute or two."

    val req = XMLHttpRequest()
    req.open("POST", "$serviceUrl/sites/$siteId/recrawl?siteSecret=$siteSecret&clearIndex=false")
    req.send()

    req.onload = {
        console.warn(req.responseText)
        if (req.status.equals(200)) {
            document.dispatchEvent(Event(crawlerFinishedEvent))
            val pageCount = JSON.parse<dynamic>(req.responseText).pageCount as Int
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
    val req = XMLHttpRequest()
    req.open("GET", "$serviceUrl/sites/$siteId/profile?siteSecret=$siteSecret")
    req.send()
    req.onload = {
        val siteProfile = JSON.parse<SiteProfile>(req.responseText)
        profile = siteProfile
        val configs = siteProfile.configs.asDynamic()
        profile.configs = mutableListOf()
        for (config in configs) {
            profile.configs.add(SiteProfileConfig(
                    allowUrlWithQuery = config.allowUrlWithQuery,
                    pageBodyCssSelector = config.pageBodyCssSelector,
                    sitemapsOnly = config.sitemapsOnly,
                    url = config.url
            ))
        }
        showConfiguration()
    }
}

private lateinit var profileConfigs: HTMLOListElement
private lateinit var configTemplate: DocumentFragment

private fun showConfiguration() {
    profileConfigs.clear()
    for (config in profile.configs) {
        appendConfig(config)
    }
}

private fun appendConfig(config: SiteProfileConfig) {
    val profileConfig = configTemplate.firstElementChild?.cloneNode(true) as HTMLLIElement
    (profileConfig.querySelector("input[name=url]") as HTMLInputElement).value = config.url
    (profileConfig.querySelector("input[name=pageBodyCssSelector]") as HTMLInputElement).value = config.pageBodyCssSelector
    (profileConfig.querySelector("input[name=sitemapsOnly]") as HTMLInputElement).checked = config.sitemapsOnly
    (profileConfig.querySelector("input[name=allowUrlWithQuery]") as HTMLInputElement).checked = config.allowUrlWithQuery
    (profileConfig.querySelector("div[name=remove]") as HTMLDivElement)
            .addEventListener("click", {
                val isLastConfig = profile.configs.size < 2
                if (!isLastConfig) {
                    profile.configs.remove(config)
                    profileConfig.remove()
                }
            })
    profileConfigs.appendChild(profileConfig)
}

class SiteSearch {
    companion object {
        const val crawlerFinishedEvent = "sis.crawlerFinished"
    }
}

data class SiteProfileConfig(val url: String = "", val pageBodyCssSelector: String = "body", val sitemapsOnly: Boolean = false, val allowUrlWithQuery: Boolean = false)

data class SiteProfile(val id: String = "", val secret: String = "", var configs: MutableList<SiteProfileConfig> = mutableListOf(), val email: String = "")