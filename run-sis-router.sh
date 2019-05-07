#!/usr/bin/env sh

docker_network=sitesearch
docker_redirect_image=router
docker_tag=dev

docker pull docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}
docker rm -f $docker_redirect_image
docker run -d --name $docker_redirect_image \
    -p 80:80 \
    -p 443:443 \
    --restart unless-stopped \
    --network $docker_network \
    docker-registry.intrafind.net/intrafind/${docker_redirect_image}:${docker_tag}

# after tests for few days will be cleaned up

#docker_network=sitesearch
#docker_redirect_image=router
#docker_tag=latest
#
#cd docker-${docker_redirect_image}
#docker build --pull --tag intrafind/${docker_redirect_image}:${docker_tag} .
#docker rm -f $docker_redirect_image
#docker run -d --name $docker_redirect_image \
#    -p 80:80 \
#    -p 443:443 \
#    -v /etc/letsencrypt:/etc/letsencrypt \
#    --restart unless-stopped \
#    --network $docker_network \
#    intrafind/${docker_redirect_image}:${docker_tag}