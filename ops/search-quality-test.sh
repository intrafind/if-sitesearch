#!/usr/bin/env sh

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

# TODO: Fix multisite profile test results

apt-get update && apt-get install -y jq

SITE_CRAWL_STATUS_REPORT=site-crawl-status.json
PROFIL_DATA=profil-data.json
SEARCH_RESULT=search.json

set -a successCrawlStatusList
set -a failedSearchResult
set -a searchResultUrl

curl -X GET \
    "https://${SIS_SERVICE_HOST}/sites/crawl/status?serviceSecret=${ADMIN_SITE_SECRET}" \
    -o $SITE_CRAWL_STATUS_REPORT

successCrawlStatusList=$(cat $SITE_CRAWL_STATUS_REPORT | jq -r '.sites[] | select (.pageCount | length != 0) | .siteId')

for i in ${successCrawlStatusList[@]}
do
  echo "here we go:" $i
  curl -X GET \
      "https://${SIS_SERVICE_HOST}/sites/${i}/profile?siteSecret=${ADMIN_SITE_SECRET}" \
      -o $PROFIL_DATA

  siteUrl=$(cat $PROFIL_DATA | jq -r '[.configs[].url][0]' | sed 's:/*$::')
  echo "First trimming : " $siteUrl
  siteUrl=$(echo $siteUrl | sed -E -e 's_.*://([^/@]*@)?([^/:]+).*_\2_')
  echo "Second trimming : " $siteUrl
  siteUrl=$(awk -F. '{ if ($(NF-1) == "co") printf $(NF-2)"."; printf $(NF-1)"."$(NF)"\n";}' <<< ${siteUrl})
  echo "Thirth trimming : " $siteUrl
  curl -X GET \
      "https://${SIS_SERVICE_HOST}/sites/${i}/search?sSearchTerm=%2A&query=%2A&_=1603728086174" \
      -o $SEARCH_RESULT

  echo "Search result: "
  cat $SEARCH_RESULT | jq .
  searchResultUrl=$(cat $SEARCH_RESULT | jq -r '.results[] | [.urlRaw]')

  echo $searchResultUrl | jq .

  echo $siteUrl
  [[ " ${searchResultUrl[@]} " == *"${siteUrl}"* ]] && echo "true" || echo "false"
done