#!/usr/bin/env sh

#sudo mkfs.ext4 -F /dev/disk/by-id/scsi-0HC_Volume_1441441
#mkdir /mnt/elk
mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_1441441 /mnt/elk

#sudo mkfs.ext4 -F /dev/disk/by-id/scsi-0HC_Volume_1458716
#mkdir /mnt/docker-registry
mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_1458716 /mnt/docker-registry

#sudo mkfs.ext4 -F /dev/disk/by-id/scsi-0HC_Volume_1459052
#mkdir /mnt/maven
mount -o discard,defaults /dev/disk/by-id/scsi-0HC_Volume_1459052 /mnt/maven

sysctl -w vm.max_map_count=262144 # required for Elasticsearch

docker start docker-registry
docker start main-router

docker start teamcity-server
rm /root/BuildAgent/logs/buildAgent.properties.lock
/root/BuildAgent/bin/agent.sh start
docker start teamcity-agent-venus
docker start teamcity-agent-merkur

docker-compose --file /srv/if-sitesearch/opt/docker-compose-elk.yaml -p main up -d

docker start gitlab
docker start gitlab-runner

docker start main-router