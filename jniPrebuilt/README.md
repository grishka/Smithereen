# libvips JNI wrapper

These are prebuilt binaries of the JNI-wrapped libvips image manipulation library. Pick one that matches your operating system and CPU architecture or build your own.

If running on a server, place the library in the same directory as the jar file (`/opt/smithereen` if using the provided systemd service).

If running on a development machine, you need to specify the path where the JNI library is via an argument to java — that's "VM Options" in configuration settings in IDEA: `-Djava.library.path=$ProjectFileDir$/jniPrebuilt/darwin-x86_64`

## Building
#### Linux (Debian-based)
```bash
# Install dependencies
sudo apt-get install gcc libglib2.0-dev libpng-dev libjpeg-dev libgif-dev libwebp-dev libexpat-dev
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

#### Windows
¯\\\_(ツ)_/¯

You could try linking the JNI wrapper to one of those prebuilt DLLs available on the libvips releases page. It would probably work. If you succeed, please submit a pull request replacing this section with how you did this.
