FROM openjdk:10-jre as service

MAINTAINER Alexander Orlov <alexander.orlov@intrafind.de>

ADD build/libs/*.jar service/
ADD config config
ENV SPRING_CONFIG_NAME application, prod

EXPOSE 8001

CMD java -jar -Xms256m -Xmx256m service/*.jar