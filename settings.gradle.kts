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

rootProject.name = "if-sitesearch"

include(
        "service",

        "stats",
        "payment",
        "gadget",
        "dashboard"
)

pluginManagement {
    repositories {
        maven("https://dl.bintray.com/kotlin/kotlinx")
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id === "kotlin2js") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.60")
            }
            if (requested.id.id === "com.github.node-gradle.node") {
                useModule("com.github.node-gradle:gradle-node-plugin:2.2.0")
            }
        }
    }
}
