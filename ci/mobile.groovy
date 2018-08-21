common = load 'ci/common.groovy'
ios = load 'ci/ios.groovy'
android = load 'ci/android.groovy'

def prepDeps() {
  sh 'rm -rf node_modules'
  sh 'cp .env.nightly .env'
  common.installJSDeps('mobile')
  sh 'mvn -f modules/react-native-status/ios/RCTStatus dependency:unpack'
  /* generate ios/StatusIm.xcworkspace */
  dir('ios') {
    sh 'pod install'
  }
}

def runTests() {
  sh 'lein test-cljs'
}

def leinBuild() {
  sh 'lein prod-build'
}

return this
