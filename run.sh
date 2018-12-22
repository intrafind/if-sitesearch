#!/usr/bin/env sh

# add "--debug-jvm" to attach debugger
{
#    SPRING_PROFILES_ACTIVE=oss \
    SPRING_CONFIG_NAME="application, local" \
        ./gradlew bootRun --continue --continuous --no-scan --parallel --build-cache $1
} || {
    hangingJavaProcessToStop=`jps | grep Application | awk '{print $1}'`
    echo "hangingJavaProcessToStop: $hangingJavaProcessToStop"
    kill -9 $hangingJavaProcessToStop
    echo "Gracefully killed hanging process: $hangingJavaProcessToStop"
}

#touch service/build/resources/main/application.yaml # to trigger Spring Boot reload