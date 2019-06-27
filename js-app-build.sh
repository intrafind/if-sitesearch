#!/usr/bin/env sh

./gradlew :payment:build :finder:build :dashboard:build --parallel --continuous --build-cache $1
#./gradlew :gadget:build :dashboard:build --parallel --continuous --build-cache $1
