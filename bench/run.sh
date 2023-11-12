#!/bin/bash

unknow_start() {
	java -jar unknow-server-test/unknow-server-test-jar/target/server.jar > logs/unknow.log 2>&1 &
	pid=$!
}
unknow_stop() {
	kill -9 $pid
	pid=
}
native_start() {
	chmod a+x server-native
	./server-native > logs/native.log 2>&1 &
	pid=$!
}
native_stop() {
	kill -9 $pid
	pid=
}
tomcat_start() {
	cp unknow-server-test/unknow-server-tomcat/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log 2>&1 || exit 1 &
	pid=$!
}
tomcat_stop() {
	$CATALINA_HOME/bin/shutdown.sh
	sleep 2
	kill -9 $pid
	pid=
}

cxf_start() {
	cp unknow-server-test/unknow-server-cxf/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log 2>&1 || exit 1 &
	pid=$!
}
cxf_stop=tomcat_stop

mkdir -p out
trap '[[ "$pid" ]] && kill -9 $pid' EXIT

${1}_start
sleep 10
$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jport=8080 -Jout=out/$1.csv
${1}_stop
sleep 10
