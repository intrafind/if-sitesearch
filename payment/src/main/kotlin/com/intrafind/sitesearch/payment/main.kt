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

package com.intrafind.sitesearch.payment

import kotlin.browser.document
import kotlin.browser.window

private fun init() {
    log("init")
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
}


