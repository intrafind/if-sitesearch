FROM nginx:stable

MAINTAINER IntraFind Software AG <alexander.orlov@intrafind.de>

RUN apt-get update
RUN apt-get -y install unzip vim

COPY letsencrypt /etc/letsencrypt
COPY ssl.nginx /etc/nginx/snippets/ssl.conf
COPY cors-headers.nginx /etc/nginx/snippets/cors-headers.conf
COPY default.nginx /etc/nginx/nginx.conf
RUN echo 'sitesearch:$apr1$yiWHOvCy$YVz/vNHOKrH9LJGEglMOg.' > /etc/nginx/.htpasswd
RUN echo 'minion:$apr1$yiWHOvCy$YVz/vNHOKrH9LJGEglMOg.' >> /etc/nginx/.htpasswd
RUN echo 'service:$2y$09$Tf3OrS.R4DNRg7OTH3x2pOC2ErJLRXzxRIjeTsbWoHfDxRtodmLUy' >> /etc/nginx/.htpasswd
RUN echo 'external:$apr1$JINyTNp.$5FMZD/9WiQv3J0SdVkeS61' >> /etc/nginx/.htpasswd

EXPOSE 443