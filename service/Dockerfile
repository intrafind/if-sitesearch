FROM openjdk:12-jdk AS service

MAINTAINER IntraFind Software AG <alexander.orlov@intrafind.de>

WORKDIR /srv
COPY config config
COPY build/libs/*.jar .
ENV SPRING_CONFIG_NAME application, prod

EXPOSE 8001

CMD ["java", "-jar", "-Xms256m", "-Xmx256m", "service.jar"]