FROM maven:latest
RUN mkdir -p /opt/application
WORKDIR /opt/application
RUN git clone git@git.oschina.net:wuyu15255872976/sc-generatorr.git
WORKDIR /opt/application/sc-generator
EXPOSE 8090:8090 5005:5005
VOLUME ["/tmp","/root/.m2"]
ENV JAVA_OPTS="-server -Xms512M -Xmx512M -Xss512k -XX:PermSize=256M"
CMD sh startup.sh