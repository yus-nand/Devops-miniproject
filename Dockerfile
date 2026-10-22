# Image for the Policyholder Grievance Portal: the official Tomcat image plus the WAR built by Maven.
# Build the WAR first (mvn clean package), then:  docker build -t grievance-portal:1.0.0 .
ARG TOMCAT_TAG=10.1-jre17
FROM tomcat:${TOMCAT_TAG}

LABEL org.opencontainers.image.title="Policyholder Grievance Portal" \
      org.opencontainers.image.description="Grievance portal WAR on Tomcat 10.1 with Java 17" \
      org.opencontainers.image.source="https://github.com/yus-nand/Devops-miniproject"

# The label shown in the page footer and in /health. Change it with: docker run -e GP_ENV_NAME=...
ENV GP_ENV_NAME=docker

# The WAR name decides the address: /grievance-portal
COPY target/grievance-portal.war /usr/local/tomcat/webapps/grievance-portal.war

# The H2 database is a file under /usr/local/tomcat/data. Mount a volume there to keep the data
# when the container is removed:  docker run -v grievance-data:/usr/local/tomcat/data ...

EXPOSE 8080
# The base image already starts Tomcat (CMD ["catalina.sh", "run"]).
