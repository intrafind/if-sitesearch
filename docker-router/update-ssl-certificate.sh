#!/usr/bin/env sh
# TODO: nginx cert path change and reload nginx
# TODO: add new certs to load balancer

date=2018-12-18

sudo apt install software-properties-common -y
sudo add-apt-repository ppa:certbot/certbot -y
sudo apt update -y
sudo apt install certbot -y
sudo apt update -y

# install and init gcloud
export CLOUD_SDK_REPO="cloud-sdk-$(lsb_release -c -s)"
echo "deb http://packages.cloud.google.com/apt $CLOUD_SDK_REPO main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -
sudo apt-get update && sudo apt-get install google-cloud-sdk -y
gcloud auth activate-service-account --key-file=/srv/minion/compute-engine-admin.json
gcloud config set project woven-alpha-150909

# still manually to trigger here ... needs some tests
sudo certbot \
    -d *.sitesearch.cloud \
    --manual \
    --preferred-challenges dns certonly \
    --server https://acme-v02.api.letsencrypt.org/directory

gcloud compute ssl-certificates list

gcloud compute ssl-certificates create sis-cert-wc-${date} --certificate=/etc/letsencrypt/live/sitesearch.cloud-0001/fullchain.pem \
        --private-key=/etc/letsencrypt/live/sitesearch.cloud-0001/privkey.pem

# TO FIX
#gcloud compute [target-https-proxies | target-ssl-proxies] update [NAME] \
#    --ssl-certificates [SSL-CERTIFICATE-NAME]
#
#gcloud compute target-https-proxies update site-search-europe-forwarding-rule \
#    --ssl-certificates sis-cert-wc-2018-12-18 --url-map=site-search-europe
#
#gcloud compute target-https-proxies update site-search-europe-forwarding-rule-2 \
#    --ssl-certificates sis-cert-wc-2018-12-18 --url-map=site-search-europe

docker exec -it router nginx -s reload