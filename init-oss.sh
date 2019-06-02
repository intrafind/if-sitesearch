#!/usr/bin/env sh

docker exec -t elk-elasticsearch curl -X PUT \
docker exec -t ops-es curl -X PUT \
  http://localhost:9200/site-profile/_doc/site-configuration-b7fde685-33f4-4a79-9ac3-ee3b75b83fa3 \
  -H 'Content-Type: application/json' \
  -d '{
    "id": ["b7fde685-33f4-4a79-9ac3-ee3b75b83fa3"],
    "secret": ["56158b15-0d87-49bf-837d-89085a4ec88d"],
    "email": ["user@example.com"]
  }'