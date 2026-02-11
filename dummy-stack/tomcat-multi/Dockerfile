FROM tomcat:9.0-jdk21-temurin

RUN rm -rf /usr/local/tomcat/webapps/* \
    && cp -R /usr/local/tomcat/webapps.dist/ROOT /usr/local/tomcat/webapps/ROOT \
    && cp -R /usr/local/tomcat/webapps.dist/manager /usr/local/tomcat/webapps/manager

COPY tomcat/conf/catalina-users.xml /usr/local/tomcat/conf/tomcat-users.xml
COPY tomcat/conf/manager-context.xml /usr/local/tomcat/webapps/manager/META-INF/context.xml

RUN mkdir -p /opt/tomcat-template \
    && cp -R /usr/local/tomcat/conf /opt/tomcat-template/conf \
    && cp -R /usr/local/tomcat/webapps /opt/tomcat-template/webapps \
    && mkdir -p /opt/tomcats/instances

COPY tomcat-multi/run-multi-tomcat.sh /usr/local/bin/run-multi-tomcat.sh
RUN chmod +x /usr/local/bin/run-multi-tomcat.sh

EXPOSE 8081 8082 8083
ENTRYPOINT ["/usr/local/bin/run-multi-tomcat.sh"]
