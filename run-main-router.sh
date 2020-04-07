#!/usr/bin/env sh

docker_network=main
docker_redirect_image=main-router
docker_tag=latest

chmod -R 777 /mnt/maven/repository
chmod -R o+x /mnt/ca-on-premise
docker pull docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}
docker rm -f $docker_redirect_image
docker run -d --name $docker_redirect_image \
    -p 80:80 \
    -p 443:443 \
    -v /mnt/maven/repository:/srv/maven-repository \
    -v /mnt/ca-on-premise:/srv/download \
    --network $docker_network \
    docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}