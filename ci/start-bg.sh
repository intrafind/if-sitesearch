#!/usr/bin/env sh

docker-compose --file opt/docker-compose-bg.yaml -p tmp down
docker-compose --file opt/docker-compose-bg.yaml -p tmp up -d --force-recreate
docker-compose --file opt/docker-compose-bg.yaml -p tmp ps