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

package com.intrafind.sitesearch.gadget

import com.intrafind.sitesearch.gadget.SiteSearch.Companion.serviceUrl
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.addClass
import kotlin.dom.removeClass

suspend fun main(args: Array<String>) {
    window.addEventListener("DOMContentLoaded", {
        init()
    })
}

private var siteId: String = ""
private var siteSecret: String = ""
private var websiteUrl: String = ""

fun triggerFirstUsageOwnership() {
    createSite()
}

private fun createSite() {
    val xhr = XMLHttpRequest()
    xhr.open("POST", "$serviceUrl/sites")
    xhr.onload = {
        siteId = JSON.parse<dynamic>(xhr.responseText).siteId as String
        siteSecret = JSON.parse<dynamic>(xhr.responseText).siteSecret as String
        siteIdContainer.textContent = siteId
        siteSecretContainer.textContent = siteSecret
        overrideSite(siteId)
        enableSearch(waitWhileCrawlerIsRunningMsg) // required for IE11 because it belongs in a museum
        document.dispatchEvent(Event("sis.triggerFirstUsageOwnershipEvent"))
    }
    xhr.setRequestHeader("content-type", "application/json")
    xhr.send(JSON.stringify(SiteProfileCreation(setOf(SiteProfileConfig(url.value, encodeURIComponent(cssSelector.value), sitemapsOnly.checked)), email.value)))
}

@JsName("overrideSite")
fun overrideSite(siteId: String) {
    document.cookie = "override-site = $siteId; domain = .${sisDomain}; path = /"
    document.cookie = "sis.websiteUrl = ${url.value}"
}

private val sisDomain = "sitesearch.cloud"
private lateinit var captchaToken: String
private lateinit var termsAccepted: HTMLInputElement
private lateinit var integrationCode: HTMLTextAreaElement
private lateinit var siteIdContainer: HTMLDivElement
private lateinit var siteIdBox: HTMLDivElement
private lateinit var siteSecretContainer: HTMLDivElement
private lateinit var siteSecretBox: HTMLDivElement
private lateinit var recaptcha: HTMLDivElement
private lateinit var emailContainer: HTMLDivElement
private lateinit var websiteUrlContainer: HTMLDivElement
private lateinit var siteSearchSetupUrl: HTMLDivElement
private lateinit var triggerButton: HTMLButtonElement
private lateinit var url: HTMLInputElement
private lateinit var email: HTMLInputElement
private lateinit var sitemapsOnly: HTMLInputElement
private lateinit var sitemapContainer: HTMLDivElement
private lateinit var cssSelector: HTMLInputElement
private lateinit var cssSelectorContainer: HTMLDivElement
private var isValidSetup: Boolean = false
private var isTermsAccepted: Boolean = false
private var isCaptchaSolved: Boolean = false

private fun init() {
    termsAccepted = document.getElementById("termsAccepted") as HTMLInputElement
    cssSelectorContainer = document.getElementById("cssSelectorContainer") as HTMLDivElement
    cssSelector = document.getElementById("cssSelector") as HTMLInputElement
    sitemapContainer = document.getElementById("sitemapContainer") as HTMLDivElement
    sitemapsOnly = document.getElementById("sitemapsOnly") as HTMLInputElement
    email = document.getElementById("email") as HTMLInputElement
    url = document.getElementById("url") as HTMLInputElement
    integrationCode = document.getElementById("integration-code") as HTMLTextAreaElement
    siteIdContainer = document.getElementById("siteId") as HTMLDivElement
    siteSecretContainer = document.getElementById("siteSecret") as HTMLDivElement
    siteIdBox = document.getElementById("siteId-container") as HTMLDivElement
    recaptcha = document.getElementById("recaptcha") as HTMLDivElement
    siteSecretBox = document.getElementById("siteSecret-container") as HTMLDivElement
    emailContainer = document.getElementById("email-container") as HTMLDivElement
    websiteUrlContainer = document.getElementById("websiteUrl-container") as HTMLDivElement
    siteSearchSetupUrl = document.getElementById("siteSearchSetupUrl") as HTMLDivElement
    triggerButton = document.getElementById("index") as HTMLButtonElement
    val enterpriseSearchbar = document.getElementById("sitesearch-searchbar") as HTMLDivElement
    integrationCode.value = enterpriseSearchbar.outerHTML

    document.addEventListener("sis.crawlerFinishedEvent", {
        showEnableSearchInfo()
    })

    termsAccepted.addEventListener("change", {
        requireTermsAndConditions()
    })

    applyAnalytics()
    enableProactiveValidation()
//    validateDomain()  // fix api.muctool.de first

    document.addEventListener("sis.triggerFirstUsageOwnershipEvent", {
        enableSearch(waitWhileCrawlerIsRunningMsg)
    })

    applyQueryOverrides()
}

