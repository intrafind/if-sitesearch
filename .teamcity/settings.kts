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

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2018.2"

project {
    description = "Software as a Service"

    vcsRoot(ProdRelease)
    vcsRoot(IfSitesearchRouter)
    vcsRoot(Recrawl_1)
    vcsRoot(HttpsGithubComLoxalIfSitesearchRefsHeadsMaster1)
    vcsRoot(Daily)
    vcsRoot(SmokeApiHealthChecksIfSitesearch)

    buildType(Recrawl)
    buildType(LoadTest)
    buildType(DailySnapshotsAndCleanups)
    buildType(BGRelease)
    buildType(Build)
    buildType(SmokeTest)
    buildType(ReleaseRouter)

    params {
        param("env.TF_VAR_docker_password", "%env.SERVICE_SECRET%")
    }

    features {
        feature {
            id = "PROJECT_EXT_1"
            type = "ReportTab"
            param("startPage", "index.html")
            param("title", "Test Report Tab")
            param("type", "BuildReportTab")
        }
        dockerRegistry {
            id = "PROJECT_EXT_10"
            name = "Site Search Docker Registry"
            url = "https://docker-registry.sitesearch.cloud"
            userName = "sitesearch"
            password = "credentialsJSON:bed82269-69df-4b8a-9435-959656fcc281"
        }
        feature {
            id = "PROJECT_EXT_11"
            type = "IssueTracker"
            param("secure:password", "")
            param("name", "intrafind/if-sitesearch")
            param("pattern", """#(\d+)""")
            param("authType", "anonymous")
            param("repository", "https://github.com/intrafind/if-sitesearch")
            param("type", "GithubIssues")
            param("secure:accessToken", "")
            param("username", "")
        }
        feature {
            id = "PROJECT_EXT_2"
            type = "ReportTab"
            param("buildTypeId", "IntraFind_Oss_LoadTest")
            param("startPage", "index.html")
            param("revisionRuleName", "lastSuccessful")
            param("revisionRuleRevision", "latest.lastSuccessful")
            param("title", "Report Tab Title")
            param("type", "ProjectReportTab")
        }
        feature {
            id = "PROJECT_EXT_5"
            type = "JetBrains.SharedResources"
            param("quota", "-1")
            param("name", "testresource")
            param("type", "quoted")
        }
        feature {
            id = "PROJECT_EXT_6"
            type = "project-graphs"
            param("series", """
                [
                  {
                    "type": "valueType",
                    "title": "Build Duration (excluding Checkout Time)",
                    "sourceBuildTypeId": "IntraFind_Oss_SmokeTest",
                    "key": "BuildDurationNetTime"
                  },
                  {
                    "type": "valueType",
                    "title": "Success Rate",
                    "sourceBuildTypeId": "IntraFind_Oss_SmokeTest",
                    "key": "SuccessRate"
                  }
                ]
            """.trimIndent())
            param("format", "text")
            param("title", "API Health")
            param("seriesTitle", "Serie")
        }
        feature {
            id = "PROJECT_EXT_7"
            type = "project-graphs"
            param("series", """
                [
                  {
                    "type": "valueType",
                    "title": "Build Duration (excluding Checkout Time)",
                    "sourceBuildTypeId": "IntraFind_Oss_LoadTest",
                    "key": "BuildDurationNetTime"
                  }
                ]
            """.trimIndent())
            param("format", "text")
            param("title", "Throughput Tests")
            param("seriesTitle", "Serie")
        }
    }
}

object BGRelease : BuildType({
    name = "PROD Release"
    description = "Leveraging B/G zero-downtime deployment"

    params {
        param("env.DEV_SKIP_FLAG", "true")
        param("env.SCM_HASH", "%build.vcs.number%")
        param("env.BUILD_NUMBER", "%build.counter%")
    }

    vcs {
        root(ProdRelease)
    }

    steps {
        script {
            scriptContent = "sh switch-release.sh"
        }
    }

    triggers {
        vcs {
            enabled = false
            triggerRules = "-:**SR"
            branchFilter = ""
            groupCheckinsByCommitter = true
        }
    }

    failureConditions {
        errorMessage = true
    }
})

object Build : BuildType({
    name = "DEV Release"
    description = "Continuous incremental & automated release"

    params {
        param("env.DEV_SKIP_FLAG", "true")
        param("env.SCM_HASH", "%build.vcs.number%")
        param("env.BUILD_NUMBER", "%build.counter%")
    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "Enable e-mail delivery"
            scriptContent = """
                whoami
                cp -r /root/docker-build-data/api-sitesearch/service/config service/
                chmod -R 755 service/config
            """.trimIndent()
        }
        script {
            name = "Build service.jar w/ Docker (using TeamCity Docker plugin)"
            scriptContent = "./gradlew clean build --info"
//            dockerImage = "openjdk:11-jre-slim"
            dockerImage = "openjdk:12-jdk-alpine"
            dockerRunParameters = "-v /root/.gradle:/root/.gradle"
        }
        script {
            name = "Build & Run Container"
            scriptContent = "sh start-sitesearch.sh"
        }
    }

    triggers {
        vcs {
            triggerRules = """
                -:comment=.*SR${'$'}:**
                -:docs/**
                -:**/sitesearch/jmh/**
                -:docker-router/**
                -:opt/**
                -:bootstrap/**
                -:docker-router/frontpage/**
            """.trimIndent()
            branchFilter = ""
            groupCheckinsByCommitter = true
        }
    }

    failureConditions {
        executionTimeoutMin = 60
        errorMessage = true
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "ERROR"
            reverse = false
            stopBuildOnFailure = true
        }
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "AssertionError"
            failureMessage = "AssertionError"
            reverse = false
            stopBuildOnFailure = true
        }
    }

    features {
        sshAgent {
            teamcitySshKey = "dev"
        }
        perfmon {
            enabled = false
        }
    }
})

