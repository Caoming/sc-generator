#!/usr/bin/env bash
export JAVA_OPTS="-server -Xms1024M -Xmx1024M -Xss512k -XX:PermSize=256M -XX:-HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof"
git pull;
sh ./mvnw clean install -U -Dmaven.test.skip=true;
sh ./mvnw dependency:copy-dependencies
sh ./shutdown.sh
java -classpath ./${project}/target/dependency/*:./${project}/target/classes ${application} --spring.profiles.active=prod