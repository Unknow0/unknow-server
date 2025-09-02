#!/bin/bash

unknow_start() {
	java -jar unknow-server-test/unknow-server-test-jar/target/server.jar --shutdown :8009 --http-addr :8080 --https-addr :8443 --keystore store.jks --keypass 123456 > logs/unknow.log 2>&1 &
	pid=$!
}
unknow_stop() {
	echo 'shutdown' | nc 127.0.0.1 8009
}
native_start() {
	chmod a+x server-native
	./server-native --shutdown :8009 --http-addr :8080 --https-addr :8443 --keystore store.jks --keypass 123456 > logs/native.log 2>&1 &
	pid=$!
}
native_stop() {
	echo 'shutdown' | nc 127.0.0.1 8009
}
tomcat_start() {
	cp unknow-server-test/unknow-server-test-tomcat/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log 2>&1 || exit 1 &
	pid=$!
}
tomcat_stop() {
	$CATALINA_HOME/bin/shutdown.sh
}

cxf_start() {
	cp unknow-server-test/unknow-server-test-cxf/target/*.war $CATALINA_HOME/webapps/ROOT.war || exit 1
	$CATALINA_HOME/bin/catalina.sh run > logs/tomcat.log 2>&1 || exit 1 &
	pid=$!
}
cxf_stop() { 
	$CATALINA_HOME/bin/shutdown.sh
 }

mkdir -p out logs
trap '[[ "$pid" ]] && kill -9 $pid' EXIT

keytool -genkey -alias server -keyalg RSA -validity 365 -keystore store.jks -storepass 123456 -storetype JKS -keypass 123456 -dname "C=FR"

${1}_start
sleep 10
echo -e "\nWarming up"
bash bench/tests.sh 127.0.0.1  1  50000

sleep 10
echo -e "\nTesting.."
bash bench/tests.sh 127.0.0.1 10 1000000 out/$1

${1}_stop &
sleep 10
kill -9 $(jobs -p) || true
