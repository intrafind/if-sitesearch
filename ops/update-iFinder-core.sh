#!/usr/bin/env sh

sudo chown -R 1000:1000 /srv/sitesearch-*

docker-compose --file ./opt/docker-compose-ifinder-core.yaml -p sitesearch down

#THIS IS TEMPORARY DELETE IF YOU STILL SEE THIS AFTER 18.04.18
#sudo rm -r /srv/sitesearch-search-service*

docker-compose --file ./opt/docker-compose-ifinder-core.yaml -p sitesearch up -d

docker exec router nginx -s reload
