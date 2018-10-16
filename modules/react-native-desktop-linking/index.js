'use strict';

import { NativeModules } from "react-native";

const RCTDesktopLinking = NativeModules.DesktopLinking;
const DesktopLinkingEventEmitter = new NativeEventEmitter(RCTNetInfo);

const DesktopLinking = {
  addEventListener: (eventName, handler) => {
    if (eventName === "urlOpened") {
      const listener = NetInfoEventEmitter.addListener(eventName, handler);
      return {remove: () => listener.remove()}
    } else {
      console.warn('Trying to subscribe to unknown event: "' + eventName + '"');
    }
  }
}

module.exports = DesktopLinking;