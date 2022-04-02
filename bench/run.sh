#!/bin/bash

unknow_start() {
	java -jar unknow-http-test/target/server.jar >/dev/null 2>/dev/null &
	pid=$!
	sleep 5
}
unknow_stop() {
	kill -9 $pid
	pid=
	sleep 2
}
tomcat_start() {
	cp unknow-http-test/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run >/dev/null 2>/dev/null || exit 1 &
	pid=$!
	sleep 10
}
tomcat_stop() {
	$CATALINA_HOME/bin/shutdown.sh
	sleep 2
	kill -9 $pid
	pid=
	sleep 2
}

mkdir out
trap 'rm -rf out; [[ "$pid" ]] && kill -9 $pid' EXIT

for i in tomcat unknow
do
	${i}_start
	$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jport=8080 -Jout=out/$i.csv
	${i}_stop
done
