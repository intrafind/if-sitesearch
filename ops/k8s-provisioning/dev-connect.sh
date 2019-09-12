#!/usr/bin/env sh

k8s_master_node=`terraform output k8s_master_node`
#tenant=`terraform workspace show`
tenant=kube-system

ssh-keygen -f ~/.ssh/known_hosts -R $k8s_master_node
screen -wipe
ssh -o StrictHostKeyChecking=no root@$k8s_master_node pkill kubectl

screen -dmS elasticsearch ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node kubectl port-forward service/elasticsearch 9200:9200 -n $tenant
screen -dmS sis-sitesearch ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node kubectl port-forward service/sis-sitesearch 8001:8001 -n $tenant
screen -dmS kibana ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node kubectl port-forward service/kibana 5601:5601 -n $tenant

ssh -o StrictHostKeyChecking=no -fNL 9200:localhost:9200 root@$k8s_master_node
ssh -o StrictHostKeyChecking=no -fNL 8001:localhost:8001 root@$k8s_master_node
ssh -o StrictHostKeyChecking=no -fNL 5601:localhost:5601 root@$k8s_master_node
