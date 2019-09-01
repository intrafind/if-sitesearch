#!/usr/bin/env sh

#workspace=`terraform workspace show`
workspace=default
#k8s_master_node=`terraform output k8s_master_node`
k8s_master_node=159.69.207.110
helmName=sis-sitesearch

scp -q -o StrictHostKeyChecking=no root@cd.intrafind.net:/etc/letsencrypt/live/intrafind.net/cert.pem asset/$helmName
scp -q -o StrictHostKeyChecking=no root@cd.intrafind.net:/etc/letsencrypt/live/intrafind.net/privkey.pem asset/$helmName

ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node rm -rf /srv/$helmName
scp -q -o StrictHostKeyChecking=no -r asset/$helmName root@$k8s_master_node:/srv/

#ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm delete $helmName --purge
#ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm delete ingress --purge
#sleep 13

#  helm install /srv/$helmName --name $helmName --namespace $workspace \
ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node \
  helm upgrade $helmName /srv/$helmName --namespace $workspace \
  --set app.tenant=$workspace,app.HETZNER_API_TOKEN=$TF_VAR_hetzner_cloud_intrafind,app.adminSecret=$ADMIN_SITE_SECRET, \
  --set app.dockerRegistrySecret=$TF_VAR_docker_registry_k8s_secret,app.sis.wooCommerceConsumerKey=$WOO_COMMERCE_CONSUMER_KEY, \
  --set app.sis.wooCommerceConsumerSecret=$WOO_COMMERCE_CONSUMER_SECRET,app.sis.serviceSecret=$SERVICE_SECRET, \
  --set app.meta.scmHash=$SCM_HASH,app.meta.buildNumber=$BUILD_NUMBER,app.recaptchaSecret=$RECAPTCHA_SITE_SECRET \
  --set-string app.volumeHandle=3052845

#ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node \
#  helm install --name ingress stable/nginx-ingress --set rbac.create=true,controller.hostNetwork=true,controller.kind=DaemonSet
ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node \
  helm upgrade ingress stable/nginx-ingress --set rbac.create=true,controller.hostNetwork=true,controller.kind=DaemonSet

ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm test $helmName --cleanup
ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm list --all

if [ "$(hostname)" = "sun" ]
then
  $(terraform output k8s_ssh)
fi
