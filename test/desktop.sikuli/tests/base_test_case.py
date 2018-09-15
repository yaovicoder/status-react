import org.sikuli.script.SikulixForJython
from sikuli import *
from subprocess import check_output


class BaseTestCase:

    Settings.ActionLogs = 0

    def setup_method(self, method):
        # check_output(['hdiutil', 'attach', 'nightly.dmg'])
        # check_output(['cp', '-rf', '/Volumes/Status/Status.app', '/Applications/'])
        # check_output(['hdiutil', 'detach', '/Volumes/Status/'])
        # import time
        # time.sleep(10)
        openApp('Status.app')

    def teardown_method(self, method):
        closeApp('Status.app')
        # for dir in '/Applications/Status.app', '/Library/Application\ Support/StatusIm', \
        #            '/Users/yberdnyk/Library/Caches/StatusIm':
        #     outcome = subprocess.check_output(['echo', <password>, '|', 'sudo', '-S', 'rm', '-rf', dir])
        #     print(outcome)
