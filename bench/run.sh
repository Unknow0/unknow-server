#!/bin/bash

unknow_start() {
	java -jar unknow-http-test/target/server.jar >log 2>log &
	pid=$!
}
unknow_stop() {
	kill -9 $pid
	pid=
}
native_start() {
	./server-native >native 2>native &
	pid=$!
}
native_stop() {
	kill -9 $pid
	pid=
}
tomcat_start() {
	cp unknow-http-test/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run >/dev/null 2>/dev/null || exit 1 &
	pid=$!
}
tomcat_stop() {
	$CATALINA_HOME/bin/shutdown.sh
	sleep 2
	kill -9 $pid
	pid=
}

mkdir -p out
trap '[[ "$pid" ]] && kill -9 $pid' EXIT

${1}_start
sleep 10
$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jport=8080 -Jout=out/$i.csv
${1}_stop
sleep 10