#!/usr/bin/env sh

workspace=`terraform workspace show`
k8s_master_node=`terraform output k8s_master_node`
helmName=sis-sitesearch

rsync -e "ssh -o StrictHostKeyChecking=no" -av asset/$helmName root@$k8s_master_node:/srv/

ssh -o StrictHostKeyChecking=no root@$k8s_master_node helm delete $helmName --purge
sleep 11
ssh -o StrictHostKeyChecking=no root@$k8s_master_node \
  helm install /srv/$helmName --name $helmName --namespace $workspace \
  --set app.tenant=$workspace,app.EXTERNAL_IP=$k8s_master_node,app.HETZNER_API_TOKEN=$TF_VAR_hetzner_cloud_intrafind,app.password=$TF_VAR_password,app.dockerRegistrySecret=$TF_VAR_docker_registry_k8s_secret \
  --set-string app.volumeHandle=3052845

`terraform output k8s_ssh`
