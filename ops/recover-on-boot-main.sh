#!/usr/bin/env sh

mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_3236434 /mnt/ca-backup
mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_4837088 /mnt/ca-on-premise
mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_1458716 /mnt/docker-registry
mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_1441441 /mnt/elk
mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_3724737 /mnt/gitlab-server

sysctl -w vm.max_map_count=262144 # required for Elasticsearch

docker start docker-auth
docker start docker-registry

#docker start teamcity-server
#rm /root/BuildAgent/logs/buildAgent.properties.lock
#/root/BuildAgent/bin/agent.sh start
#docker start teamcity-agent-venus
#docker start teamcity-agent-merkur

docker-compose --file /srv/if-sitesearch/opt/docker-compose-elk.yaml -p main up -d

docker start gitlab
docker start gitlab-shared-runner1
docker start gitlab-shared-runner2
docker start gitlab-shared-runner-venus
docker start sourcegraph

docker start main-router

docker exec gitlab gitlab-ctl restart