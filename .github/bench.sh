#!/bin/bash

TESTS=(
	'missing'
	'test'
	'Webservice POST <.github/bare.xml'
	)
SERVER=(unknow tomcat)

run() {
	[[ '$1' = 'Webservice POST <.github/bare.xml' ]] && return
	r=$(siege --no-parser -b -c 2 -t10s "http://win7:8080/$1" 2>/dev/null)
	echo "$r" >&2
	echo "$r" | jq .transaction_rate
}

dotests() {
	for t in ${TESTS[@]}; do run "$t"; done
}

unknow_start() {
	java -jar unknow-http-test/target/server.jar >/dev/null 2>/dev/null &
	pid=$!
	trap "kill -9 $pid" EXIT
}
unknow_stop() {
	kill -9 $pid
}
tomcat_start() {
	cp unknow-http-test/target/*.war /usr/share/tomcat9/webapps/ROOT.war
	/usr/share/tomcat9/bin/catalina.sh run >/dev/null 2>/dev/null &
	pid=$!
	trap "kill -9 $pid" EXIT
}
tomcat_stop() {
	/usr/share/tomcat9/bin/shutdown.sh
}

declare -a results
for i in ${SERVER[@]}
do
	$i_start
	echo "warmup $i"
	dotests > /dev/null
	echo "testing $i"
	results+=("$(dotests)")
	$i_stop
done

printf '%-10s' 'server'
for i in ${TEST[@]}; do printf '%-10s' "${i## *}"; done
echo
IFS=$'\n'
for((i=0; i<${#SERVER[@]}; i++))
do
	printf '%-10s' "${SERVER[$i]}"
	for r in ${results[$i]}; do printf '%-10s' "$r"; done
	echo
done
