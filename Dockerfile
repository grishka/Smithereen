FROM maven:3.6.3-openjdk-15 as builder

WORKDIR /usr/src/app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
RUN mvn package
RUN java LibVipsDownloader.java

FROM openjdk:15-buster

SHELL ["bash", "-c"]
RUN mkdir -p /opt/smithereen
WORKDIR /opt/smithereen
COPY --from=builder /usr/src/app/target/smithereen-jar-with-dependencies.jar /opt/smithereen/smithereen.jar
COPY --from=builder /usr/src/app/*.so /opt/smithereen/
RUN echo -e '#!/bin/bash\njava -jar /opt/smithereen/smithereen.jar /usr/local/etc/config.properties init_admin' > smithereen-init-admin && chmod +x smithereen-init-admin

EXPOSE 4567
ENTRYPOINT java -Djna.library.path=/opt/smithereen -jar /opt/smithereen/smithereen.jar /usr/local/etc/config.properties