FROM ubuntu:bionic
SHELL ["bash", "-c"]
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -y --no-install-recommends software-properties-common \
    && add-apt-repository ppa:ubuntu-toolchain-r/test \
    && apt-get update \
    && apt-get install -y --no-install-recommends make libssl-dev zlib1g-dev libbz2-dev libreadline-dev libsqlite3-dev wget ca-certificates curl llvm libncurses5-dev xz-utils tk-dev libxml2-dev libxmlsec1-dev libffi-dev liblzma-dev mecab-ipadic-utf8 git gcc-10 g++-10
RUN git clone https://github.com/yyuu/pyenv.git .pyenv
ENV PYENV_ROOT=/root/.pyenv
ENV PATH=$PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH

ENV PYTHON_VERSION=3.13.7
ENV CC=/usr/bin/gcc-10
ENV CXX=/usr/bin/g++-10

RUN set -ex \
    && curl https://pyenv.run | bash \
    && pyenv update \
    && pyenv install $PYTHON_VERSION \
    && pyenv global $PYTHON_VERSION \
    && pyenv rehash

WORKDIR /root
RUN curl --location https://github.com/Kitware/CMake/releases/download/v4.1.1/cmake-4.1.1-linux-$(uname -m).sh -o cmake_install.sh \
    && chmod +x cmake_install.sh \
    && mkdir cmake \
    && ./cmake_install.sh --skip-license --prefix=cmake \
    && rm cmake_install.sh
ENV PATH=/root/cmake/bin:$PATH
