#!/usr/bin/env bash
export JAVA_OPTS="-server -Xms1024M -Xmx1024M -Xss512k -XX:PermSize=256M -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof"
port =8080
git pull;
sh ./mvnw clean package -U -Dmaven.test.skip=true;
java -jar ./target/application.jar -h
java -jar ./target/application.jar -httpPort${port} -uriEncoding utf-8 -extractDirectory ./target/tomcat
