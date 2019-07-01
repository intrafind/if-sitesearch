package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the buildType with id = 'Build'
accordingly, and delete the patch script.
*/
changeBuildType(RelativeId("Build")) {
    expectSteps {
        script {
            name = "Enable e-mail delivery"
            scriptContent = """
                cp -r /root/docker-build-data/api-sitesearch/service/config service/
                chmod -R 755 service/config
            """.trimIndent()
        }
        script {
            name = "Build service.jar w/ Docker (using TeamCity Docker plugin)"
            scriptContent = "./gradlew clean build --info"
            dockerImage = "openjdk:11-jre-slim"
            dockerRunParameters = "-v /root/.gradle:/root/.gradle"
        }
        script {
            name = "Build & Run Container"
            scriptContent = "sh start-sitesearch.sh"
        }
    }
    steps {
        update<ScriptBuildStep>(1) {
            scriptContent = """
                ./gradlew clean :gadget:includeKotlinJsRuntime build --info -x test --no-build-cache
                #SPRING_PROFILES_ACTIVE=oss ./gradlew clean build --info
            """.trimIndent()
            dockerImage = "openjdk:13-alpine"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }
}
