#!/usr/bin/env bash
git pull
sh ./mvnw clean install -U
export JAVA_OPTS="-server -Xms1024M -Xmx1024M -Xss512k -XX:PermSize=256M -XX:MaxPermSize=512M -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -Dcom.sun.management.jmxremote.port=5006 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
java -classpath ./${project}/target/dependency/*:./${project}/target/classes ${application}