#!/usr/bin/env sh

SPRING_PROFILES_ACTIVE=$SPRING_PROFILES_ACTIVE \
./gradlew :service:clean jmh
#./gradlew runJMH $1
#./gradlew clean runJMH $1 > /dev/null