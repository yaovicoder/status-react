FROM ubuntu:16.04

ENV LANG en_US.UTF-8
ENV LC_ALL en_US.UTF-8
ENV LANGUAGE en_US.UTF-8
ENV NPM_CONFIG_CACHE /tmp/npm

RUN apt-get update && apt-get install -y wget curl

RUN cd /tmp/ && \
    wget https://download.qt.io/archive/qt/5.9/5.9.1/qt-opensource-linux-x64-5.9.1.run && \
    echo "b8dd904894ac6b09076b5f61d8b9d71a  qt-opensource-linux-x64-5.9.1.run" > qt.md5 && \
    md5sum --check qt.md5

RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -

RUN apt-get install -y \
      git unzip nodejs build-essential openjdk-8-jdk-headless \
      extra-cmake-modules libx11-xcb1 libxss1 libasound2

RUN curl -sL https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/bin/lein && \
    chmod +x /usr/bin/lein && /usr/bin/lein version

RUN git clone https://github.com/benlau/qtci.git /tmp/qtci
RUN /tmp/qtci/bin/extract-qt-installer /tmp/qt-opensource-linux-x64-5.9.1.run /opt/qt

RUN ln -s /opt/qt/5.9.1/gcc_64/mkspecs /usr/local/mkspecs && \
    ln -s /opt/qt/5.9.1/gcc_64/plugins /usr/local/plugins && \
    ln -s /opt/qt/5.9.1/gcc_64 /opt/qt59

RUN cd /tmp && git clone https://github.com/status-im/react-native-desktop.git
RUN cd /tmp/react-native-desktop/react-native-cli && \
    npm update && npm install -g

RUN apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

LABEL source="https://github.com/status-im/infra-utils"
LABEL description="Image for building Linux Desktop version of Status app."
LABEL maintainer="jakub@status.im"
