# Smithereen

Federated, ActivityPub-compatible social network with friends, walls, and (*at some point in the future*) groups.

**At the moment, this is very far from being production-ready. Things may and likely will break.**

## Building/installation

This will be easier when (if?) the project reaches a usable state.

1. Install and configure MySQL 5
2. Install maven if you don't have it already
3. Build the jar by running `mvn package`
4. Build the native library ([libvips](https://github.com/libvips/libvips) and JNI bindings), see below
5. Fill in the config file, see a commented example [here](examples/config.properties)
6. Create a new MySQL database and initialize it with the [schema](schema.sql) using a command (`mysql -p smithereen < schema.sql`) or any GUI like phpMyAdmin
7. Create an invite code for the first user using this SQL query: `INSERT INTO signup_invitations (signups_remaining) VALUES (1)`
8. (if running on a server) set up a service in your OS to run Smithereen as a daemon (the jar expects one command-line argument — path to the config file, e.g. `java -jar smithereen.jar /etc/smithereen/config.properties`)
9. (if running on a server) set up your web server to proxy everything except `/s/` and `/.well-known/acme-challenge` to 127.0.0.1:4567 or whichever port you set in the config, [example for nginx](examples/nginx.conf)
10. Navigate to either localhost:4567 (if developing) or your server address and register your account, using 00000000000000000000000000000000 (32 zeros) as your invite code
11. Optionally, change `access_level` to 3 in the `accounts` table to grant yourself admin access

## Building the JNI library

This step depends on your operating system.

#### Linux (Debian-based)
```bash
# Install dependencies
sudo apt-get install gcc libglib20-dev libpng-dev libjpeg-dev libgif-dev libwebp-dev libexpat-dev
# Download, unpack and build libvips
wget https://github.com/libvips/libvips/releases/download/v8.8.4/vips-8.8.4.tar.gz
tar -xzvf vips-8.8.4.tar.gz
cd vips-8.8.4
./configure
make -j4
sudo make install
cd ..
# Build the JNI library
g++ path/to/libvips_jni.cpp -o libvips_jni.so -lvips -lpng -ljpeg -lglib-2.0 -lgobject-2.0 -lgmodule-2.0 -lgif -lwebp -lwebpmux -lwebpdemux -lexpat -shared `pkg-config --cflags-only-I glib-2.0` -I/usr/lib/jvm/java-11-openjdk-amd64/include -I/usr/lib/jvm/java-11-openjdk-amd64/include/linux -std=c++11 -fPIC
# Install the JNI library
sudo mkdir -p /usr/lib/jni
sudo cp libvips_jni.so /usr/lib/jni/
```

#### Mac OS
```bash
# Install dependencies
brew install libpng webp giflib jpeg glib expat
# Download, unpack and build libvips
curl https://github.com/libvips/libvips/releases/download/v8.8.4/vips-8.8.4.tar.gz -o vips-8.8.4.tar.gz
tar -xzvf vips-8.8.4.tar.gz
cd vips-8.8.4
./configure
make -j4
make install
# Build the JNI library
g++ path/to/libvips_jni.cpp -o libvips_jni.jnilib -Ivips-8.8.4/libvips/include -Ivips-8.8.4/cplusplus/include -I/usr/local/Cellar/glib/2.58.1/lib/glib-2.0/include -I/usr/local/Cellar/glib/2.58.1/include/glib-2.0 -lvips -lpng16 -ljpeg -lglib-2.0 -lgobject-2.0 -lgmodule-2.0 -lgif -lwebp -lwebpmux -lwebpdemux -lexpat -shared -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin/"
```
You then need to specify the path where the JNI library is via an argument to java — that's "VM Options" in configuration settings in IDEA: `-Djava.library.path=$ProjectFileDir$/local` (you aren't going to be running a full server on your Mac, are you?)

#### Windows
¯\\\_(ツ)_/¯

You could try linking the JNI wrapper to one of those prebuilt DLLs available on the libvips releases page. It would probably work. If you succeed, please submit a pull request replacing this section with how you did this.
