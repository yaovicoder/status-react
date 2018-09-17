#!/bin/bash

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
SCRIPTPATH="$( cd "$(dirname "$0")" ; pwd -P )"
STATUSREACTPATH="$SCRIPTPATH/.."
WORKFOLDER="$STATUSREACTPATH/StatusImPackage"
DEPLOYQT="./linuxdeployqt-continuous-x86_64.AppImage"
QTBIN="$QT_PATH/bin/"
OS=$(uname -s)

external_modules_dir=( \
  'node_modules/react-native-i18n/desktop' \
  'node_modules/react-native-config/desktop' \
  'node_modules/react-native-fs/desktop' \
  'node_modules/react-native-http-bridge/desktop' \
  'node_modules/react-native-webview-bridge/desktop' \
  'node_modules/react-native-keychain/desktop' \
  'node_modules/react-native-securerandom/desktop' \
  'modules/react-native-status/desktop' \
  'node_modules/google-breakpad' \
)

external_fonts=( \
  '../../../../../resources/fonts/SF-Pro-Text-Regular.otf' \
  '../../../../../resources/fonts/SF-Pro-Text-Medium.otf' \
  '../../../../../resources/fonts/SF-Pro-Text-Light.otf' \
)

function is_macos() {
  [[ "$OS" =~ Darwin ]]
}

function is_linux() {
  [[ "$OS" =~ Linux ]]
}

function init() {
  if [ -z $QT_PATH ]; then
    echo "${RED}QT_PATH environment variable is not defined!${NC}"
    exit 1
  fi

  if is_macos; then
    if [ -z $MACDEPLOYQT ]; then
      set +e
      MACDEPLOYQT=$(which macdeployqt)
      if [ -z $MACDEPLOYQT ]; then
        echo "${RED}MACDEPLOYQT environment variable is not defined and macdeployqt executable not found in path!${NC}"
        exit 1
      fi
      set -e
    fi
  fi

  if is_macos; then
    WORKFOLDER="$STATUSREACTPATH/mac_bundle"
    DEPLOYQT="$MACDEPLOYQT"
  fi
}

function joinStrings() {
  local arr=("$@")
  printf -v var "%s;" "${arr[@]}"
  var=${var%?}
  echo ${var[@]}
}

function buildClojureScript() {
  # create directory for all work related to bundling
  rm -rf $WORKFOLDER
  mkdir -p $WORKFOLDER
  echo -e "${GREEN}Work folder created: $WORKFOLDER${NC}"
  echo ""

  # from index.desktop.js create javascript bundle and resources folder
  echo "Generating StatusIm.jsbundle and assets folder..."
  react-native bundle --entry-file index.desktop.js --bundle-output $WORKFOLDER/StatusIm.jsbundle \
                      --dev false --platform desktop --assets-dest $WORKFOLDER/assets
  echo -e "${GREEN}Generating done.${NC}"
  echo ""

  # Add path to javascript bundle to package.json
  jsBundleLine="\"desktopJSBundlePath\": \"$WORKFOLDER/StatusIm.jsbundle\""
  if grep -Fq "$jsBundleLine" "$STATUSREACTPATH/desktop_files/package.json"; then
    echo -e "${GREEN}Found line in package.json.${NC}"
  else
    # Add line to package.json just before "dependencies" line
    if is_macos; then
      sed -i '' -e "/\"dependencies\":/i\\
 \  $jsBundleLine," "$STATUSREACTPATH/desktop_files/package.json"
    else
      sed -i -- "/\"dependencies\":/i\  $jsBundleLine," "$STATUSREACTPATH/desktop_files/package.json"
    fi
    echo -e "${YELLOW}Added 'desktopJSBundlePath' line to desktop_files/package.json:${NC}"
    echo ""
  fi
}

function compile() {
  pushd desktop
    rm -rf CMakeFiles CMakeCache.txt cmake_install.cmake Makefile
    cmake -Wno-dev \
          -DCMAKE_BUILD_TYPE=Release \
          -DEXTERNAL_MODULES_DIR="$(joinStrings ${external_modules_dir[@]})" \
          -DDESKTOP_FONTS="$(joinStrings ${external_fonts[@]})" \
          -DJS_BUNDLE_PATH="$WORKFOLDER/StatusIm.jsbundle" \
          -DCMAKE_CXX_FLAGS:='-DBUILD_FOR_BUNDLE=1 -std=c++11'
    make
  popd
}

