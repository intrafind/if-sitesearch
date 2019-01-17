#!/usr/bin/env bash

# add "--debug-jvm" to attach debugger
SPRING_PROFILES_ACTIVE=oss \
SPRING_CONFIG_NAME="application, local" ./gradlew clean :service:test \
    --no-scan --parallel \
    --build-cache --continuous --continue \
    --tests SimpleClientTest --info \
    $1

#--tests *Subscription*.subscribeViaGitHub
#--tests SimpleClientTest