private fun showEnableSearchInfo() {
    triggerButton.textContent = "Enable Search"
    triggerButton.disabled = false
    (document.getElementById("ifs-sb-searchfield") as HTMLInputElement).placeholder = "$crawlerPageCount pages from ${url.value} have been crawled."
}

val waitWhileCrawlerIsRunningMsg = "Crawler is running... please give us just a minute or two."
private fun enableSearch(waitWhileCrawlerIsRunningMsg: String) {
    startCrawler()
    triggerButton.textContent = waitWhileCrawlerIsRunningMsg
    triggerButton.disabled = true
    siteSearchSetupUrl.innerHTML = "<strong>Copy this search setup URL to resume evaluation later:</strong> " +
            "<a href='https://${sisDomain}/getting-started?siteId=$siteId&siteSecret=$siteSecret&url=${url.value}'>" +
            "https://${sisDomain}/getting-started?siteId=$siteId&siteSecret=$siteSecret&url=${url.value}" +
            "</a>"
    (document.getElementById("ifs-sb-searchfield") as HTMLInputElement).placeholder = waitWhileCrawlerIsRunningMsg
    insertSiteIdIntoIntegrationCode()

    trackEnabledSearch()
}

private fun trackEnabledSearch() {
    js("ga('send', {" +
            "hitType: 'event'," +
            "eventCategory: 'GSG'," +
            "eventAction: 'enabledSearch'," +
            "eventLabel: document.getElementById('siteId').textContent + ' - ' + window.document.referrer + ' - ' + document.getElementById('url').value" +
            "})")
}

private fun applyAnalytics() {
    sitemapsOnly.addEventListener("change", {
        js("ga('send', {" +
                "hitType: 'event'," +
                "eventCategory: 'GSG'," +
                "eventAction: 'sitemapsChecked'," +
                "eventLabel: document.getElementById('sitemapsOnly').checked" +
                "})")
    })

    cssSelector.addEventListener("change", {
        js("ga('send', {" +
                "hitType: 'event'," +
                "eventCategory: 'GSG'," +
                "eventAction: 'cssSelectorChanged'," +
                "eventLabel: document.getElementById('cssSelector').value" +
                "})")
    })

    email.addEventListener("change", {
        js("ga('send', {" +
                "hitType: 'event'," +
                "eventCategory: 'GSG'," +
                "eventAction: 'emailChanged'," +
                "eventLabel: document.getElementById('email').value" +
                "})")
    })

    url.addEventListener("change", {
        js("ga('send', {" +
                "hitType: 'event'," +
                "eventCategory: 'GSG'," +
                "eventAction: 'urlChanged'," +
                "eventLabel: document.getElementById('url').value" +
                "})")
    })

    url.addEventListener("click", {
        js("ga('send', {" +
                "hitType: 'event'," +
                "eventCategory: 'GSG'," +
                "eventAction: 'urlClicked'," +
                "eventLabel: navigator.userAgent" +
                "})")
    })
}

private fun enableProactiveValidation() {
    url.addEventListener("blur", {
        if (!(url.value.startsWith("http") || url.value.startsWith("https"))) {
            url.value = "https://${url.value}"
        }
        // validateDomain() // fix api.muctool.de first
    })

    url.addEventListener("change", {
        // validateDomain() // fix api.muctool.de first
    })

    // do not set keypress listeners on both `cssSelector` & `url` until Shadow DOM is not used instead of `document.createElement("html")`

    cssSelector.addEventListener("blur", {
        validateCssSelector()
    })

    cssSelector.addEventListener("change", {
        validateCssSelector()
    })
}

private fun validateCssSelector() {
    val pageShadow = document.createElement("html") as HTMLElement
    pageShadow.innerHTML = pageBody
    if (pageShadow.querySelector(cssSelector.value) == null) {
        classifyCssSelectorAsValid(false)
    } else {
        classifyCssSelectorAsValid(true)
    }
}

lateinit var pageBody: String
private fun validateDomain() {
    val xhr = XMLHttpRequest()
    xhr.open("GET", "https://api.muctool.de/curl?url=${url.value}")
    xhr.send()
    xhr.onload = {
        if (allowedToCrawl(xhr)) {
            pageBody = JSON.parse<dynamic>(xhr.responseText).body as String
            classifyUrlAsValid(true)
            validateCssSelector()
        } else {
            classifyUrlAsValid(false)
        }
    }
}

private fun allowedToCrawl(xhr: XMLHttpRequest): Boolean {
    if (xhr.status.equals(200)) {
        val statusCode = JSON.parse<dynamic>(xhr.responseText).statusCode as Short
        return statusCode.equals(200) || statusCode.equals(302) // 301 is not supported by the crawler
    }
    return false
}

