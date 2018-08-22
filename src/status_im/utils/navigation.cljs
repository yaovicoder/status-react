(ns status-im.utils.navigation
  (:require [status-im.react-native.js-dependencies :as js-dependencies]))

(def navigation-actions (.-NavigationActions js-dependencies/react-navigation))
(def navigator-ref (atom nil))

(defn set-navigator-ref [ref]
  (reset! navigator-ref ref))

(defn navigate-to [route]
  (when @navigator-ref
    (.dispatch
     @navigator-ref
     (.navigate
      navigation-actions
      #js {:routeName (name route)}))))

(defn navigate-back []
  (when @navigator-ref
    (.dispatch
     @navigator-ref
     (.back navigation-actions))))
