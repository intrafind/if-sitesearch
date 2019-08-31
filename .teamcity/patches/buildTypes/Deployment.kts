package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.v2018_2.buildFeatures.sshAgent
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.failureConditions.BuildFailureOnText
import jetbrains.buildServer.configs.kotlin.v2018_2.failureConditions.failOnText
import jetbrains.buildServer.configs.kotlin.v2018_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2018_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a buildType with id = 'Deployment'
in the root project, and delete the patch script.
*/
create(DslContext.projectId, BuildType({
    id("Deployment")
    name = "Deployment"
    description = "Continuous incremental & automated release"

    params {
        param("env.DEV_SKIP_FLAG", "true")
        param("env.SCM_HASH", "%build.vcs.number%")
        param("env.BUILD_NUMBER", "%build.counter%")
    }

    vcs {
        root(DslContext.settingsRoot)

        cleanCheckout = true
    }

    steps {
        script {
            name = "Enable e-mail delivery"
            enabled = false
            scriptContent = """
                cp -r /root/docker-build-data/api-sitesearch/service/config service/
                chmod -R 755 service/config
            """.trimIndent()
        }
        script {
            name = "Build service.jar w/ Docker (using TeamCity Docker plugin)"
            scriptContent = """
                #SPRING_PROFILES_ACTIVE=oss
                ./gradlew clean includeKotlinJsRuntime build --info -x test
            """.trimIndent()
            dockerImage = "openjdk:13-alpine"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerPull = true
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
}))

