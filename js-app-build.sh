#!/usr/bin/env sh

./gradlew includeKotlinJsRuntime \
  :payment:build \
  :dashboard:build \
  :stats:build \
  :gadget:build \
  --parallel \
  --continuous \
  --build-cache \
  $1
#./gradlew :gadget:build :dashboard:build --parallel --continuous --build-cache $1
