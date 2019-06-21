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

package com.intrafind.sitesearch.finder

import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDListElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.OPEN
import org.w3c.dom.ShadowRootInit
import org.w3c.dom.ShadowRootMode
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.xhr.XMLHttpRequest
import kotlin.browser.document
import kotlin.browser.window
import kotlin.dom.clear

private data class Finding(
        val title: String = "",
        val body: String = "",
        val url: String = "",
        val urlRaw: String = ""
)

private data class Findings(val query: String, val results: Array<Finding> = arrayOf())

private val debugView = document.createElement("dl") as HTMLDListElement
private val selfTestTrigger = document.createElement("button") as HTMLButtonElement

private fun selfTest() {
    debugView.clear()
    validateServiceCall("search")
    validateServiceCall("autocomplete")
}

private fun validateServiceCall(apiEndpoint: String) {
    val xhr = XMLHttpRequest()
    xhr.open("GET", "$finderService/$siteId/$apiEndpoint?query=sites")
    xhr.onload = {
        val dt = document.createElement("dt") as HTMLElement
        debugView.appendChild(dt)
        val dd = document.createElement("dd")
        if (xhr.status.equals(200) && JSON.parse<dynamic>(xhr.responseText).results.length > 0) {
            dt.style.color = "#191"
            dt.textContent = "PASSED: $apiEndpoint"
        } else {
            dt.style.color = "#911"
            dt.textContent = "FAILED: $apiEndpoint"
        }
        dd.textContent = "Status: ${xhr.status} | Response Length: ${xhr.responseText.length}"
        debugView.appendChild(dd)
    }
    xhr.send()
}

fun init() {
    log("init")
    buildPageFinder()
    injectPageFinderIntoWebsite()

    if (isDebugView) {
        selfTestTrigger.addEventListener("click", { selfTest() })
        selfTestTrigger.innerText = "Self Test"
        selfTestTrigger.style.display = "block"
        pageFinderContainer.appendChild(selfTestTrigger)
        pageFinderContainer.appendChild(debugView)
    }
}

private fun injectPageFinderIntoWebsite() {
    val hiddenBehindFlag: String? = pageFinderInit.getAttribute("data-hidden-behind-query-flag")

    if (hiddenBehindFlag.isNullOrBlank()) {
        showPageFinder()
    } else if (window.location.search.contains(hiddenBehindFlag)) {
        showPageFinder()
    }
}

private fun showPageFinder() {
    val style = document.createElement("style") as HTMLStyleElement
    style.innerText = ".if-teaser-highlight {font-weight: bold;}"
    finder.appendChild(style)

    findingsContainer.style.cssText =
            "box-shadow: 0 2px 2px 0 gray, 0 1px 4px 0 gray, 0 3px 1px -2px gray; " +
                    "letter-spacing: .02em; " +
                    "min-height: 50px; max-height: 400px;" +
                    "margin-top: 2px; overflow-y: auto; overflow-x: hidden;" +
                    "width: ${finder.getAttribute("width")?.toInt()!! - 9}px;" +
                    "padding-left: 8px;"

    findingsContainer.style.background = "#fff"
    findingsContainer.style.zIndex = "99"
    findingsContainer.style.position = "relative"

    pageFinderContainer.id = "sitesearch-page-finder"
    encapsulateAsComponent()

    pageFinderContainer.appendChild(finder)
    pageFinderContainer.appendChild(findingsContainer)
    findingsContainer.style.display = "none"
}

private val pageFinderComponent = document.createElement("page-finder") as HTMLElement
private fun encapsulateAsComponent() {
    val parentContainerId: String? = pageFinderInit.getAttribute("data-append-as-child-to")
    val parent: Element? =
            if (parentContainerId.isNullOrBlank()) {
                pageFinderInit.parentElement
            } else {
                val parentContainer = document.querySelector(parentContainerId)
                log("Inserted into: ${parentContainer?.outerHTML}")
                parentContainer ?: pageFinderInit.parentElement
            }

    window.onload = {
        resetInheritedStyleProperties(pageFinderComponent)
        pageFinderComponent.appendChild(pageFinderContainer)
        parent?.attachShadow(ShadowRootInit(ShadowRootMode.OPEN))
                ?.appendChild(pageFinderComponent)
        document.dispatchEvent(Event("page-finder-loaded"))
    }
}

