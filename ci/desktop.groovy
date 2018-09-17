common = load 'ci/common.groovy'

packageFolder = './StatusImPackage'

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
  sh './scripts/create-desktop-package.sh buildClojureScript'
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
  qtPath='/opt/qt59'
  env.PATH="/opt/qt59/bin:${env.PATH}"
  env.QT_PATH="$qtPath"
  sh './scripts/create-desktop-package.sh compile'
}

def bundleLinux(type = 'nightly') {
  def pkg

  sh './scripts/create-desktop-package.sh bundle'
  dir(packageFolder) {
    pkg = common.pkgFilename(type, 'AppImage')
    sh "mv ../StatusIm-x86_64.AppImage ${pkg}"
  }
  return "${packageFolder}/${pkg}".drop(2)
}

def compileMacOS() {
  /* add path for QT installation binaries */
  qtPath='/Users/administrator/qt/5.9.1'
  env.PATH="$qtPath/clang_64/bin:${env.PATH}"
  env.QT_PATH="$qtPath"
  env.MACDEPLOYQT='macdeployqt'
  sh './scripts/create-desktop-package.sh compile'
}

def bundleMacOS(type = 'nightly') {
  def pkg = common.pkgFilename(type, 'dmg')
  sh './scripts/create-desktop-package.sh bundle'
  dir(packageFolder) {
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
