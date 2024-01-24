#!/bin/sh

echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">'
echo '	<modelVersion>4.0.0</modelVersion>'
echo '	<parent>'
echo '		<groupId>unknow.server</groupId>'
echo '		<artifactId>unknow-server</artifactId>'
echo '		<version>0.0.1-SNAPSHOT</version>'
echo '	</parent>'
echo '	<artifactId>unknow-server-bom</artifactId>'
echo '	<packaging>pom</packaging>'
echo ''
echo '	<dependencyManagement>'
echo '		<dependencies>'

find .. -name "pom.xml" -not -path "*/target/*" -not -path "*/unknow-server-bom/*" -exec xmllint --xpath '/*/*[local-name()="artifactId"]' {} \; |
while read d
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

