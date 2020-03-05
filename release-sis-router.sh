#!/usr/bin/env sh

docker_network=sitesearch
docker_redirect_image=router
docker_tag=latest

if [ "$*" != "" ]; then
    docker_tag="$*"
    echo "Router image tag is : " $docker_tag
fi

docker network create $docker_network
cd docker-router
sudo rm -rf letsencrypt
#scp -r root@main-if:/root/certs/* .
sudo cp -r /etc/letsencrypt .
sudo docker build --no-cache --pull --tag docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag} .
sudo rm -rf letsencrypt
docker push docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}