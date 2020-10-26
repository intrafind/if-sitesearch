#!/usr/bin/env sh
#
# Copyright 2020 IntraFind Software AG. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# TODO: 1. get crawl list                                                       = DONE!
# TODO: 2. get siteIds of successful crawled sites                              = DONE!
# TODO: 3. get/extract site url                                                 = DONE!
# TODO: 4. get search of site                                                   =
# TODO: 5. compare with url => if there are not exact same url in search fail   =

apt-get update && apt-get install -y jq

SITE_CRAWL_STATUS_REPORT=site-crawl-status.json

# get all sites crawl status
curl -X GET \
    "https://${SIS_SERVICE_HOST}/sites/crawl/status?serviceSecret=${ADMIN_SITE_SECRET}" \
    -o $SITE_CRAWL_STATUS_REPORT

cat site-crawl-status.json | jq .

successCrawlStatusList=$(cat $SITE_CRAWL_STATUS_REPORT | jq -r '.sites[] | select (.pageCount | length != 0) | .siteId')

#echo $successCrawlStatusList

for i in ${successCrawlStatusList[@]}
do
  echo "here we go:" $i
  curl -X GET \
      "https://${SIS_SERVICE_HOST}/sites/${i}/profile?siteSecret=${ADMIN_SITE_SECRET}" \
      -o $PROFIL_DATA
  # cat $PROFIL_DATA | jq .
  siteUrl=$(cat $PROFIL_DATA | jq -r '.configs[] | select (.url | length != 0) | .url')
  echo "Site URL: " $siteUrl
done