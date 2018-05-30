#!/usr/bin/env sh

cd /srv/if-sitesearch/ops/terraform/prod-provisioning/
sudo -u ubuntu terraform taint -module=kubernetes null_resource.Tagging-Service
sudo -u ubuntu terraform apply -auto-approve