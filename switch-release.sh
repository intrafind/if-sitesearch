#!/usr/bin/env sh

export docker_tag=latest
container_name=if-sitesearch
docker_image_name=sis-sitesearch
docker_network=sitesearch

isBlueUp() {
    if [ -f "./blue-green-deployment.lock" ]; then
        rm ./blue-green-deployment.lock
        return 0
    else
        touch ./blue-green-deployment.lock
        return 1
    fi
}

runService() {
    ssh ubuntu@main.sitesearch.cloud docker run -d --name $1 \
        --log-driver=gelf \
        --log-opt gelf-address=udp://logs.sitesearch.cloud:12201 \
        --env SERVICE_SECRET=$SERVICE_SECRET \
        --env SIS_SERVICE_HOST=$SIS_SERVICE_HOST \
        --env WOO_COMMERCE_CONSUMER_KEY="$WOO_COMMERCE_CONSUMER_KEY" \
        --env WOO_COMMERCE_CONSUMER_SECRET="$WOO_COMMERCE_CONSUMER_SECRET" \
        --env ADMIN_SITE_SECRET=$ADMIN_SITE_SECRET \
        --env INVISIBLE_RECAPTCHA_SITE_SECRET=${INVISIBLE_RECAPTCHA_SITE_SECRET} \
        --env SPRING_SECURTY_USER_PASSWORD=$SPRING_SECURITY_USER_PASSWORD \
        --env BUILD_NUMBER=$BUILD_NUMBER \
        --env SCM_HASH=$SCM_HASH \
        --env SECURITY_OAUTH2_CLIENT_CLIENT_SECRET=$SECURITY_OAUTH2_CLIENT_CLIENT_SECRET \
        --restart unless-stopped \
        --network $docker_network \
        docker-registry.intrafind.net/intrafind/${docker_image_name}:${docker_tag}
}

startComponent() {
    ssh ubuntu@main.sitesearch.cloud docker rm -f $1
    ssh ubuntu@main.sitesearch.cloud docker rmi -f docker-registry.intrafind.net/intrafind/sis-sitesearch:latest
    runService $1
}

if isBlueUp; then
    echo "blue is active"
    current="${container_name}-green"

    startComponent ${current}
    startComponent ${current}-1
    sleep 21
    ssh ubuntu@main.sitesearch.cloud docker exec router switch.sh green

else
    echo "blue is inactive"
    current="${container_name}-blue"

    startComponent ${current}
    startComponent ${current}-1
    sleep 21
    ssh ubuntu@main.sitesearch.cloud docker exec router switch.sh blue
fi
