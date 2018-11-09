import datetime
import logging
import subprocess
import time

import docker
from docker.errors import NotFound


DOCKER_SHARED_VOLUME_PATH = '/root/shared_volume'


class AppiumContainer:

    def start_appium_container(self, shared_volume):
        docker_client = docker.from_env()
        try:
            self.container = docker_client.containers.get("appium")
            self.container.restart()
        except NotFound:
            self.container = docker_client.containers.run("appium/appium:local", detach=True, name="appium",
                                                          ports={'4723/tcp': 4723},
                                                          volumes={shared_volume: {
                                                              'bind': DOCKER_SHARED_VOLUME_PATH, 'mode': 'rw'}})
        logging.info("Running Appium container. ID: %s" % self.container.short_id)

    def docker_exec_run(self, cmd):
        return self.container.exec_run(cmd, stdout=True, stderr=True, stdin=True)

    def connect_device(self, device_ip):
        device_address = device_ip + ':5555'
        connected_state = "connected to %s" % device_address

        cmd = self.docker_exec_run(['adb', 'connect', device_address])
        if connected_state in cmd.output.decode("utf-8"):
            logging.info("adb is already connected with the device")
        else:
            logging.info("Connecting the device with adb..")
            # Restart adb on host machine
            subprocess.call(['adb', 'kill-server'])
            input("Connect USB cable to the device and press ENTER..")
            # Reset USB on host machine
            subprocess.call(['adb', 'usb'])
            time.sleep(5)  # wait until adb usb is restarted
            # Set the target device to listen for a TCP/IP connection on port 5555 on host machine
            subprocess.call(['adb', 'tcpip', '5555'])
            # restarting in TCP mode port: 5555
            input("Now, disconnect the USB cable and press ENTER..")
            # Connect to the device in docker container
            self.docker_exec_run(['adb', 'connect', device_address])
            input("Please check your device and allow for USB debugging if necessary. Then press ENTER to continue "
                  "the test..\n")

    def reset_battery_stats(self):
        logging.info("Resetting device stats..")
        self.docker_exec_run(['adb', 'shell', 'dumpsys', 'batterystats', '--reset'])

    def generate_battery_stats(self, report_name):
        now = datetime.datetime.now()
        bugreport_name = "bugreport_%s_%s" % (report_name, now.strftime("%Y-%m-%d_%H-%M-%S"))

        print("\nGenerating device report from the test..")
        self.docker_exec_run(['adb', 'bugreport', "/%s/%s" % (DOCKER_SHARED_VOLUME_PATH, bugreport_name)])
        print("Device report saved in the shared volume as %s.zip" % bugreport_name)

    def stop_container(self):
        self.container.stop()
