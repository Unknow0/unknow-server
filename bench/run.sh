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
	cp unknow-server-test/unknow-server-test-tomcat/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
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
	cp unknow-server-test/unknow-server-test-cxf/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log 2>&1 || exit 1 &
	pid=$!
}
cxf_stop=tomcat_stop

mkdir -p out
trap '[[ "$pid" ]] && kill -9 $pid' EXIT

${1}_start
sleep 10
echo "Warming up"
$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jt=20 -Jport=8080 -Jout=/dev/null
sleep 10
echo "Testing.."
$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jt=60 -Jc=10 -Jport=8080 -Jout=out/$1.csv

h2load -c 10 -t 10 -m 10 -D 60 --warm-up-time=10 http://127.0.0.1:8080/test

${1}_stop
sleep 10
