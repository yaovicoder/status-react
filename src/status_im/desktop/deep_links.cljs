(ns status-im.desktop.deep-links
  (:require [re-frame.core :as re-frame]
            [status-im.react-native.js-dependencies :as js-dependencies]))

(defn on-url-opened []
  (.onUrlOpened js-dependencies/desktop-linking
                #(re-frame/dispatch [:desktop/handle-universal-link %])))
