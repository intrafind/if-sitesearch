#!/usr/bin/env sh

docker_network=main
docker_redirect_image=main-router
docker_tag=latest

if [ "$*" != "" ]; then
    docker_tag="$*"
    echo "Router image tag is : " $docker_tag
fi

chmod -R o+x /mnt/ca-on-premise
docker pull docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}
docker rm -f $docker_redirect_image
docker run -d --name $docker_redirect_image \
    -p 80:80 \
    -p 443:443 \
    -v /mnt/ca-on-premise:/srv/download \
    --network $docker_network \
    docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}