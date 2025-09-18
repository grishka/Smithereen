FROM amazonlinux:2

# Build dependencies
RUN \
  yum update -y && \
  amazon-linux-extras install epel -y && \
  yum install -y --setopt=tsflags=nodocs \
    --exclude=gcc \
    autoconf \
    automake \
    bzip2 \
    cmake3 \
    curl \
    gcc10 \
    gcc10-binutils \
    gcc10-c++ \
    gettext \
    git \
    gperf \
    jq \
    make \
    nasm \
    openssl-devel \
    patch \
    pkgconfig \
    python3 \
    tar \
    xz \
    && \
  ln -s /usr/bin/gcc10-gcc /usr/bin/cc && \
  ln -s /usr/bin/gcc10-g++ /usr/bin/c++ && \
  ln -s /usr/bin/gcc10-ar /usr/bin/ar && \
  ln -s /usr/bin/gcc10-nm /usr/bin/nm && \
  ln -s /usr/bin/gcc10-ld.bfd /usr/bin/ld && \
  ln -s /usr/bin/gcc10-strip /usr/bin/strip && \
  ln -s /usr/bin/gcc10-ranlib /usr/bin/ranlib && \
  ln -s /usr/bin/gcc10-readelf /usr/bin/readelf && \
  ln -sf /usr/bin/gcc10-ld.bfd /usr/bin/gcc10-ld && \
  rm /usr/bin/gcc10-ld.gold && \
  ln -s /usr/bin/cmake3 /usr/bin/cmake && \
  pip3 install meson==1.7.2 ninja

RUN if [ "$(uname -m)" = "x86_64" ]; then yum install -y --setopt=tsflags=nodocs nasm; fi
RUN if [ "$(uname -m)" = "aarch64" ]; then ln -s /usr/bin/gcc10-as /usr/bin/as; fi

