# libvips JNI wrapper

These are prebuilt binaries of the JNI-wrapped libvips image manipulation library. Pick one that matches your operating system and CPU architecture or build your own.

If running on a server, place the library somewhere where the JVM will be looking for it (`/opt/smithereen` if using the provided systemd service). You'll still need to install these packages:
```
libjpeg libglib2.0 libgif libpng libexpat
```

If running on a development machine, you need to specify the path where the JNI library is via an argument to java — that's "VM Options" in configuration settings in IDEA: `-Djava.library.path=$ProjectFileDir$/jniPrebuilt/darwin-x86_64`

The Linux libraries are linked statically with libvips, libwebp, libwebpdemux, and libwebpmux. The latter ones because some Linux distros provide horrendously outdated libwebp versions that libvips doesn't support. Libvips is built with support for jpeg, png, gif, and webp.

## Building your own
#### Linux (Debian-based)
Some distros offer their version of libvips, but we won't be using that because it pulls in 300 MB worth of packages in an effort to support every image-like format under the sun including stuff like SVG and PDF. This isn't good for security. If, however, you're doing this on your development machine, this is a much easier way — just install libvips from your package manager and then run `make`. Otherwise...
```bash
# Install dependencies
sudo apt-get install gcc libglib2.0-dev libpng-dev libjpeg-dev libgif-dev libexpat-dev
# Download, unpack and build libvips
wget https://github.com/libvips/libvips/releases/download/v8.10.5/vips-8.10.5.tar.gz
tar -xzvf ips-8.10.5.tar.gz
cd vips-8.10.5.tar.gz
./configure
make -j4
sudo make install
# Do the same for libwebp
wget https://storage.googleapis.com/downloads.webmproject.org/releases/webp/libwebp-1.1.0.tar.gz
tar -xvzf libwebp-1.1.0.tar.gz
cd libwebp-1.1.0
CFLAGS=-fPIC ./configure --enable-libwebpmux
make -j4
sudo make install
cd ..
# Now, build the JNI library
make
```
If you want to statically link libvips like I did here, there's unfortunately no clean and nice way of doing it, so run this atrocious command after make:
```bash
g++ *.o -o libvips_jni.so -shared -L/usr/local/lib -l:libvips.a -l:libwebp.a -l:libwebpmux.a -l:libwebpdemux.a -ljpeg -lgif -lpng -lexpat -lgobject-2.0 -lglib-2.0 -lgmodule-2.0 -lgio-2.0
```

#### Mac OS
I'll assume you won't be running a real server on your Mac, so see above about dependencies and security. If you still don't have [Homebrew](https://brew.sh/) for some reason, you need to install it first.
```bash
brew install vips
make
```
That's it.

#### Windows
¯\\\_(ツ)_/¯

You could try linking the JNI wrapper to one of those prebuilt DLLs available on the libvips releases page. It would probably work. If you succeed, please submit a pull request replacing this section with how you did this.
