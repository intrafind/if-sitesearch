FROM openjdk:13-oracle AS service

MAINTAINER IntraFind Software AG <alexander.orlov@intrafind.de>

RUN groupadd -g 1000 svc && \
    useradd --system -u 1000 -g svc svc
USER svc

WORKDIR /srv
COPY config config
COPY build/libs/*.jar .
ENV SPRING_CONFIG_NAME application, prod

EXPOSE 8001

CMD ["java", "-jar", "-Xms256m", "-Xmx256m", "service.jar"]