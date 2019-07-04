FROM openjdk:13-alpine AS service

MAINTAINER IntraFind Software AG <alexander.orlov@intrafind.de>

RUN groupadd -g 1000 user && \
    useradd --system -u 1000 -g user user
USER user

WORKDIR /srv
COPY config config
COPY build/libs/*.jar .
ENV SPRING_CONFIG_NAME application, prod

EXPOSE 8001

CMD ["java", "-jar", "-Xms256m", "-Xmx256m", "service.jar"]