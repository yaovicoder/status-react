'use strict';

const NativeModules = require('NativeModules');
const NativeEventEmitter = require('NativeEventEmitter');
const DesktopLinkingEventEmitter = new NativeEventEmitter(NativeModules.DesktopLinking);

NativeModules.DesktopLinking.addEventListener = (eventName, handler) => {
  if (eventName === "urlOpened") {
    const listener = DesktopLinkingEventEmitter.addListener(eventName, handler);
    return { remove: () => listener.remove() }
  } else {
    console.warn('Trying to subscribe to unknown event: "' + eventName + '"');
  }
}

module.exports = NativeModules.DesktopLinking;