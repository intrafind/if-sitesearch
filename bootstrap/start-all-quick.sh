#!/usr/bin/env sh

echo "== startup-script =="

docker start sitesearch-elasticsearch # replaced by Kubernetes
docker start sitesearch-elasticsearch-1 # replaced by Kubernetes
docker start sitesearch-elasticsearch-quorum # replaced by Kubernetes
docker start sitesearch-search-service # replaced by Kubernetes
docker start sitesearch-search-service-1 # replaced by Kubernetes

docker restart if-sitesearch
docker restart if-sitesearch-green
docker restart if-sitesearch-green-1
docker restart if-sitesearch-blue
docker restart if-sitesearch-blue-1

docker start consul

docker restart router

sudo sysctl -w vm.max_map_count=262144 # required for Elasticsearch
#docker-compose --file ops/docker-compose-bg.yaml -p tmp up -d # TODO really required?

docker exec router nginx -s reload
sleep 30
docker restart router
docker exec router nginx -s reload
echo "/== startup-script =="


