#!/usr/bin/env sh

SITE_CRAWL_STATUS_REPORT=site-crawl-status.json

# get all sites crawl status
curl -X GET \
    "https://${SIS_SERVICE_HOST}/sites/crawl/status?serviceSecret=${ADMIN_SITE_SECRET}" \
    -o $SITE_CRAWL_STATUS_REPORT

# apply fetched site status to run the crawler
curl -X POST \
    "https://${SIS_SERVICE_HOST}/sites/crawl?serviceSecret=${ADMIN_SITE_SECRET}&clearIndex=false&isThrottled=false&allSitesCrawl=true" \
    -H 'content-type: application/json' \
    -T $SITE_CRAWL_STATUS_REPORT

# update local crawl status
curl -X GET \
    "https://${SIS_SERVICE_HOST}/sites/crawl/status?serviceSecret=${ADMIN_SITE_SECRET}" \
    -o $SITE_CRAWL_STATUS_REPORT

apt-get update
apt-get install -y jq
failedCrawlStatusList=$(cat $SITE_CRAWL_STATUS_REPORT | jq -r '.sites[] | select (.pageCount | length  == 0 )');

# if failedCrawlStatusList not empty give me the siteIDs
if [ -n "$failedCrawlStatusList" ]; then
   echo "CRAWLING_FAILED"
   failedSiteIds=$(cat $SITE_CRAWL_STATUS_REPORT | jq -r '.sites[] | select (.pageCount | length == 0) | .siteId')
   echo "Site IDs: $failedSiteIds"
else
  echo "CRAWLING_SUCCESS"
fi

cat $SITE_CRAWL_STATUS_REPORT | jq .