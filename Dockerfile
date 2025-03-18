FROM maven:3.9.5-eclipse-temurin-21 AS builder

WORKDIR /usr/src/app
COPY . .
ARG MAVEN_OPTS
RUN mvn package -Dmaven.test.skip=true
RUN java LibVipsDownloader.java

FROM eclipse-temurin:21-jdk

SHELL ["bash", "-c"]
RUN mkdir -p /opt/smithereen
WORKDIR /opt/smithereen
COPY --from=builder /usr/src/app/target/smithereen.jar /opt/smithereen/smithereen.jar
COPY --from=builder /usr/src/app/target/lib /opt/smithereen/lib
COPY --from=builder /usr/src/app/*.so /opt/smithereen/
RUN echo -e '#!/bin/bash\njava -jar /opt/smithereen/smithereen.jar /usr/local/etc/config.properties init_admin' > smithereen-init-admin && chmod +x smithereen-init-admin

EXPOSE 4567
ENTRYPOINT java -Djna.library.path=/opt/smithereen --module-path /opt/smithereen/smithereen.jar:/opt/smithereen/lib -m smithereen.server/smithereen.SmithereenApplication /usr/local/etc/config.properties
