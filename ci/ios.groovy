def compile() {
  version = readFile("${env.WORKSPACE}/VERSION").trim()
  build_no = common.tagBuild()
  withCredentials([
    string(credentialsId: 'SLACK_URL', variable: 'SLACK_URL'),
    string(credentialsId: "slave-pass-${env.NODE_NAME}", variable: 'KEYCHAIN_PASSWORD'),
    string(credentialsId: 'FASTLANE_PASSWORD', variable: 'FASTLANE_PASSWORD'),
    string(credentialsId: 'APPLE_ID', variable: 'APPLE_ID'),
    string(credentialsId: 'fastlane-match-password', variable:'MATCH_PASSWORD')
  ]) {
    sh "plutil -replace CFBundleShortVersionString  -string ${version} ios/StatusIm/Info.plist"
    sh "plutil -replace CFBundleVersion -string ${build_no} ios/StatusIm/Info.plist"
    sh 'fastlane ios nightly'
  }
  def pkg = "StatusIm-${GIT_COMMIT.take(6)}.ipa"
  sh "cp status-adhoc/StatusIm.ipa ${pkg}"
  return pkg
}

def uploadToDiawi() {
  withCredentials([
    string(credentialsId: 'diawi-token', variable: 'DIAWI_TOKEN'),
    string(credentialsId: 'GIT_HUB_TOKEN', variable: 'GITHUB_TOKEN'),
    string(credentialsId: 'SLACK_JENKINS_WEBHOOK', variable: 'SLACK_URL')
  ]) {
    sh 'fastlane android upload_diawi'
  }
  diawiUrl = readFile "${env.WORKSPACE}/fastlane/diawi.out"
  return diawiUrl
}

return this
