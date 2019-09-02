#!/usr/bin/env sh

if [ $# -eq 0 ]
  then
    echo "Supply [blue | green] as argument"
    exit 1
fi

if [ $1 = "blue" ]
  then
    echo -n "blue" > /var/live
  else
    echo -n "green" > /var/live
fi

consul-template -consul-addr=$CONSUL_URL -template="/templates/default.ctmpl:/etc/nginx/nginx.conf:nginx -s reload" -retry 30s -once