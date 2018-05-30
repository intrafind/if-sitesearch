#!/usr/bin/env sh

cd /srv/if-sitesearch/ops/terraform/prod-provisioning/
sudo -u ubuntu terraform destroy -auto-approve
# if you want have complete new state use the code below
# rm -rf terraform.tfstate
# rm -rf .terraform/
# sudo -u ubuntu terraform init
# ======================================================
sudo -u ubuntu terraform apply -auto-approve