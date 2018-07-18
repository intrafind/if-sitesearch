#!/usr/bin/env sh

# TODO team productize

today=`date -u +"%Y-%m-%d"`
cd service/src/main/resources/static/searchbar/build
rm searchbar-latest.zip
curl http://nexus:8081/nexus/service/local/repositories/snapshots/content/com/intrafind/if-app-searchbar/5.1.3.2-SNAPSHOT/if-app-searchbar-5.1.3.2-20180115.152120-26.zip \
    -o searchbar-latest.zip

rm -rf if-app-searchbar-*
unzip searchbar-latest.zip
mkdir ../$today
mv if-app-searchbar-* ../$today/app
cp -r ../latest/config ../$today
cp -r ../latest/gadget ../$today

# check if a ./$today folder is created and delete it, if that is the case

# Google Storage Operations

searchbarVersion="2018-04-06-disabled"
gsutil -m rm -r gs://site-search-europe/searchbar/$searchbarVersion
gsutil -m cp -r ./service/src/main/resources/static/searchbar/$searchbarVersion gs://site-search-europe/searchbar/

# transfer as gzip files to cdn with real metadata so that browser knows what to do with those gzip files
gsutil cp -z css -a public-read ./service/src/main/resources/static/searchbar/$searchbarVersion/app/css/app.css gs://site-search-europe/searchbar/$searchbarVersion/app/css/
gsutil cp -z js -a public-read ./service/src/main/resources/static/searchbar/$searchbarVersion/app/js/app.js gs://site-search-europe/searchbar/$searchbarVersion/app/js/
gsutil cp -z json -a public-read ./service/src/main/resources/static/searchbar/$searchbarVersion/config/sitesearch.json gs://site-search-europe/searchbar/$searchbarVersion/config/
