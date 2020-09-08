#!/usr/bin/env sh
# Should be executed on the host with `~/BuildAgent` directory

version=2020.1.3-linux
service_name=teamcity-server
network=main

docker rm -f $service_name
docker run -d -t --name $service_name \
    -e TEAMCITY_SERVER_MEM_OPTS="-Xmx2g -XX:MaxPermSize=270m -XX:ReservedCodeCacheSize=350m" \
    -v /srv/${service_name}:/data/teamcity_server/datadir \
    -v /srv/${service_name}/logs:/opt/teamcity/logs \
    --network $network \
    jetbrains/${service_name}:${version}

sudo ~/BuildAgent/bin/agent.sh stop kill # stop localhost-agent

# Run for major version upgrades
#cd ~/BuildAgent/bin
#    ./install.sh https://ci.sitesearch.cloud
#~/BuildAgent/bin/agent.sh run

start_ci_agent() {
    docker rm -f teamcity-agent-$1
    docker run -d -t --name teamcity-agent-$1 \
        -e SERVER_URL="https://ci.sitesearch.cloud" \
        -e AGENT_NAME=$1 \
        -v /srv/teamcity-agent-$1:/data/teamcity_agent/conf \
        --network $network \
        jetbrains/teamcity-agent:${version}
}

start_ci_agent merkur
start_ci_agent venus

sleep 20
sudo ~/BuildAgent/bin/agent.sh start # run localhost-agent on host machine

sleep 20
docker exec main-router nginx -s reload