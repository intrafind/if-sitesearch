#!/usr/bin/env bash

# add "--debug-jvm" to attach debugger
#SPRING_CONFIG_NAME="application, local" ./gradlew clean :service:test \
SPRING_PROFILES_ACTIVE=oss \
SPRING_CONFIG_NAME="application, local" ./gradlew :service:test \
    --no-scan --parallel \
    --build-cache --continuous --continue \
    --info \
    $1
#    --tests SimpleClientTest \

#--tests *Subscription*.subscribeViaGitHub
#--tests SimpleClientTest
