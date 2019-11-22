#!/usr/bin/env sh

./gradlew includeKotlinJsRuntime \
  :stats:build \
  --parallel \
  --continuous \
  --build-cache \
  $1

#  :payment:build \
#  :dashboard:build \
#  :jest:build \
#  :gadget:build \