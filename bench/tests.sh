#!/bin/bash

h=$1
p=$2
c=$3


test() {
	n=$1
	shift
	TIMEFORMAT="duration $n %R"
	time curl -s -o /dev/null --no-progress-meter -Z --parallel-immediate --parallel-max $p -w "$n %{response_code} %{time_total} %{time_starttransfer}\n" $@ 
}

cd "$(dirname "$0")"

test missing -XGET http://$h:8080/missing?[0-$c]  2>&1
test simple -XGET  http://$h:8080/test?[0-$c] 2>&1
test ssl -XGET  https://$h:8443/test?[0-$c]
test ws -XPOST -d@req/ws.xml  http://$h:8443/ws?[0-$c]
test rest -XPOST -d'{"v":"toto"}' http://$h:8443/rest/[0-$c]
test http2 --http2 -XGET https://$h:8443/test?[0-$c]