FROM maven:3-jdk-11

EXPOSE 4567

# Install dev utils

RUN apt-get update
RUN apt-get install -y gcc g++ make libglib2.0-dev libpng-dev libjpeg-dev libgif-dev libwebp-dev libexpat-dev

# Build and install libvips 8.8.4 (version **does** matter)

WORKDIR /usr/src/libvips
RUN wget https://github.com/libvips/libvips/releases/download/v8.8.4/vips-8.8.4.tar.gz -O - | tar xzf -

WORKDIR /usr/src/libvips/vips-8.8.4
RUN sh configure
RUN make -j4
RUN make install

# Compile JNI library

WORKDIR /usr/src/app/src/main
COPY ./src/main/jni .
RUN g++ libvips_jni.cpp -o /tmp/libvips_jni.so -lvips -lpng -ljpeg -lglib-2.0 -lgobject-2.0 -lgmodule-2.0 -lgif -lwebp -lwebpmux -lwebpdemux -lexpat -shared `pkg-config --cflags-only-I glib-2.0` -I/usr/local/openjdk-11/include -I/usr/local/openjdk-11/include/linux -std=c++11 -fPIC
RUN mkdir -p /usr/local/lib/jni
RUN cp /tmp/libvips_jni.so /usr/local/lib/jni

# Build JAR

WORKDIR /usr/src/app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
RUN mvn package

ENTRYPOINT java -Djava.library.path=/usr/local/lib/jni -jar /usr/src/app/target/smithereen-jar-with-dependencies.jar /usr/local/etc/config.properties
