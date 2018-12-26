#!/usr/bin/env bash

# add "--debug-jvm" to attach debugger
SPRING_PROFILES_ACTIVE=oss \
SPRING_CONFIG_NAME="application, local" ./gradlew :service:test \
    --no-scan --parallel \
    --build-cache --continuous --continue \
    --tests SimpleClientTest.test --info \
    $1

#--tests *Subscription*.subscribeViaGitHub
#--tests SimpleClientTest
