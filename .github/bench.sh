#!/bin/bash

TESTS=(
	'missing'
	'test'
	'Webservice POST <.github/bare.xml'
	)
SERVER=(unknow tomcat)

run() {
	echo "test $1" >2
	[[ '$1' = 'Webservice POST <.github/bare.xml' ]] && return
	siege -R .github/siegerc -t$2 "http://127.0.0.1:8080/$1" 2>/dev/null
}

dotests() {
	for t in ${TESTS[@]}; do run "$t" "$1"; done
	tail -n +2 log | cut -d ',' -f 6
	rm log
}

unknow_start() {
	java -jar unknow-http-test/target/server.jar >/dev/null 2>/dev/null &
	pid=$!
	trap "kill -9 $pid" EXIT
	sleep 5
}
unknow_stop() {
	kill -9 $pid
}
tomcat_start() {
	sudo cp unknow-http-test/target/*.war /var/lib/tomcat9/webapps/ROOT.war
	sudo /var/lib/tomcat9/bin/catalina.sh run &
	pid=$!
	trap "kill -9 $pid" EXIT
	sleep 10
	cat /var/lib/tomcat9/logs/catalina.out
}
tomcat_stop() {
	/var/lib/tomcat9/bin/shutdown.sh &
}

declare -a results
for i in ${SERVER[@]}
do
	${i}_start
	echo "warmup $i"
	dotests 10s >/dev/null
	echo "testing $i"
	results+=("$(dotests 60s)")
	${i}_stop
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
