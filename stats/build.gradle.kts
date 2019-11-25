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

import com.moowork.gradle.node.npm.NpmTask
import com.moowork.gradle.node.task.NodeTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce

plugins {
    id("kotlin2js")
    id("com.github.node-gradle.node")
}

apply {
    plugin("kotlin-dce-js")
}

dependencies {
    val kotlinVersion = "1.3.60"

    implementation("org.jetbrains.kotlin:kotlin-stdlib-js:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-js:$kotlinVersion")
}

val artifactPath = "${project(":service").buildDir}/resources/main/static/app" // the only module specific property
project.file("$artifactPath/${project.name}").delete()

val compileKotlin2Js = tasks.named<Kotlin2JsCompile>("compileKotlin2Js") {
    kotlinOptions {
        sourceMap = true
        sourceMapEmbedSources = "always"
        moduleKind = "umd"
        noStdlib = true
    }
    doLast {
        copy {
            from(sourceSets.main.get().resources)
            into("$artifactPath/${project.name}/resources")
        }
        copy {
            from(sourceSets.main.get().output)
            into("$artifactPath/${project.name}")
        }
        copy {
            from(sourceSets.test.get().output)
            into("$artifactPath/${project.name}")
        }
    }
}

val compileTestKotlin2Js = tasks.named<Kotlin2JsCompile>("compileTestKotlin2Js") {
    kotlinOptions {
        sourceMap = true
        sourceMapEmbedSources = "always"
        moduleKind = "umd"
        noStdlib = true
    }
    doLast {
        copy {
            from(sourceSets.main.get().resources)
            into("$artifactPath/${project.name}/resources")
        }
        copy {
            from(sourceSets.main.get().output)
            into("$artifactPath/${project.name}")
        }
        copy {
            from(sourceSets.test.get().output)
            into("$artifactPath/${project.name}")
        }
    }
}

node {
    version = "12.13.0"
    download = true
}

tasks.test {
    dependsOn(runJest)
}

val installJest = task<NpmTask>("installJest") {
    setNpmCommand("install", "--save-dev", "jest")
}

val runDceKotlinJs = tasks.named<KotlinJsDce>("runDceKotlinJs") {
    keep("main.loop")
    dceOptions.devMode = false
    dceOptions.outputDirectory = "$artifactPath/${project.name}/min"
}

val runJest = tasks.register<NodeTask>("runJest") {
    dependsOn(compileTestKotlin2Js, populateNodeModules, installJest)
    script = file("node_modules/jest/bin/jest.js")
    addArgs((tasks.getByName("compileTestKotlin2Js", Kotlin2JsCompile::class)).outputFile)
}

val populateNodeModules = task<Copy>("populateNodeModules") {
    dependsOn(compileKotlin2Js)
    from((tasks.getByName("compileKotlin2Js", Kotlin2JsCompile::class)).destinationDir)

    configurations["testCompileClasspath"].forEach {
        from(zipTree(it.absolutePath).matching { include("*.js") })
    }

    into("$buildDir/node_modules")
}