#!/bin/bash

h=$1
p=$2
c=$3
out=$4

out() {
	if [[ -z $out ]]
	then
		cat - > /dev/null
	else
		mkdir -p $out
		cat - > $out/$1.csv
	fi
}

curls() {
	n=$1
	shift
	for((i=0; i<$p; i++))
	do
		curl -s -o /dev/null --no-progress-meter -w "$n %{response_code} %{time_total} %{time_starttransfer} %{errormsg}\n" "$@" | out $i &
	done
	wait $(jobs -p)
}

test() {
	TIMEFORMAT="duration $1 %R"
	echo "run $1" >&2
	{ time curls "$@"; } |& tee /dev/stderr | out times
}

cd "$(dirname "$0")"

test missing -XGET http://$h:8080/missing?[0-$c]
test simple -XGET  http://$h:8080/test?[0-$c]
test ssl -k --http1.1 -XGET https://$h:8443/test?[0-$c]
test ws -XPOST -d@req/ws.xml  http://$h:8080/ws?[0-$c]
test rest -XPOST -H 'Accept: application/json' -H 'Content-type: application/json' -d'{"v":"toto"}' http://$h:8080/rest/[0-$c]
test http2 --http2-prior-knowledge -XGET http://$h:8080/test?[0-$c]
