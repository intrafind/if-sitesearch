#!/usr/bin/env sh

sudo apt install software-properties-common -y
sudo apt update -y

sudo rm /etc/apt/sources.list.d/google-cloud-sdk.list
export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)"
echo "deb http://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
sudo apt-get update && sudo apt-get install google-cloud-sdk -y
gcloud auth activate-service-account --key-file=/srv/minion/compute-engine-admin.json
gcloud config set project woven-alpha-150909

gcloud compute disks list --format='value(name,zone)'| while read DISK_NAME ZONE; do
  gcloud compute disks snapshot $DISK_NAME --snapshot-names ${DISK_NAME}-$(date "+%Y-%m-%d-%s") --zone $ZONE
done

if [ "$(uname)" = "Linux" ]; then
  from_date=$(date -d "-30 days" "+%Y-%m-%d")
else
  from_date=$(date -v -30d "+%Y-%m-%d")
fi
gcloud compute snapshots list --filter="creationTimestamp<$from_date" --uri | while read SNAPSHOT_URI; do
   gcloud compute snapshots delete $SNAPSHOT_URI  --quiet
done