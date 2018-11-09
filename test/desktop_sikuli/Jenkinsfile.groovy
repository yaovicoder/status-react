node ('linux1'){
  stage('Git & Setup') {
  checkout([$class: 'GitSCM', branches: [[name: '$branch']],
  doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout']],
  submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/status-im/status-react.git']]])
 }

  stage('Tests & Report'){
  try {withCredentials([string(credentialsId: 'TESTRAIL_PASS', variable: 'TESTRAIL_PASS'),
                        string(credentialsId: 'TESTRAIL_USER', variable: 'TESTRAIL_USER')]){
            sh 'cd test/desktop.sikuli && python3 main.py --linux_app_url="https://status-im.ams3.digitaloceanspaces.com/StatusIm-181112-025922-d2160e-nightly.AppImage" --test_results_path}}
            finally {
            testResults: 'test/desktop.sikuli/tests/*.xml' }}
}
