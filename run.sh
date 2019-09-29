#!/usr/bin/env sh

# add "--debug-jvm" to attach debugger
{
    SPRING_CONFIG_NAME="application, local" \
        ./gradlew bootRun --continue --no-scan --no-build-cache $1
#        ./gradlew bootRun --continue --continuous --no-scan --parallel --build-cache --refresh-dependencies $1
#        --refresh-dependencies --rerun-tasks --no-build-cache $1
} || {
    hangingJavaProcessToStop=`jps | grep Application | awk '{print $1}'`
    echo "hangingJavaProcessToStop: $hangingJavaProcessToStop"
    kill -9 $hangingJavaProcessToStop
    echo "Gracefully killed hanging process: $hangingJavaProcessToStop"
}

#touch service/build/resources/main/application.yaml # to trigger Spring Boot reload
#./gradlew :service:compileJava :service:processResources # to trigger reload