object DailySnapshotsAndCleanups : BuildType({
    name = "Daily Snapshots and Cleanups"
    description = "daily snapshots and cleanups on gce ..."

    vcs {
        root(Daily)
    }

    steps {
        script {
            scriptContent = "sh ./ops/snapshot-disk-and-cleanup.sh"
        }
    }

    triggers {
        schedule {
            schedulingPolicy = daily {
                hour = 3
            }
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false
            enableQueueOptimization = false
        }
    }

    failureConditions {
        errorMessage = true
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "Bad substitution"
            reverse = false
        }
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "not found"
            reverse = false
        }
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "invalid option"
            reverse = false
        }
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "ERROR"
            reverse = false
        }
    }
})

object LoadTest : BuildType({
    name = "Throughput & Load Test"

    artifactRules = "service/build/reports/jmh/results.json"
    detectHangingBuilds = false

    vcs {
        root(HttpsGithubComLoxalIfSitesearchRefsHeadsMaster1)
    }

    steps {
        script {
            scriptContent = "sh load-test.sh"
            dockerImage = "openjdk:12-jdk-alpine"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "-v /root/.gradle:/root/.gradle"
        }
    }

    triggers {
        schedule {
            schedulingPolicy = daily {
                hour = 23
            }
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    failureConditions {
        errorMessage = true
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "AssertionError"
            reverse = false
            stopBuildOnFailure = true
        }
    }

    features {
        perfmon {
        }
    }
})

object Recrawl : BuildType({
    name = "Recrawl"

    vcs {
        root(Recrawl_1)
    }

    steps {
        script {
            scriptContent = "sh ./ops/recrawl.sh"
        }
    }

    triggers {
        schedule {
            schedulingPolicy = cron {
                hours = "4"
            }
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false
            param("hour", "3")
        }
    }

    failureConditions {
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "CRAWLING_SUCCESS"
            reverse = true
        }
    }
})

object ReleaseRouter : BuildType({
    name = "Router Release"

    vcs {
        root(IfSitesearchRouter)
    }

    steps {
        script {
            scriptContent = """
                #ssh alexander_orlov@dev "cd ${'$'}PWD; sh ./release-router.sh"
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            triggerRules = "+:**/frontpage"
            branchFilter = ""
        }
    }

    failureConditions {
        executionTimeoutMin = 5
        errorMessage = true
    }
})

object SmokeTest : BuildType({
    name = "API Health & Availability"
    description = "Smoke Tests"

    vcs {
        root(SmokeApiHealthChecksIfSitesearch)
    }

    steps {
        script {
            scriptContent = "sh ./ci/smoke-test.sh"
            dockerImage = "openjdk:12-jdk-alpine"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerRunParameters = "-v /root/.gradle:/root/.gradle"
        }
    }

    triggers {
        schedule {
            schedulingPolicy = cron {
                minutes = "0/1"
            }
            branchFilter = ""
            triggerBuild = always()
            withPendingChangesOnly = false
        }
    }

    failureConditions {
        executionTimeoutMin = 5
        errorMessage = true
        failOnText {
            conditionType = BuildFailureOnText.ConditionType.CONTAINS
            pattern = "BUILD FAILED"
            failureMessage = "Gradle test execution failed"
            reverse = false
            stopBuildOnFailure = true
        }
    }
})

object Daily : GitVcsRoot({
    name = "daily"
    url = "https://github.com/intrafind/if-sitesearch.git"
    authMethod = password {
        userName = "loxal"
        password = "credentialsJSON:91f52c32-83d5-465b-bd17-262b0a37cd3f"
    }
})

object ProdRelease : GitVcsRoot({
    name = "prod-release"
    url = "https://github.com/intrafind/if-sitesearch"
    authMethod = password {
        userName = "loxal"
        password = "credentialsJSON:91f52c32-83d5-465b-bd17-262b0a37cd3f"
    }
})

object HttpsGithubComLoxalIfSitesearchRefsHeadsMaster1 : GitVcsRoot({
    name = "load-test"
    url = "https://github.com/intrafind/if-sitesearch"
    authMethod = password {
        userName = "loxal"
        password = "credentialsJSON:91f52c32-83d5-465b-bd17-262b0a37cd3f"
    }
})

object IfSitesearchRouter : GitVcsRoot({
    name = "if-sitesearch-router"
    url = "https://github.com/intrafind/if-sitesearch"
    authMethod = password {
        userName = "loxal"
        password = "credentialsJSON:91f52c32-83d5-465b-bd17-262b0a37cd3f"
    }
})

object SmokeApiHealthChecksIfSitesearch : GitVcsRoot({
    name = "smoke-api-health-check"
    url = "https://github.com/intrafind/if-sitesearch"
    authMethod = password {
        userName = "loxal"
        password = "credentialsJSON:91f52c32-83d5-465b-bd17-262b0a37cd3f"
    }
})

object Recrawl_1 : GitVcsRoot({
    id("Recrawl")
    name = "Recrawl"
    url = "https://github.com/intrafind/if-sitesearch"
    authMethod = password {
        userName = "loxal"
        password = "credentialsJSON:91f52c32-83d5-465b-bd17-262b0a37cd3f"
    }
})
