#!/bin/bash

unknow_start() {
	java -jar unknow-http-test/target/server.jar > logs/unknow.log 2>&1 &
	pid=$!
}
unknow_stop() {
	kill -9 $pid
	pid=
}
native_start() {
	./server-native > logs/native.log 2>&1 &
	pid=$!
}
native_stop() {
	kill -9 $pid
	pid=
}
tomcat_start() {
	cp unknow-http-test/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log 2>&1 || exit 1 &
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
$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jport=8080 -Jout=out/$1.csv
${1}_stop
sleep 10
