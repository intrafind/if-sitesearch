FROM openjdk:8-jdk AS builder

MAINTAINER Alexander Orlov <alexander.orlov@intrafind.de>

WORKDIR /opt/builder

ARG SIS_API_SERVICE_URL
ARG SERVICE_SECRET
ARG SIS_SERVICE_HOST
ARG WOO_COMMERCE_CONSUMER_KEY
ARG WOO_COMMERCE_CONSUMER_SECRET
ARG BASIC_ENCODED_PASSWORD
ARG ADMIN_SITE_SECRET
ARG INVISIBLE_RECAPTCHA_SITE_SECRET
ARG SPRING_SECURITY_USER_PASSWORD
ARG BUILD_NUMBER
ARG SCM_HASH
ARG SECURITY_OAUTH2_CLIENT_CLIENT_SECRET 

ENV SIS_API_SERVICE_URL $SIS_API_SERVICE_URL
ENV SERVICE_SECRET $SERVICE_SECRET
ENV SIS_SERVICE_HOST $SIS_SERVICE_HOST
ENV WOO_COMMERCE_CONSUMER_KEY "$WOO_COMMERCE_CONSUMER_KEY"
ENV WOO_COMMERCE_CONSUMER_SECRET "$WOO_COMMERCE_CONSUMER_SECRET"
ENV BASIC_ENCODED_PASSWORD "$BASIC_ENCODED_PASSWORD"
ENV ADMIN_SITE_SECRET $ADMIN_SITE_SECRET
ENV INVISIBLE_RECAPTCHA_SITE_SECRET ${INVISIBLE_RECAPTCHA_SITE_SECRET}
ENV SPRING_SECURITY_USER_PASSWORD $SPRING_SECURITY_USER_PASSWORD
ENV BUILD_NUMBER $BUILD_NUMBER
ENV SCM_HASH $SCM_HASH
ENV DEV_SKIP_FLAG true
ENV SECURITY_OAUTH2_CLIENT_CLIENT_SECRET $SECURITY_OAUTH2_CLIENT_CLIENT_SECRET

COPY . /opt/builder
RUN ./gradlew build --info

FROM openjdk:10-jre AS service

WORKDIR /srv
COPY --from=builder /opt/builder/service/build/libs/*.jar .
COPY --from=builder /opt/builder/service/config config
ENV SPRING_CONFIG_NAME application, prod

EXPOSE 8001

#CMD ["java", "-jar", "-Xms256m", "-Xmx256m", "/srv/*.jar"]
CMD ["java", "-jar", "-Xms256m", "-Xmx256m", "service.jar"]
#CMD java -jar -Xms256m -Xmx256m /srv/*.jar