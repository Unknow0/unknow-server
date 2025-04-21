#!/bin/bash

h=$1
p=$2
c=$3
out=$4


[[ -n $out ]] && mkdir -p $out

curls() {
	local n=$1
	shift
	for((i=0; i<$p; i++))
	do
		[[ -z "$out" ]] && f="/dev/null" || f="$out/$i.csv"
		curl -s -o /dev/null --no-progress-meter -w "%output{$f}$n %{response_code} %{time_total} %{time_starttransfer} %{errormsg}\n" "$@" &
	done

	wait
}

test() {
	TIMEFORMAT="duration $1 %R"
	echo "run $1"
	tout=/dev/null
	cout=/dev/null
	if [[ -z "$out" ]]
	then
		tout="$out/times.csv"
		cout="$out/$1.csv"
	fi
	{
		time curl -Z --parallel-immediate --parallel-max $p -s -o /dev/null --no-progress-meter -w "%output{>>$cout}$n %{response_code} %{time_total} %{time_starttransfer} %{errormsg}\n" "$@"
	} |& tee -a "$tout"
}

test missing -m .001 -XGET http://$h:8080/missing?[1-$c]
test simple  -m .001 -XGET http://$h:8080/test?[1-$c]
test ssl     -m .001 -XGET -k --http1.1 https://$h:8443/test?[1-$c]
test ws      -m .001 -XPOST -d@bench/req/ws.xml  http://$h:8080/ws?[1-$c]
test rest    -m .001 -XPOST -H 'Accept: application/json' -H 'Content-type: application/json' -d'{"v":"toto"}' http://$h:8080/rest/[1-$c]
test http2   -m .001 -XGET -k --http2 --resolve "*:$h" https://host[1-5]:8443/test?[1-$c]