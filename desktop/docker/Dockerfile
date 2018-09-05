FROM ubuntu:16.04 AS qt_build

RUN apt-get update && apt-get install -y \
      git libx11-xcb1 libxss1 libasound2 libfontconfig1 libdbus-1-3 && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

ADD qt-opensource-linux-x64-5.9.1.run /tmp/
RUN git clone --depth=1 https://github.com/benlau/qtci.git /tmp/qtci && \
    /tmp/qtci/bin/extract-qt-installer /tmp/qt-opensource-linux-x64-5.9.1.run /opt/qt && \
    cd /opt/qt && rm -fr Examples Docs Tools 5.9.1/android_armv7

FROM ubuntu:16.04

COPY --from=qt_build /opt/qt /opt/qt
RUN ln -s /opt/qt/5.9.1/gcc_64/mkspecs /usr/local/mkspecs && \
    ln -s /opt/qt/5.9.1/gcc_64/plugins /usr/local/plugins && \
    ln -s /opt/qt/5.9.1/gcc_64 /opt/qt59

ENV LANG en_US.UTF-8
ENV LC_ALL en_US.UTF-8
ENV LANGUAGE en_US.UTF-8

ENV LEIN_HOME /var/tmp/lein
ENV NPM_CONFIG_CACHE /var/tmp/npm
#ENV NPM_CONFIG_PREFIX /var/lib/npm

# We have to do this because Jenkins doens't let us
# https://issues.jenkins-ci.org/browse/JENKINS-49076
ENV PATH /opt/qt59/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

RUN apt-get update && apt-get install -y curl software-properties-common && \
    curl -sL https://deb.nodesource.com/setup_8.x | bash - && \
    curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - && \
    echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list && \
    add-apt-repository -y ppa:longsleep/golang-backports && \
    apt-get update && \
    apt-get install -y wget git unzip golang-go nodejs yarn \
      apt-transport-https locales openjdk-8-jdk-headless \
      cmake extra-cmake-modules build-essential gcc g++ \
      libx11-xcb1 libxss1 libasound2 libgl-dev && \
    locale-gen en_US.UTF-8 && \
    npm install -g npm@5.5.1 && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN curl -sL https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/bin/lein && \
    chmod +x /usr/bin/lein && /usr/bin/lein version

RUN cd /tmp && \
    git clone --depth=1 https://github.com/status-im/react-native-desktop.git && \
    cp -r /tmp/react-native-desktop/react-native-cli /opt && \
    cd /opt/react-native-cli && \
    npm update && npm install -g && \
    cd && rm -r /tmp/react-native-desktop

# These are the UID and GID values used by Jenkins
RUN addgroup --gid 1002 jenkins && \
    adduser --shell /bin/bash \
      --disabled-password --gecos "" \
      --uid 1001 --gid 1002 \
      --home /var/tmp/jenkins jenkins

LABEL source="https://github.com/status-im/status-react/tree/develop/desktop/docker"
LABEL description="Image for building Linux Desktop version of Status app."
LABEL maintainer="jakub@status.im"
