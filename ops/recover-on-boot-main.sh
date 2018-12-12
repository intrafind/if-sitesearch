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


