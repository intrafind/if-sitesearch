#!/usr/bin/env sh

docker_tag=latest
docker_image_name=sis-sitesearch
img_fqn=docker-registry.intrafind.net/intrafind/${docker_image_name}:${docker_tag}
docker_network=sitesearch

ssh ubuntu@main.sitesearch.cloud docker rm -f ${img_fqn}

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
        --env SIS_API_SERVICE_URL=$SIS_API_SERVICE_URL \
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
        ${img_fqn}
}

startComponent() {
    ssh ubuntu@main.sitesearch.cloud docker rm -f $1
    runService $1
}

if isBlueUp; then
    echo "blue is active"
    green="${docker_image_name}-green"

    startComponent ${green}
    startComponent ${green}-1
    sleep 21
    ssh ubuntu@main.sitesearch.cloud docker exec router switch.sh green

else
    echo "blue is inactive"
    blue="${docker_image_name}-blue"

    startComponent ${blue}
    startComponent ${blue}-1
    sleep 21
    ssh ubuntu@main.sitesearch.cloud docker exec router switch.sh blue
fi
