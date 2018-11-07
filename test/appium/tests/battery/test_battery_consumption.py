import datetime
import logging
import os
from subprocess import call

import docker
import pytest
from docker.errors import NotFound

from tests import test_suite_data
from tests.base_test_case import SingleDeviceTestCase, Driver
from views.sign_in_view import SignInView


class BatteryConsumptionTestCase(SingleDeviceTestCase):

    def setup_method(self, method):
        self.start_appium_container()
        self.connect_device()
        self.driver = Driver(self.executor_local, self.capabilities_local)
        # WTF is that? Fix it in Driver#number()
        test_suite_data.current_test.testruns[-1].jobs[self.driver.session_id] = 1
        self.driver.implicitly_wait(self.implicitly_wait)
        self.reset_battery_stats()
        print("\nRunning battery test..")

    def teardown_method(self, method):
        self.generate_battery_stats()
        self.driver.quit()
        self.container.stop()

    @property
    def capabilities_local(self):
        caps = super(SingleDeviceTestCase, self).capabilities_local
        caps['app'] = '/root/shared_volume/' + pytest.config.getoption('apk')
        caps['platformVersion'] = self.docker_exec_run(['adb', 'shell', 'getprop', 'ro.build.version.release']).output.decode("utf-8")
        caps['maxDuration'] = 18000
        return caps

    def start_appium_container(self):
        docker_client = docker.from_env()
        try:
            self.container = docker_client.containers.get("appium")
            self.container.restart()
            self.container_already_started = True
        except NotFound:
            shared_volume = "%s/shared" % os.path.dirname(os.path.realpath(__file__))
            self.container = docker_client.containers.run("appium/appium:local", detach=True, name="appium",
                                                          ports={'4723/tcp': 4723},
                                                          volumes={shared_volume: {
                                                              'bind': '/root/shared_volume', 'mode': 'rw'}})
        logging.info("Running Appium container. ID: %s" % self.container.short_id)

    def docker_exec_run(self, cmd):
        return self.container.exec_run(cmd, stdout=True, stderr=True, stdin=True)

    def connect_device(self):
        # Restart 5555 port on host to then connect to the device in docker instance
        call(['adb', 'tcpip', '5555'])
        # Connect to the device in docker container
        self.docker_exec_run(['adb', 'kill-server'])
        self.docker_exec_run(['adb', 'start-server'])
        device_address = pytest.config.getoption('device_ip') + ':5555'
        self.docker_exec_run(['adb', 'connect', device_address])
        if not self.container_already_started:
            input("\nPlease check your device and allow for USB debugging. Then press ENTER to continue the test..\n")

    def reset_battery_stats(self):
        logging.info("Resetting battery stats..")
        self.docker_exec_run(['adb', 'shell', 'dumpsys', 'batterystats', '--reset'])

    def generate_battery_stats(self):
        now = datetime.datetime.now()
        bugreport_name = "bugreport_%s" % now.strftime("%Y-%m-%d_%H-%M-%S")

        print("\nGenerating bugreport..")
        self.docker_exec_run(['adb', 'bugreport', "/root/shared_volume/%s" % bugreport_name])
        print("Bugreport saved in ./shared/%s.zip" % bugreport_name)


@pytest.mark.battery
class TestBatteryConsumption(BatteryConsumptionTestCase):

    def test_battery_consumption(self):
        sign_in_view = SignInView(self.driver)
        sign_in_view.create_user()
