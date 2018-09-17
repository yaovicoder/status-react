common = load 'ci/common.groovy'

packageFolder = './StatusImPackage'
external_modules_dir = [
  'node_modules/react-native-i18n/desktop',
  'node_modules/react-native-config/desktop',
  'node_modules/react-native-fs/desktop',
  'node_modules/react-native-http-bridge/desktop',
  'node_modules/react-native-webview-bridge/desktop',
  'node_modules/react-native-keychain/desktop',
  'node_modules/react-native-securerandom/desktop',
  'modules/react-native-status/desktop',
  'node_modules/google-breakpad',
]

external_fonts = [
  '../../../../../resources/fonts/SF-Pro-Text-Regular.otf',
  '../../../../../resources/fonts/SF-Pro-Text-Medium.otf',
  '../../../../../resources/fonts/SF-Pro-Text-Light.otf',
]

def cleanupBuild() {
  sh """
    rm -rf ${packageFolder}
  """
  sh 'make clean'
}

def cleanupAndDeps() {
  cleanupBuild()
  sh 'cp .env.jenkins .env'
  sh 'lein deps'
  common.installJSDeps('desktop')
}

def slackNotify(message, color = 'good') {
  slackSend(
    color: color,
    channel: '#jenkins-desktop',
    message: "develop (${env.CHANGE_BRANCH}) ${message} ${env.BUILD_URL}"
  )
}

def buildClojureScript() {
  sh 'make prod-build-desktop'
  sh './scripts/create-desktop-linux-package.sh buildClojureScript'
}

def uploadArtifact(filename) {
  def domain = 'ams3.digitaloceanspaces.com'
  def bucket = 'status-im-desktop'
  withCredentials([usernamePassword(
    credentialsId: 'digital-ocean-access-keys',
    usernameVariable: 'DO_ACCESS_KEY',
    passwordVariable: 'DO_SECRET_KEY'
  )]) {
    sh """
      s3cmd \\
        --acl-public \\
        --host='${domain}' \\
        --host-bucket='%(bucket)s.${domain}' \\
        --access_key=${DO_ACCESS_KEY} \\
        --secret_key=${DO_SECRET_KEY} \\
        put ${filename} s3://${bucket}/
    """

  }
  def url = "https://${bucket}.${domain}/${filename}"
  return url
}

/* MAIN --------------------------------------------------*/

def prepDeps() {
  common.doGitRebase()
  cleanupAndDeps()
}

def compileLinux() {
  /* add path for QT installation binaries */
  env.QT_PATH="/opt/qt59/bin"
  sh './scripts/create-desktop-linux-package.sh compileLinux'
}

def bundleLinux(type = 'nightly') {
  def pkg

  sh './scripts/create-desktop-linux-package.sh bundleLinux'
  dir(packageFolder) {
    pkg = common.pkgFilename(type, 'AppImage')
    sh "mv ../StatusIm-x86_64.AppImage ${pkg}"
  }
  return "${packageFolder}/${pkg}".drop(2)
}

def compileMacOS() {
  /* add path for QT installation binaries */
  env.QT_PATH = "/Users/administrator/qt/5.9.1"
  dir('desktop') {
    sh 'rm -rf CMakeFiles CMakeCache.txt cmake_install.cmake Makefile'
    sh """
      cmake -Wno-dev \\
        -DCMAKE_BUILD_TYPE=Release \\
        -DEXTERNAL_MODULES_DIR='${external_modules_dir.join(";")}' \\
        -DDESKTOP_FONTS='${external_fonts.join(";")}' \\
        -DJS_BUNDLE_PATH='${workspace}/${packageFolder}/StatusIm.jsbundle' \\
        -DCMAKE_CXX_FLAGS:='-DBUILD_FOR_BUNDLE=1'
    """
    sh 'make'
  }
}

def bundleMacOS(type = 'nightly') {
  def pkg = common.pkgFilename(type, 'dmg')
  dir(packageFolder) {
    sh 'curl -L -O "https://github.com/status-im/StatusAppFiles/raw/PR5702/Status.app.zip"'
    sh 'unzip Status.app.zip'
    sh 'cp -r assets/share/assets Status.app/Contents/Resources'
    sh 'ln -sf ../Resources/assets ../Resources/ubuntu-server ../Resources/node_modules ' +
            'Status.app/Contents/MacOS'
    sh 'chmod +x Status.app/Contents/Resources/ubuntu-server'
    sh 'cp ../desktop/bin/StatusIm Status.app/Contents/MacOS/Status'
    sh 'cp ../desktop/reportApp/reportApp Status.app/Contents/MacOS'
    sh "cp ../deployment/env  Status.app/Contents/Resources"
    sh 'ln -sf ../Resources/env Status.app/Contents/MacOS/env'
    sh 'cp -f ../deployment/macos/qt-reportApp.conf Status.app/Contents/Resources'
    sh 'ln -sf ../Resources/qt-reportApp.conf Status.app/Contents/MacOS/qt.conf'
    sh 'install_name_tool -add_rpath "@executable_path/../Frameworks" ' +
            '-delete_rpath "/Users/administrator/qt/5.9.1/clang_64/lib" ' +
            'Status.app/Contents/MacOS/reportApp'
    sh 'cp -f ../deployment/macos/Info.plist Status.app/Contents'
    sh 'cp -f ../deployment/macos/status-icon.icns Status.app/Contents/Resources'
    sh """
      macdeployqt Status.app -verbose=1 \\
        -qmldir='${workspace}/node_modules/react-native/ReactQt/runtime/src/qml/'
    """
    sh 'rm -f Status.app.zip'

    withCredentials([
      string(credentialsId: 'desktop-gpg-outer-pass', variable: 'GPG_PASS_OUTER'),
      string(credentialsId: 'desktop-gpg-inner-pass', variable: 'GPG_PASS_INNER'),
      string(credentialsId: 'desktop-keychain-pass', variable: 'KEYCHAIN_PASS')
    ]) {
      sh '../scripts/sign-macos-pkg.sh Status.app ../deployment/macos/macos-developer-id.keychain-db.gpg'
      sh "../node_modules/appdmg/bin/appdmg.js ../deployment/macos/status-dmg.json ${pkg}"
      sh "../scripts/sign-macos-pkg.sh ${pkg} ../deployment/macos/macos-developer-id.keychain-db.gpg"
    }
  }
  return "${packageFolder}/${pkg}".drop(2)
}

return this
