(ns status-im.utils.navigation
  (:require [status-im.react-native.js-dependencies :as js-dependencies]))

(def navigation-actions (.-NavigationActions js-dependencies/react-navigation))
(def navigation-events (.-NavigationEvents js-dependencies/react-navigation))
(def stack-actions (.-StackActions js-dependencies/react-navigation))
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

;; 'Navigation/REPLACE'
;; https://github.com/react-navigation/react-navigation/blob/master/src/routers/StackActions.js#L5
(defn navigate-replace [route]
  (when @navigator-ref
    (.dispatch
     @navigator-ref
     (.replace
      stack-actions
      #js {:routeName (name route)}))))

(defn- navigate [params]
  (.navigate navigation-actions (clj->js params)))

(defn navigate-reset [state]
  (when @navigator-ref
    (.dispatch
     @navigator-ref
     (.reset
      stack-actions
      (clj->js state)))))

(defn navigate-back []
  (when @navigator-ref
    (.dispatch
     @navigator-ref
     (.back navigation-actions))))
