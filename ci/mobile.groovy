def installJSDeps() {
    def attempt = 1
    def maxAttempts = 10
    def installed = false
    while (!installed && attempt <= maxAttempts) {
        println "#${attempt} attempt to install npm deps"
        sh 'npm install'
        installed = fileExists('node_modules/web3/index.js')
        attemp = attempt + 1
    }
}

def gitPrep() {
  sh 'git fetch --tags'
  sh 'rm -rf node_modules'
  sh 'cp .env.nightly .env'
  sh 'scripts/prepare-for-platform.sh mobile'
}

def uploadArtifact() {
  def artifact_dir = pwd() + '/android/app/build/outputs/apk/release/'
  println (artifact_dir + 'app-release.apk')
  def artifact = (artifact_dir + 'app-release.apk')
  def server = Artifactory.server('artifacts')
  shortCommit = sh(
    returnStdout: true,
    script: 'git rev-parse HEAD'
  ).trim().take(6)
  def filename = "im.status.ethereum-${shortCommit}-n-fl.apk"
  def newArtifact = (artifact_dir + filename)
  sh "cp ${artifact} ${newArtifact}"
  def uploadSpec = '{ "files": [ { "pattern": "*apk/release/' + filename + '", "target": "nightlies-local" }]}'
  def buildInfo = server.upload(uploadSpec)
  return 'http://artifacts.status.im:8081/artifactory/nightlies-local/' + filename
}

def prepDeps() {
  stage('Prep deps') {
    gitPrep()
    version = readFile("${env.WORKSPACE}/VERSION").trim()
    installJSDeps()
    sh 'mvn -f modules/react-native-status/ios/RCTStatus dependency:unpack'
    dir('ios') {
      sh 'pod install'
    }
  }
}

def compileAndroid() {
  stage('Compile') {
    withCredentials([
      string(credentialsId: "SUPPLY_JSON_KEY_DATA", variable: 'GOOGLE_PLAY_JSON_KEY'),
      string(credentialsId: "SLACK_URL", variable: 'SLACK_URL')
    ]) {
      sh ('bundle exec fastlane android nightly')
    }
  }
}

def tagBuild() {
  stage('Tag Build') {
    withCredentials([[
      $class: 'UsernamePasswordMultiBinding',
      credentialsId: 'status-im-auto',
      usernameVariable: 'GIT_USER',
      passwordVariable: 'GIT_PASS'
    ]]) {
      build_no = sh(
        returnStdout: true,
        script: './scripts/build_no.sh --increment'
      ).trim()
    }
  }
}

def runTests() {
  stage('Tests') {
    sh 'lein test-cljs'
  }
}

def leinBuild() {
  stage('Build') {
    sh 'lein prod-build'
  }
}

def buildAndroid() {
  stage('Build (Android)') {
    dir('android') {
      sh './gradlew react-native-android:installArchives'
      sh './gradlew assembleRelease'
    }
  }

  stage('Build (Android) for e2e tests') {
    dir('android') {
      sh """
        mv app/build/outputs/apk/release/app-release.apk \\
           app/build/outputs/apk/release/app-release.original.apk
      """
      env.ENVFILE=".env.e2e"
      sh './gradlew assembleRelease'
    }
  }
}

def compileiOS() {
  stage('Compile') {
    withCredentials([
      string(credentialsId: "SLACK_URL", variable: 'SLACK_URL'),
      string(credentialsId: "slave-pass-${env.NODE_NAME}", variable: 'KEYCHAIN_PASSWORD'),
      string(credentialsId: 'FASTLANE_PASSWORD', variable: 'FASTLANE_PASSWORD'),
      string(credentialsId: 'APPLE_ID', variable: 'APPLE_ID'),
      string(credentialsId: 'fastlane-match-password', variable:'MATCH_PASSWORD')
    ]) {
      sh "plutil -replace CFBundleShortVersionString  -string ${version} ios/StatusIm/Info.plist"
      sh "plutil -replace CFBundleVersion -string ${build_no} ios/StatusIm/Info.plist"
      sh 'fastlane ios nightly'
    }
  }
}

return this
