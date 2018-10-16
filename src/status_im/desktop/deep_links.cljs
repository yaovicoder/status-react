(ns status-im.desktop.deep-links
  (:require [re-frame.core :as re-frame]
            [status-im.react-native.js-dependencies :as js-dependencies]))

(defn add-event-listener []
  (.addEventListener js-dependencies/desktop-linking
                     "urlOpened"
                     #(re-frame/dispatch [:desktop/handle-universal-link %])))

