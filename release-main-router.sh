#!/usr/bin/env sh

docker_network=main
docker_redirect_image=main-router
docker_tag=latest

docker network create $docker_network
cd main-router
sudo rm -rf letsencrypt
sudo cp -r /etc/letsencrypt .
sudo docker build --no-cache --pull --tag docker-registry.sitesearch.cloud/intrafind/${docker_redirect_image}:${docker_tag} .
sudo rm -rf letsencrypt
docker push docker-registry.sitesearch.cloud/intrafind/${docker_redirect_image}:${docker_tag}
