#!/usr/bin/env sh

docker_redirect_image=sis-router
docker_tag=latest

sudo rm -rf letsencrypt
sudo cp -r /etc/letsencrypt .
sudo docker build --no-cache --pull --tag docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag} .
sudo rm -rf letsencrypt
docker push docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}