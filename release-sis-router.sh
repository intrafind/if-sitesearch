#!/usr/bin/env sh

docker_network=sitesearch
docker_redirect_image=router
docker_tag=dev

docker network create $docker_network
cd docker-router
sudo rm -rf letsencrypt
sudo cp -r /etc/letsencrypt .
sudo docker build --no-cache --pull --tag docker-registry.sitesearch.cloud/intrafind/${docker_redirect_image}:${docker_tag} .
sudo rm -rf letsencrypt
docker push docker-registry.sitesearch.cloud/intrafind/${docker_redirect_image}:${docker_tag}

