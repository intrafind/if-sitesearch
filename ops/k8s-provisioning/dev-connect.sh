#!/usr/bin/env sh

k8s_master_node=`terraform output k8s_master_node`
tenant=`terraform workspace show`

screen -wipe
pkill -9 screen
screen -wipe
ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node pkill -9 kubectl

screen -dmS core ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node kubectl port-forward service/core 9200:9200 -n $tenant
screen -dmS if-sitesearch ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node kubectl port-forward service/if-sitesearch 8001:8001 -n $tenant

ssh -o StrictHostKeyChecking=no -fNL 9200:localhost:9200 root@$k8s_master_node
ssh -o StrictHostKeyChecking=no -fNL 8001:localhost:8001 root@$k8s_master_node