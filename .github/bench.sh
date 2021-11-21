#!/bin/bash

TESTS=(
	'missing'
	'test'
	'Webservice POST <.github/xml/bare.xml'
	)
SERVER=(unknow tomcat)

dotests() {
	for t in ${TESTS[@]}; do siege -R .github/siegerc --log="$2" -t$1 "http://127.0.0.1:8080/$t"; done
}

unknow_start() {
	java -jar unknow-http-test/target/server.jar >/dev/null 2>/dev/null &
	pid=$!
	trap "kill -9 $pid" EXIT
	sleep 5
}
unknow_stop() {
	kill -9 $pid
	trap "" EXIT
	sleep 2
}
tomcat_start() {
	cp unknow-http-test/target/*.war $CATALINA_HOME/webapps/ROOT.war
	$CATALINA_HOME/bin/catalina.sh run >/dev/null 2>/dev/null || exit 1 &
	pid=$!
	trap "kill -9 $pid" EXIT
	sleep 10
}
tomcat_stop() {
	kill -9 $pid
	trap "" EXIT
	sleep 2
}

for i in ${SERVER[@]}
do
	${i}_start
	echo "warmup $i"
	dotests 10s "/dev/null"
	echo "testing $i"
	dotests 60s "$i"
	${i}_stop
done

printf '%-10s' 'server'
for i in ${TEST[@]}; do printf '%-10s' "${i## *}"; done
echo
IFS=$'\n'
for s in ${SERVER[@]}
do
	printf '%-10s' "$s"
#	for r in $(; do printf '%-10s' "$r"; done
	cat "$s"
done
