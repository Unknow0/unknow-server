#!/bin/sh

# install dependencies
VER=$(wget -q -O - https://dlcdn.apache.org/tomcat/tomcat-9/ | grep '<a href="v9' | sed 's:.*>v\(9[^</]*\)/</a.*:\1:' | sort -rV | head -1)
mkdir -p tomcat
wget -O - https://dlcdn.apache.org/tomcat/tomcat-9/v$VER/bin/apache-tomcat-$VER.tar.gz | tar xz --strip-components=1 -C tomcat
rm -r tomcat/webapps/*
VER=$(wget -q -O - https://archive.apache.org/dist/jmeter/binaries/ | grep -Po '(?<=>apache-jmeter-).*(?=.tgz)' | sort -rV | head -1)
mkdir -p jmeter
wget -O -  https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-$VER.tgz | tar xz --strip-components=1 -C jmeter

# avoid goiing out of ephemeral port
sudo sysctl net.ipv4.tcp_tw_reuse=1