//private val pageFinderContainer = document.createElement("template") as HTMLTemplateElement
private val pageFinderContainer = document.createElement("div") as HTMLDivElement
private val findingsContainer = document.createElement("dl") as HTMLDListElement
//private val sisDomain = "sitesearch.cloud"
//private val finderService = "https://api.${sisDomain}/sites"
private val finderService = "${window.location.origin}/sites"
private val pageFinderInit = document.currentScript as HTMLScriptElement
private val siteId = pageFinderInit.getAttribute("data-siteId")
private val finderEndpoint = "search"
private val autocompleteEndpoint = "autocomplete"
private val finder = document.createElement("input") as HTMLInputElement
private fun buildPageFinder() {
    val finderStyle = pageFinderInit.getAttribute("data-search-style")
    finder.type = "search"
    finder.spellcheck = true
    finder.title = "Finder"
    finder.placeholder = "Search..."
    finder.style.cssText =
            if (finderStyle.isNullOrBlank())
                "width: 500px; font-size: 2em; text-indent: .5em;"
            else
                finderStyle
    finder.width = finder.style.width.substringBeforeLast("px").toInt()
}

private val isDebugView = window.location.search.contains("debug-view")
private fun log(msg: Any?) {
    if (isDebugView) {
        println(msg)
    }
}

private fun main() {
    init()
    document.addEventListener("DOMContentLoaded", {
        log("DOMContentLoaded")
    })

    log("Query: ${finder.value}")
    finder.addEventListener("change", {
        log("change")
        log(finder.value)
    })
    finder.addEventListener("keydown", { event: Event ->
        debugView.remove()
        val keyboardEvent = event as KeyboardEvent
        if (keyboardEvent.key.equals("Enter")) {
            if (finder.value.isBlank()) {
                findingsContainer.style.display = "none"
            } else {
                search()
            }
        } else {
            autocomplete()
        }
    })
}

private fun autocomplete() {
    val xhr = XMLHttpRequest()
    xhr.open("GET", "$finderService/$siteId/$autocompleteEndpoint?query=${finder.value}")
    xhr.onload = {
        findingsContainer.clear()
        if (xhr.status.equals(200)) {
            val suggestions = JSON.parse<dynamic>(xhr.responseText)
            for (suggestion: String in suggestions.results) {
                val suggestionEntry = document.createElement("dd") as HTMLElement
                suggestionEntry.style.borderBottom = "1px dotted #000"
                suggestionEntry.style.marginLeft = "0"
                suggestionEntry.style.padding = "0.2em"
                suggestionEntry.style.fontSize = "1.5em"
                suggestionEntry.innerText = suggestion
                suggestionEntry.onclick = {
                    log(suggestionEntry.innerText)
                    finder.value = suggestionEntry.innerText
                    search()
                }
                findingsContainer.appendChild(suggestionEntry)
            }
            if (suggestions.results.length > 0) {
                findingsContainer.style.display = "block"
            }
        }
    }
    xhr.onerror = {
        log("Error: ${xhr.response}")
    }
    xhr.send()
}

private fun search() {
    if (finder.value.equals("/selftest")) {
        findingsContainer.remove()
        finder.parentNode?.appendChild(debugView)
        selfTest()
        return
    }
    val xhr = XMLHttpRequest()
    xhr.open("GET", "$finderService/$siteId/$finderEndpoint?query=${finder.value}")
    xhr.onload = {
        findingsContainer.clear()
        if (xhr.status.equals(200)) {
            val findings = JSON.parse<Findings>(xhr.responseText)
            findings.results.forEach { finding ->
                val dtTitle = document.createElement("dt") as HTMLElement
                dtTitle.innerHTML = finding.title
                dtTitle.setAttribute("style", "margin-bottom: .5em; font-style: italic;")
                findingsContainer.appendChild(dtTitle)
                val ddBody = document.createElement("dd") as HTMLElement
                ddBody.innerHTML = finding.body
                ddBody.setAttribute("style", "margin-bottom: .5em")
                findingsContainer.appendChild(ddBody)
                val ddUrl = document.createElement("dd") as HTMLElement
                ddUrl.innerHTML = "<a style=\"text-decoration:none\" href=\"${finding.urlRaw}\">${finding.url}</a>"
                ddUrl.setAttribute("style", "margin-bottom: 1em;")
                findingsContainer.appendChild(ddUrl)
            }
            if (findings.results.isEmpty()) {
                val dtTitle = document.createElement("dt") as HTMLElement
                dtTitle.innerHTML = "..."
                dtTitle.setAttribute("style", "margin-bottom: .5em;" +
                        "text-align: center;" +
                        "font-size: 2em;"
                )
                findingsContainer.appendChild(dtTitle)
            }
            findingsContainer.style.display = "block"
        }
    }
    xhr.onerror = {
        log("Error: ${xhr.response}")
    }
    xhr.send()
}

private fun resetInheritedStyleProperties(dirtyElement: HTMLElement) { // very expensive operation!!!
    window.getComputedStyle(dirtyElement).cssText.split(": ; ")
            .forEach { cssProperty -> dirtyElement.style.setProperty(cssProperty, "initial") }
}