#!/bin/sh

java -jar unknow-http-test/target/server.jar >log 2>log &
pid=$!
trap "kill -9 $pid" EXIT
sleep 5

bash .github/http_test.sh http://127.0.0.1:8080