## Instructions

1. Install needed tools

    - Install Docker https://docs.docker.com/install/
    - Install Python 3

    Go to https://realpython.com/installing-python and follow the instructions. For macOS with Homebrew, you can run `brew install python3` to install it. 

2. Build Appium docker image

    - Make sure Docker app is running
    - Build Appium image to have environment in which tests will be executed

    ```
    $ cd tests/battery/docker
    $ docker build -t "appium/appium:local" .
    ```

    Make sure all build steps were successful. You should see:
    ```
    Successfully tagged appium/appium:local
    ```

3. Setup bluetooth connection with your device

    To run the battery test without charging the device, adb needs to connect to it wirelessly. To avoid wifi connection issues, we will connect to the device via bluetooth.

    macOS instructions:
    - On device, enable bluetooth on your device in Android settings -> connections -> bluetooth
    - On device, go to mobile hotspots and tethering and enable bluetooth tethering
    - On macOS, go to the bluetooth settings and connect with the device (click "Connect to Network")

4. Find out IP address of the device

    On device, open wifi settings and click on the current network. Write down the IP address as it will be needed for running the battery test wirelessly.

5. Run the battery test

    - Make sure the device is sufficiently charged
    - Disconnect the device USB cable
    - Create `shared` directory in `status-react/test/appium/tests/battery` and put there an .apk to test
    - Run the battery test
        - `--apk=StatusIm-181106-095508-7aa597-e2e.apk` - name of the apk file in `status-react/test/appium/tests/battery/shared/` dir
        - `--device_ip` - IP address of the device (see 3.)
   
    ```
    $ cd status-react/test/appium/tests/battery
    $ python3 -m pytest --apk=StatusIm-181106-095508-7aa597-e2e.apk --device_ip=192.168.11.38 test_battery_consumption.py
    ```
    - Enable USB debugging when asked and press ENTER to proceed with the test.

    **In case of issues:**
    
    Find out the Appium container id: `docker ps` and get the latest logs: `docker logs ebdf1761f51f` where `ebdf1761f51f` is the container id.
    
6. Analyse test results

    The test generates Android bugreport that is saved `shared/`. It contains data about battery, cpu, memory, network usage, and more. See [energy-efficient-bok](https://github.com/status-im/energy-efficient-bok/blob/master/QA_Android.md) or [Battery Historian](https://developer.android.com/studio/profile/battery-historian) to learn how to analyse it.

