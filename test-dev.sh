#!/usr/bin/env bash

# add "--debug-jvm" to attach debugger
SPRING_PROFILES_ACTIVE=oss \
SPRING_CONFIG_NAME="application, local" ./gradlew :service:test \
    --no-scan --parallel \
    --build-cache --continuous --continue \
    --tests SimpleClientTest --info \
    -Dspring.groovy.template.check-template-location=false \
    $1

#--tests *Subscription*.subscribeViaGitHub
#--tests SimpleClientTest
