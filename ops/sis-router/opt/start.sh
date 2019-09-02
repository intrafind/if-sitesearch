#!/usr/bin/env sh

nginx -g 'daemon off;' &
echo -n $LIVE > /var/live
consul-template -consul-addr=$CONSUL_URL -template="/templates/default.ctmpl:/etc/nginx/nginx.conf:nginx -s reload"