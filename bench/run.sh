#!/bin/bash

unknow_start() {
	time java -jar unknow-server-test/unknow-server-test-jar/target/server.jar --http-addr :8080 --https-addr :8443 --keystore store.jks --keystore-pass 123456 > logs/unknow.log &
	pid=$!
}
unknow_stop() {
	kill -9 $pid
	pid=
}
native_start() {
	chmod a+x server-native
	time ./server-native --http-addr :8080 --https-addr :8443 --keystore store.jks --keystore-pass 123456 > logs/native.log &
	pid=$!
}
native_stop() {
	kill -9 $pid
	pid=
}
tomcat_start() {
	cp unknow-server-test/unknow-server-test-tomcat/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	time $CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log || exit 1 &
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
	time $CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log || exit 1 &
	pid=$!
}
cxf_stop=tomcat_stop

mkdir -p out
trap '[[ "$pid" ]] && kill -9 $pid' EXIT

keytool -genkey -alias server -keyalg RSA -validity 365 -keystore store.jks -storepass 123456 -storetype JKS -dname "C=FR"

${1}_start
sleep 10
echo -e "\nWarming up"
$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jt=20 -Jport=8080
sleep 10
echo -e "\nTesting.."
$JMETER -n -t bench/test.jmx -Jhost=127.0.0.1 -Jt=60 -Jc=10 -Jport=8080 -l out/$1.jtl

echo -e "\n launch http2 bench"
h2load -c 10 -t 10 -m 10 -D 60 --warm-up-time=10 http://127.0.0.1:8080/test --log-file=out/$1.h2

${1}_stop
sleep 10
