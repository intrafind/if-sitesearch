#!/usr/bin/env sh

echo "== startup-script =="
# toolchain
#docker start teamcity-server
#sudo rm /home/ubuntu/BuildAgent/logs/buildAgent.properties.lock
#/home/ubuntu/BuildAgent/bin/agent.sh start
#docker start teamcity-agent-venus
#docker start teamcity-agent-merkur

docker start sitesearch-elasticsearch # replaced by Kubernetes
docker start sitesearch-elasticsearch-1 # replaced by Kubernetes
docker start sitesearch-elasticsearch-quorum # replaced by Kubernetes
docker start sitesearch-search-service # replaced by Kubernetes
docker start sitesearch-search-service-1 # replaced by Kubernetes

docker start if-sitesearch
docker start if-sitesearch-green
docker restart if-sitesearch-green-1
docker start if-sitesearch-blue
docker restart if-sitesearch-blue-1

docker restart consul

#docker restart if-tagging-service # replaced by Kubernetes

docker restart router
#docker start if-app-webcrawler # removed/deprecated for good

sudo sysctl -w vm.max_map_count=262144 # required for Elasticsearch
#docker-compose --file opt/docker-compose-elk.yaml -p sitesearch up -d # moved to Hetzner main
docker-compose --file opt/docker-compose-bg.yaml -p tmp up -d

docker exec router nginx -s reload
sleep 30
docker restart router
docker exec router nginx -s reload
echo "/== startup-script =="


