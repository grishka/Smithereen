FROM maven:3.8.3-eclipse-temurin-17 as builder

WORKDIR /usr/src/app
COPY . .
RUN mvn package -DskipTests=true
RUN java LibVipsDownloader.java

FROM eclipse-temurin:17-jdk

SHELL ["bash", "-c"]
RUN mkdir -p /opt/smithereen
WORKDIR /opt/smithereen
COPY --from=builder /usr/src/app/target/smithereen-jar-with-dependencies.jar /opt/smithereen/smithereen.jar
COPY --from=builder /usr/src/app/*.so /opt/smithereen/
RUN echo -e '#!/bin/bash\njava -jar /opt/smithereen/smithereen.jar /usr/local/etc/config.properties init_admin' > smithereen-init-admin && chmod +x smithereen-init-admin

EXPOSE 4567
ENTRYPOINT java -Djna.library.path=/opt/smithereen -jar /opt/smithereen/smithereen.jar /usr/local/etc/config.properties