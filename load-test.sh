#!/usr/bin/env sh

./gradlew :service:clean jmh
#./gradlew runJMH $1
#./gradlew clean runJMH $1 > /dev/null