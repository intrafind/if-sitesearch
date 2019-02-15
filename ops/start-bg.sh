#!/usr/bin/env sh

# TODO required at all?
docker-compose --file ops/docker-compose-bg.yaml -p sitesearch down
docker-compose --file ops/docker-compose-bg.yaml -p sitesearch up -d --force-recreate
docker-compose --file ops/docker-compose-bg.yaml -p sitesearch ps