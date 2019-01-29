#!/usr/bin/env sh

#cp -r /home/ubuntu/docker-build-data/api-sitesearch/service .
cd service
docker build --pull --no-cache --tag docker-registry.sitesearch.cloud/intrafind/if-sitesearch:latest .
docker push docker-registry.sitesearch.cloud/intrafind/if-sitesearch:latest

ssh ubuntu@main.sitesearch.cloud docker rm -f if-sitesearch
#    --log-opt gelf-address=udp://logs.sitesearch.cloud:12201 \
#    --log-driver=gelf \
ssh ubuntu@main.sitesearch.cloud docker run -d --name if-sitesearch \
    --env SIS_API_SERVICE_URL=$SIS_API_SERVICE_URL \
    --env SERVICE_SECRET=$SERVICE_SECRET \
    --env SIS_SERVICE_HOST=$SIS_SERVICE_HOST \
    --env WOO_COMMERCE_CONSUMER_KEY="$WOO_COMMERCE_CONSUMER_KEY" \
    --env WOO_COMMERCE_CONSUMER_SECRET="$WOO_COMMERCE_CONSUMER_SECRET" \
    --env ADMIN_SITE_SECRET=$ADMIN_SITE_SECRET \
    --env INVISIBLE_RECAPTCHA_SITE_SECRET=${INVISIBLE_RECAPTCHA_SITE_SECRET} \
    --env SPRING_SECURITY_USER_PASSWORD=$SPRING_SECURITY_USER_PASSWORD \
    --env BUILD_NUMBER=$BUILD_NUMBER \
    --env SCM_HASH=$SCM_HASH \
    --env SECURITY_OAUTH2_CLIENT_CLIENT_SECRET=$SECURITY_OAUTH2_CLIENT_CLIENT_SECRET \
    --network sitesearch \
    docker-registry.sitesearch.cloud/intrafind/if-sitesearch:latest

danglingImages=$(docker images -f "dangling=true" -q)
if [ "$danglingImages" ]; then
    docker rmi -f $danglingImages # cleanup, GC for dangling images
else
    echo "There are no dangling Docker images"
fi

docker volume prune -f