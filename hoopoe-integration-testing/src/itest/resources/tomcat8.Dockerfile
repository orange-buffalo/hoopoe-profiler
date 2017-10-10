FROM tomcat:8.0-jre8
RUN echo "CATALINA_OPTS=\"\$CATALINA_OPTS -javaagent:\$HOOPOE_AGENT\"" > /usr/local/tomcat/bin/setenv.sh && \
    chmod +x /usr/local/tomcat/bin/setenv.sh