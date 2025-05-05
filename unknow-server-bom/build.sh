#!/bin/sh

echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">'
echo '	<modelVersion>4.0.0</modelVersion>'
echo '	<parent>'
echo '		<groupId>io.github.unknow0.server</groupId>'
echo '		<artifactId>unknow-server</artifactId>'
echo '		<version>0.0.1-SNAPSHOT</version>'
echo '	</parent>'
echo '	<artifactId>unknow-server-bom</artifactId>'
echo '	<packaging>pom</packaging>'
echo ''
echo '	<name>${project.groupId}:${project.artifactId}</name>'
echo '	<description>BOM for unknow-server modules</description>'
echo '	<url>https://github.com/Unknow0/unknow-server</url>'
echo ''
echo '	<licenses>'
echo '		<license>'
echo '			<name>The Apache License, Version 2.0</name>'
echo '			<url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>'
echo '		</license>'
echo '	</licenses>'
echo '	<developers>'
echo '		<developer>'
echo '			<name>Unknow0</name>'
echo '			<email>unknow0@free.fr</email>'
echo '			<organizationUrl>https://github.com/Unknow0</organizationUrl>'
echo '		</developer>'
echo '	</developers>'
echo '	<scm>'
echo '		<connection>scm:git:git://github.com/Unknow0/unknow-server.git</connection>'
echo '		<developerConnection>scm:git:ssh://github.com:Unknow0/unknow-server.git</developerConnection>'
echo '		<url>https://github.com/Unknow0/unknow-server</url>'
echo '	</scm>'
echo ''
echo '	<dependencyManagement>'
echo '		<dependencies>'

r="$(dirname "$0")/.."
find "$r" -name "pom.xml" \
	-not -path "*/target/*" \
	-not -path "*/unknow-server-bom/*" \
	-not -path "*/unknow-server-bench/*" \
	-not -path "*/unknow-server-test/*" \
	-not -path "$r/pom.xml" \
	-exec xmllint --xpath '/*/*[local-name()="artifactId"]' {} \; | while read d
do
	echo '			<dependency>'
	echo '				<groupId>${project.groupId}</groupId>'
	echo "				$d"
	echo '				<version>${project.version}</version>'
	echo '			</dependency>'
done 

echo '		</dependencies>'
echo '	</dependencyManagement>'
echo '</project>'

