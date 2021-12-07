#!/bin/bash

TESTS=(
	'missing'
	'test'
	'ws POST <.github/xml/bare_req.xml'
	)
SERVER=(tomcat unknow)

dotests() {
	for t in "${TESTS[@]}"
	do
		date +"%Y-%m-%d %H:%M run test $t"
		siege -R .github/siegerc --quiet --log="$2" -t$1 "http://127.0.0.1:8080/$t" >/dev/null 2>/dev/null
	done
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
	$CATALINA_HOME/bin/shutdown.sh
	sleep 2
	kill -9 $pid
	trap "" EXIT
	sleep 2
}

print() {
	printf '%-10s' 'server'
	for i in "${TESTS[@]}"; do printf '%-10s' "${i%% *}"; done
	echo
	IFS=$'\n'
	for s in ${SERVER[@]}
	do
		printf '%-10s' "$s"
		for r in $(tail -n +2 $s | cut -d ',' -f $1); do printf '%-10s' "${r// }"; done
		echo
	done
}

for i in ${SERVER[@]}
do
	${i}_start
	echo "warmup $i"
	dotests 10s "/dev/null"
	rm -f $i
	echo "testing $i"
	dotests 60s "$i"
	${i}_stop
done

echo
echo "Transaction per sec"
print 6

echo "Failled connection"
print 10