private fun classifyUrlAsValid(isValid: Boolean) {
    isValidSetup = if (isValid) {
        validateField(websiteUrlContainer, true)
        true
    } else {
        validateField(websiteUrlContainer, false)
        false
    }
}

private fun classifyCssSelectorAsValid(isValid: Boolean) {
    if (isValid) {
        validateField(cssSelectorContainer, true)
    } else {
        validateField(cssSelectorContainer, false)
    }
}

private fun validateField(container: HTMLElement, isValid: Boolean) {
    if (isValid) {
        container.removeClass("invalidField")
    } else {
        container.addClass("invalidField")
    }
}

@JsName("verifyCallback")
private fun verifyCallback(token: String) {
    if (!token.isEmpty()) {
        triggerButton.disabled = false
        isCaptchaSolved = true
        captchaToken = token
    } else {
        // TODO show CAPTCHA error
    }
}

private fun requireTermsAndConditions() {
    isTermsAccepted = termsAccepted.checked
    if (isTermsAccepted) {
        termsAccepted.disabled = true
        js("grecaptcha.execute();")
    } else {
        termsAccepted.style.background = "#911" // TODO Jochen add some invalidity communicating style here instead and additionally show a message like "Accepting T&C is required in order to proceed"
    }
}

@JsName("preserveSearchSetup")
private fun preserveSearchSetup() {
    document.execCommand("copy")
}

@JsName("applyQueryOverrides")
private fun applyQueryOverrides() {
    siteId = when {
        window.location.search.indexOf("siteId=") != -1 -> window.location.search.substring(window.location.search.indexOf("siteId=") + 7, 44)
        document.cookie.indexOf("override-site") != -1 ->
            document.cookie.substring(document.cookie.indexOf("override-site") + 14, document.cookie.indexOf("override-site") + 14 + 36) // relies on cookie-setting code in embedding iframe container
        else -> ""
    }
    val longExtraction = document.cookie.substring(document.cookie.indexOf("sis.websiteUrl") + 15)
            .substring(0, document.cookie.substring(document.cookie.indexOf("sis.websiteUrl") + 15).indexOf(";"))
    val shortExtraction = document.cookie.substring(document.cookie.indexOf("sis.websiteUrl") + 15)
    websiteUrl = when {
        longExtraction.isNotEmpty() -> longExtraction // relies on cookie-setting code in embedding iframe container
        shortExtraction.isNotEmpty() -> shortExtraction // relies on cookie-setting code in embedding iframe container
        else -> "Site Validation Warning" // just a pseudo message to avoid blank field
    }
    if (siteId.isNotEmpty()) {
        url.value = websiteUrl
        url.readOnly = true
        overrideSite(siteId)
        insertSiteIdIntoIntegrationCode()
        triggerButton.style.display = "none"
        recaptcha.style.display = "none"
        siteSecretBox.style.display = "none"
        emailContainer.style.display = "none"
        siteIdBox.style.display = "none"
        sitemapContainer.style.display = "none"
        cssSelectorContainer.style.display = "none"
        termsAccepted.checked = true
        termsAccepted.disabled = true
    }
}

private fun insertSiteIdIntoIntegrationCode() {
    integrationCode.value = integrationCode.value.replace("siteId: \".+".toRegex(), "siteId: \"$siteId\"")
}

external fun encodeURIComponent(str: String): String
private var crawlerPageCount: Int = 0
fun startCrawler() {
    val xhr = XMLHttpRequest()
    xhr.open("POST", "$serviceUrl/sites/$siteId/crawl?siteSecret=$siteSecret&url=${encodeURIComponent(url.value)}&token=$captchaToken&email=${email.value}&sitemapsOnly=${sitemapsOnly.checked}&pageBodyCssSelector=${encodeURIComponent(cssSelector.value)}")
    xhr.onload = {
        if (xhr.status.equals(200)) {
            crawlerPageCount = JSON.parse<dynamic>(xhr.responseText).pageCount as Int
            showEnableSearchInfo() // required for IE11 because it belongs in a museum
            document.dispatchEvent(Event("sis.crawlerFinishedEvent"))
        } else {
            console.error("startCrawler failed")
        }
    }
    xhr.send()
}

class SiteSearch {
    companion object {
        val captchaSiteKey = "6LflVEQUAAAAANVEkwc63uQX96feH1H_6jDU-Bn5"
        internal val serviceUrl: String = window.location.origin
    }
}

data class SiteProfileConfig(val url: String, val pageBodyCssSelector: String = "body", val sitemapsOnly: Boolean = false)

data class SiteProfileCreation(val configs: Set<SiteProfileConfig> = emptySet(), val email: String = "")
