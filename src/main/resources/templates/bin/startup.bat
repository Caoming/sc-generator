set JAVA_OPTS="-server -Xms1024M -Xmx1024M -Xss512k -XX:PermSize=256M"
cmd /c mvnw.cmd clean install -U
cmd /c mvnw.cmd dependency:copy-dependencies
java -classpath .\\${project}\target\dependency\*;.\\${project}\target\classes ${application}