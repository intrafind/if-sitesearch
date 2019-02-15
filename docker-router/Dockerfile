FROM nginx:stable

MAINTAINER IntraFind Software AG <alexander.orlov@intrafind.de>

RUN apt-get update
RUN apt-get -y install unzip vim
ADD https://releases.hashicorp.com/consul-template/0.19.5/consul-template_0.19.5_linux_amd64.zip /usr/bin/
RUN unzip /usr/bin/consul-template_0.19.5_linux_amd64.zip -d /usr/local/bin

COPY opt/* /bin/
RUN chmod +x /bin/switch.sh /bin/start.sh
COPY opt/default.* /templates/

RUN mkdir -p /etc/letsencrypt/.well-known # enables Letsencrypt validation
COPY ssl.nginx /etc/nginx/snippets/ssl.conf
COPY cors-headers.nginx /etc/nginx/snippets/cors-headers.conf
COPY routes.nginx /etc/nginx/conf.d/default.conf
RUN echo 'sitesearch:$apr1$yiWHOvCy$YVz/vNHOKrH9LJGEglMOg.' > /etc/nginx/.htpasswd

ENV BLUE_NAME blue
ENV GREEN_NAME green
# TODO are below ENV required at all?
ENV CONSUL_URL consul:8500
ENV constraint:node master

EXPOSE 80 443
ENTRYPOINT ["/bin/start.sh"]