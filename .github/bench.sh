#!/bin/bash

run() {
	siege -b -c 25 -t 60s http://127.0.0.1:8080/$1 2>/dev/null | jq .
}

dotests() {
	run 'missing'
	run 'test'
}

unknow_init() {
	java -jar unknow-http-test/target/server.jar >/dev/null 2>/dev/null &
	pid=$!
	trap "kill -9 $pid" EXIT
}

declare -a results
for i in unknow
do
	$i_init
	echo "warmup $i"
	dotests > /dev/null
	results+=("$(dotests)")
done

declare -p results;
