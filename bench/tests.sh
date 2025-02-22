#!/bin/bash

h=$1
p=$2
c=$3
t=$4
pid=$5
out=$6

out() {
	if [[ -z $out ]]
	then
		cat - > /dev/null
	else
		mkdir -p $out
		cat - >> $out/$1.csv
	fi
}

curls() {
	local n=$1
	shift
	for((i=0; i<$p; i++))
	do
		#curl -s -o /dev/null --no-progress-meter -w "$n %{response_code} %{time_total} %{time_starttransfer} %{errormsg}\n" "$@" | out $i &
		curl -s -o /dev/null --no-progress-meter -w "$n %{response_code} %{time_total} %{time_appconnect} %{errormsg}\n" "$@" | out $i &
	done
	waitpid $t $(jobs -p)
	kill -3 $pid 2>/dev/null
	kill $(jobs -p) 2> /dev/null
}

waitpid() {
	i=$1
	shift
	for p in "$@"
	do
		while kill -0 $p 2> /dev/null
		do
	   		sleep 1
			[[ $((--i)) -lt 1 ]] && return 1
		done
	done
}

test() {
	TIMEFORMAT="duration $1 %R"
	echo "run $1" >&2
	{ time curls "$@"; } |& tee /dev/stderr | out times
}

test missing -XGET http://$h:8080/missing?[1-$c]
test simple -XGET  http://$h:8080/test?[1-$c]
test ssl -k --http1.1 -XGET https://$h:8443/test?[1-$c]
test ws -XPOST -d@bench/req/ws.xml  http://$h:8080/ws?[1-$c]
test rest -XPOST -H 'Accept: application/json' -H 'Content-type: application/json' -d'{"v":"toto"}' http://$h:8080/rest/[1-$c]
test http2 -k --http2 --parallel-immediate --parallel-max 10 -XGET https://$h:8443/test?[1-$c]
