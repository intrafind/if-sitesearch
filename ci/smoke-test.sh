#!/usr/bin/env sh

#    --no-scan --parallel \
#    --continue \
#    --continue \
./gradlew :service:test \
    --no-scan --parallel \
    --tests *Smoke* \
    --info \
    $1