function bundleLinux() {
  # invoke linuxdeployqt to create StatusIm.AppImage
  echo "Creating AppImage..."

  pushd $WORKFOLDER
    rm -rf StatusImAppImage
    # TODO this needs to be fixed: status-react/issues/5378
    [ -f ./StatusImAppImage.zip ] || wget https://github.com/status-im/StatusAppFiles/raw/master/StatusImAppImage.zip
    unzip ./StatusImAppImage.zip
    rm -rf AppDir
    mkdir AppDir
  popd

  cp -r ./deployment/linux/usr ${WORKFOLDER}/AppDir
  cp ./deployment/env ${WORKFOLDER}/AppDir/usr/bin
  cp ./desktop/bin/StatusIm ${WORKFOLDER}/AppDir/usr/bin
  cp ./desktop/reportApp/reportApp ${WORKFOLDER}/AppDir/usr/bin
  if [ ! -f $DEPLOYQT ]; then
    wget --output-document="$DEPLOYQT" --show-progress -q https://github.com/probonopd/linuxdeployqt/releases/download/continuous/linuxdeployqt-continuous-x86_64.AppImage
    chmod a+x $DEPLOYQT
  fi

  rm -f Application-x86_64.AppImage
  rm -f StatusIm-x86_64.AppImage

  ldd ${WORKFOLDER}/AppDir/usr/bin/StatusIm
  $DEPLOYQT \
    ${WORKFOLDER}/AppDir/usr/bin/reportApp \
    -verbose=3 -always-overwrite -no-strip -no-translations -qmake="${QTBIN}/qmake" \
    -qmldir="${STATUSREACTPATH}/desktop/reportApp"

  $DEPLOYQT \
    ${WORKFOLDER}/AppDir/usr/share/applications/StatusIm.desktop \
    -verbose=3 -always-overwrite -no-strip \
    -no-translations -bundle-non-qt-libs \
    -qmake="${QTBIN}/qmake" \
    -extra-plugins=imageformats/libqsvg.so \
    -qmldir="${STATUSREACTPATH}/node_modules/react-native"

  pushd $WORKFOLDER
    ldd AppDir/usr/bin/StatusIm
    cp -r assets/share/assets AppDir/usr/bin
    cp -rf StatusImAppImage/* AppDir/usr/bin
    rm -f AppDir/usr/bin/StatusIm.AppImage
  popd

  $DEPLOYQT \
    $WORKFOLDER/AppDir/usr/share/applications/StatusIm.desktop \
    -verbose=3 -appimage -qmake="${QTBIN}/qmake"
  pushd $WORKFOLDER
    ldd AppDir/usr/bin/StatusIm
    cp -r assets/share/assets AppDir/usr/bin
    cp -rf StatusImAppImage/* AppDir/usr/bin
    rm -f AppDir/usr/bin/StatusIm.AppImage
  popd
  $DEPLOYQT \
    "$WORKFOLDER/AppDir/usr/share/applications/StatusIm.desktop" \
    -verbose=3 -appimage -qmake="${QTBIN}/qmake"
  pushd $WORKFOLDER
    ldd AppDir/usr/bin/StatusIm
    rm -rf StatusIm.AppImage
  popd

  echo -e "${GREEN}Package ready in ./StatusIm-x86_64.AppImage!${NC}"
  echo ""
}

function bundleMacOS() {
  # download prepared package with mac bundle files (it contains qt libraries, icon)
  echo "Downloading skeleton of mac bundle..."

  pushd $WORKFOLDER
    rm -rf StatusImAppImage
    # TODO this needs to be fixed: status-react/issues/5378
    [ -f ./Status.app.zip ] || curl -L -o Status.app.zip https://github.com/status-im/StatusAppFiles/raw/master/Status.app.zip
    echo -e "${GREEN}Downloading done.${NC}"
    echo ""
    unzip ./Status.app.zip
    cp -r assets/share/assets Status.app/Contents/Resources
    ln -sf ../Resources/assets ../Resources/ubuntu-server ../Resources/node_modules Status.app/Contents/MacOS
    chmod +x Status.app/Contents/Resources/ubuntu-server
    cp ../desktop/bin/StatusIm Status.app/Contents/MacOS/Status
    cp ../desktop/reportApp/reportApp Status.app/Contents/MacOS
    cp ../deployment/env  Status.app/Contents/Resources
    ln -sf ../Resources/env Status.app/Contents/MacOS/env
    cp -f ../deployment/macos/qt-reportApp.conf Status.app/Contents/Resources
    ln -sf ../Resources/qt-reportApp.conf Status.app/Contents/MacOS/qt.conf
    install_name_tool -add_rpath "@executable_path/../Frameworks" \
                      -delete_rpath "$QT_PATH/clang_64/lib" \
                      'Status.app/Contents/MacOS/reportApp'
    cp -f ../deployment/macos/Info.plist Status.app/Contents
    cp -f ../deployment/macos/status-icon.icns Status.app/Contents/Resources
    $DEPLOYQT Status.app -verbose=1 \
      -qmldir="${STATUSREACTPATH}/node_modules/react-native/ReactQt/runtime/src/qml/"
    rm -f Status.app.zip
  popd

  echo -e "${GREEN}Package ready in $WORKFOLDER/Status.app!${NC}"
  echo ""
}

function bundle() {
  if is_macos; then
    bundleMacOS
  else
    bundleLinux
  fi
}

init

if [ -z "$@" ]; then
  buildClojureScript
  compile
  bundle
else
  "$@"
fi
