set JAVA_OPTS="-server -Xms1024M -Xmx1024M -Xss512k -XX:PermSize=256M"
cmd /c mvnw.cmd clean package -U
set port=8080
echo %port% >Application.port
java -jar ./target/application.jar -h
java -jar ./target/application.jar -httpPort=%port% -uriEncoding utf-8 -extractDirectory .\target\tomcat