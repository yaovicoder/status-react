(ns status-im.desktop.deep-links
  (:require [re-frame.core :as re-frame]
            [status-im.react-native.js-dependencies :as js-dependencies]))

(defn add-event-listener []
  (let [event-emitter (new (.-NativeEventEmitter js-dependencies/react-native)
                           js-dependencies/desktop-linking)]
    (.addListener event-emitter
                  "urlOpened"
                  #(re-frame/dispatch [:handle-universal-link %]))))