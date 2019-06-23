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

import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce

plugins {
    id("kotlin2js") version "1.3.40"
}

apply {
    plugin("kotlin-dce-js")
}

dependencies {
    val kotlinVersion = "1.3.40"

    compile("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion")
}

tasks {
    val artifactPath = "${project(":service").projectDir}/src/main/resources/static/app" // the only module specific property
    project.file("$artifactPath/${project.name}").delete()

    "compileKotlin2Js"(Kotlin2JsCompile::class) {
        kotlinOptions {
            outputFile = "$artifactPath/${project.name}/${project.name}.js"
            sourceMap = true
            moduleKind = "umd"
            noStdlib = true
        }
        copy {
            from(sourceSets.main.get().resources)
            into("$artifactPath/${project.name}/resources")
        }
    }

    "runDceKotlinJs"(KotlinJsDce::class) {
        keep("main.loop")
        dceOptions.devMode = false
        dceOptions.outputDirectory = "$artifactPath/${project.name}/min"
    }
}