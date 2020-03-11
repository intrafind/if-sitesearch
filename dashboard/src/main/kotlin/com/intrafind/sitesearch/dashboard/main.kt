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

private lateinit var siteId: String
private lateinit var siteSecret: String
private val serviceUrl: String = window.location.origin

private lateinit var updateSiteProfile: HTMLButtonElement
private lateinit var pageCountContainer: HTMLDivElement
private lateinit var pageCount: HTMLParagraphElement
private val siteIdElement: HTMLDivElement = document.getElementById("siteId") as HTMLDivElement
private val siteSecretElement: HTMLDivElement = document.getElementById("siteSecret") as HTMLDivElement
private lateinit var recrawl: HTMLButtonElement
private lateinit var profile: SiteProfile
private lateinit var addSiteConfig: HTMLDivElement
private val email: HTMLInputElement = document.getElementById("email") as HTMLInputElement

private fun init() {
    updateSiteProfile = document.getElementById("updateSiteProfile") as HTMLButtonElement
    pageCountContainer = document.getElementById("pageCountContainer") as HTMLDivElement
    pageCount = document.getElementById("pageCount") as HTMLParagraphElement
    recrawl = document.getElementById("recrawl") as HTMLButtonElement
    profileConfigs = document.getElementById("profileConfigs") as HTMLOListElement
    configTemplate = (document.getElementById("profileConfig") as HTMLTemplateElement).content
    addSiteConfig = document.getElementById("addSiteConfig") as HTMLDivElement

    applyQueryParameters()
}

@JsName("addSiteProfileConfig")
private fun addSiteProfileConfig() {
    profile.configs.add(SiteProfileConfig(url = "https://example.com/to-change-to-your-site"))
    showConfiguration()
}

private fun applyQueryParameters() {
    val siteIdIndex = window.location.search.indexOf("siteId=") + 7
    siteId = window.location.search.substring(siteIdIndex, siteIdIndex + 36)
    val siteSecretIndex = window.location.search.indexOf("siteSecret=") + 11
    siteSecret = window.location.search.substring(siteSecretIndex, siteSecretIndex + 36)
    fetchProfile()

    siteIdElement.textContent = siteId
    siteSecretElement.textContent = siteSecret
}

@JsName("updateSiteProfile")
private fun updateSiteProfile() {
    val req = XMLHttpRequest()
    req.open("PUT", "$serviceUrl/sites/$siteId/profile?siteSecret=$siteSecret")
    profile.configs.clear()
    for (profileConfig in profileConfigs.querySelectorAll("li").asList()) {
        profileConfig as HTMLLIElement
        val url = (profileConfig.querySelector("input[name=url]") as HTMLInputElement).value
        val pageBodyCssSelector = (profileConfig.querySelector("input[name=pageBodyCssSelector]") as HTMLInputElement).value
        val sitemapsOnly = (profileConfig.querySelector("input[name=sitemapsOnly]") as HTMLInputElement).checked
        val allowUrlWithQuery = (profileConfig.querySelector("input[name=allowUrlWithQuery]") as HTMLInputElement).checked

        profile.email = email.value
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
        fetchProfile()
    }
}

external fun encodeURIComponent(str: String): String

@JsName("recrawl")
private fun recrawl() {
    recrawl.disabled = true
    recrawl.textContent = "Crawling... please give us a minute or two."

    val req = XMLHttpRequest()
    req.open("POST", "$serviceUrl/sites/$siteId/recrawl?siteSecret=$siteSecret&clearIndex=false")
    req.send()

    req.onload = {
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
        profile = JSON.parse(req.responseText)
        val configs = profile.configs.asDynamic()
        profile.configs = mutableListOf()
        for (config: SiteProfileConfig in configs) {
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
    email.value = profile.email

    profileConfigs.clear()
    for (config: SiteProfileConfig in profile.configs) {
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
                removeConfig(config, profileConfig)
            })
    profileConfigs.appendChild(profileConfig)
}

private fun removeConfig(config: SiteProfileConfig, profileConfig: HTMLLIElement) {
    val isLastConfig = profile.configs.size < 2
    if (!isLastConfig) {
        profile.configs.remove(config)
        profileConfig.remove()
    }
}

class SiteSearch {
    companion object {
        const val crawlerFinishedEvent = "sis.crawlerFinished"
    }
}

data class SiteProfileConfig(val url: String = "", val pageBodyCssSelector: String = "body", val sitemapsOnly: Boolean = false, val allowUrlWithQuery: Boolean = false)

data class SiteProfile(val id: String = "", var secret: String = "", var configs: MutableList<SiteProfileConfig> = mutableListOf(), var email: String = "")