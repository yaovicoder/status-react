FROM ubuntu:16.04 AS qt_build

RUN apt-get update && apt-get install -y \
      git libx11-xcb1 libxss1 libasound2 libfontconfig1 libdbus-1-3 && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

COPY qt-opensource-linux-x64-5.9.1.run /tmp/
RUN git clone https://github.com/benlau/qtci.git /tmp/qtci && \
    /tmp/qtci/bin/extract-qt-installer /tmp/qt-opensource-linux-x64-5.9.1.run /opt/qt && \
    cd /opt/qt && rm -fr Examples Docs Tools 5.9.1/android_armv7

FROM ubuntu:16.04

ENV LANG en_US.UTF-8
ENV LC_ALL en_US.UTF-8
ENV LANGUAGE en_US.UTF-8
ENV NPM_CONFIG_CACHE /var/tmp/npm
ENV LEIN_HOME /var/tmp/lein
ENV NODE_PATH ./node_modules:/var/lib/node_modules
ENV NPM_CONFIG_PREFIX /var/lib/node_modules

RUN apt-get update && apt-get install -y curl && \
    curl -sL https://deb.nodesource.com/setup_8.x | bash - && \
    apt-get install -y wget git unzip nodejs apt-transport-https \
      openjdk-8-jdk-headless extra-cmake-modules locales \
      build-essential libx11-xcb1 libxss1 libasound2 && \
    locale-gen en_US.UTF-8 && \
    npm install -g npm@5.5.1 && \
    ln -fs /var/lib/node_modules/bin/npm /usr/bin/npm && \
    ln -fs /var/lib/node_modules/bin/npx /usr/bin/npx && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN curl -sL https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/bin/lein && \
    chmod +x /usr/bin/lein && /usr/bin/lein version

# yarn might be necessary in the future
#RUN curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add - && \
#    echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list && \
#    apt-get update && apt-get install yarn && \
#    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

#RUN git clone https://github.com/status-im/status-react /tmp/status-react && \
#    cd /tmp/status-react && scripts/prepare-for-platform.sh desktop && \
#    npm install -g && \
#    cd && rm -r /tmp/status-react 

RUN npm install --silent -g https://github.com/status-im/react-native.git

RUN cd /tmp && \
    git clone https://github.com/status-im/react-native-desktop.git && \
    cd /tmp/react-native-desktop/react-native-cli && \
    npm install -g && \
    cd && rm -r /tmp/react-native-desktop

COPY --from=qt_build /opt/qt /opt/qt
RUN ln -s /opt/qt/5.9.1/gcc_64/mkspecs /usr/local/mkspecs && \
    ln -s /opt/qt/5.9.1/gcc_64/plugins /usr/local/plugins && \
    ln -s /opt/qt/5.9.1/gcc_64 /opt/qt59

# These are the UID and GID values used by Jenkins
RUN addgroup --gid 1002 jenkins && \
    adduser --shell /bin/bash \
      --disabled-password --gecos "" \
      --uid 1001 --gid 1002 \
      --home /var/tmp/jenkins jenkins

LABEL source="https://github.com/status-im/status-react/tree/develop/desktop/docker"
LABEL description="Image for building Linux Desktop version of Status app."
LABEL maintainer="jakub@status.im"
