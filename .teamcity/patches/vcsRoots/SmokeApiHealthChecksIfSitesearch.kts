package patches.vcsRoots

import jetbrains.buildServer.configs.kotlin.v2018_2.*
import jetbrains.buildServer.configs.kotlin.v2018_2.ui.*
import jetbrains.buildServer.configs.kotlin.v2018_2.vcs.GitVcsRoot

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, change the vcsRoot with id = 'SmokeApiHealthChecksIfSitesearch'
accordingly, and delete the patch script.
*/
changeVcsRoot(RelativeId("SmokeApiHealthChecksIfSitesearch")) {
    val expected = GitVcsRoot({
        id("SmokeApiHealthChecksIfSitesearch")
        name = "smoke-api-health-check"
        url = "https://github.com/intrafind/if-sitesearch"
        authMethod = password {
            userName = "loxal"
            password = "credentialsJSON:91f52c32-83d5-465b-bd17-262b0a37cd3f"
        }
    })

    check(this == expected) {
        "Unexpected VCS root settings"
    }

    (this as GitVcsRoot).apply {
        authMethod = anonymous()
        param("secure:password", "")
        param("username", "")
    }

}
