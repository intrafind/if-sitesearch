#!/usr/bin/env sh

workspace=default
k8s_master_node=116.203.228.233
k8s_master_node=$(terraform output k8s_master_node)
helmName=sis-sitesearch

scp -q -o StrictHostKeyChecking=no root@cd.intrafind.net:/etc/letsencrypt/live/intrafind.net/cert.pem asset/$helmName
scp -q -o StrictHostKeyChecking=no root@cd.intrafind.net:/etc/letsencrypt/live/intrafind.net/privkey.pem asset/$helmName

ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node rm -rf /opt/$helmName
scp -q -o StrictHostKeyChecking=no -r asset/$helmName root@$k8s_master_node:/opt/

#ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm delete $helmName --purge
#ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm delete ingress --purge
#sleep 13

ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node \
  helm upgrade $helmName /opt/$helmName --install --namespace $workspace \
  --set app.tenant=$workspace,app.HETZNER_API_TOKEN=$TF_VAR_hetzner_cloud_intrafind,app.adminSecret=$ADMIN_SITE_SECRET, \
  --set app.dockerRegistrySecret=$TF_VAR_docker_registry_k8s_secret,app.sis.wooCommerceConsumerKey=$WOO_COMMERCE_CONSUMER_KEY \
  --set app.sis.wooCommerceConsumerSecret=$WOO_COMMERCE_CONSUMER_SECRET,app.sis.serviceSecret=$SERVICE_SECRET, \
  --set app.meta.scmHash=$SCM_HASH,app.meta.buildNumber=$BUILD_NUMBER,app.recaptchaSecret=$RECAPTCHA_SITE_SECRET, \
  --set-string app.volumeHandle=3052845, \
  --set app.basicAuth=$BASIC_ENCODED_PASSWORD

ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node \
  helm upgrade ingress stable/nginx-ingress --install --namespace $workspace --set rbac.create=true,controller.hostNetwork=true,controller.kind=DaemonSet

#ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm test $helmName --cleanup
ssh -q -o StrictHostKeyChecking=no root@$k8s_master_node helm list --all

if [ "$(whoami)" = "alex" ]
then
  $(terraform output k8s_ssh)
fi

#kubectl create configmap \
#    --namespace kube-system kube-gelf \
#    --from-literal GELF_HOST=logs.sitesearch.cloud \
#    --from-literal GELF_PORT=12201 \
#    --from-literal GELF_PROTOCOL=udp