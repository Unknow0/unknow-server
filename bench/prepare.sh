#!/bin/sh

set -e

TOMCAT=10

# install dependencies
VER=$(wget -q -O - "https://dlcdn.apache.org/tomcat/tomcat-$TOMCAT/" | grep "<a href=\"v$TOMCAT." | sed "s:.*>v\($TOMCAT[^</]*\)/</a.*:\1:" | sort -rV | head -1)
mkdir -p tomcat
wget -O - https://dlcdn.apache.org/tomcat/tomcat-$TOMCAT/v$VER/bin/apache-tomcat-$VER.tar.gz | tar xz --strip-components=1 -C tomcat
rm -r tomcat/webapps/*
VER=$(wget -q -O - https://archive.apache.org/dist/jmeter/binaries/ | grep -Po '(?<=>apache-jmeter-).*(?=.tgz)' | sort -rV | head -1)
mkdir -p jmeter
wget -O -  https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-$VER.tgz | tar xz --strip-components=1 -C jmeter

# avoid going out of ephemeral port
sudo sysctl net.ipv4.tcp_tw_reuse=1
