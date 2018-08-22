(ns status-im.utils.navigation
  (:require [status-im.react-native.js-dependencies :as js-dependencies]))

(def navigation-actions
  (when (not status-im.utils.platform/desktop?)
    (.-NavigationActions js-dependencies/react-navigation)))

(def navigation-events
  (when (not status-im.utils.platform/desktop?)
    (.-NavigationEvents js-dependencies/react-navigation)))

(def stack-actions
  (when (not status-im.utils.platform/desktop?)
    (.-StackActions js-dependencies/react-navigation)))

(def navigator-ref (atom nil))

(defn set-navigator-ref [ref]
  (reset! navigator-ref ref))

(defn can-be-called? []
  (and @navigator-ref
       (not status-im.utils.platform/desktop?)))

(defn navigate-to [route]
  (when (can-be-called?)
    (.dispatch
     @navigator-ref
     (.navigate
      navigation-actions
      #js {:routeName (name route)}))))

;; 'Navigation/REPLACE'
;; https://github.com/react-navigation/react-navigation/blob/master/src/routers/StackActions.js#L5
(defn navigate-replace [route]
  (when (can-be-called?)
    (.dispatch
     @navigator-ref
     (.replace
      stack-actions
      #js {:routeName (name route)}))))

(defn- navigate [params]
  (when (can-be-called?)
    (.navigate navigation-actions (clj->js params))))

(defn get-available-routes []
  (let [routes (.. @navigator-ref
                   -state -nav -routes)]
    (->> routes
         js->clj
         (map #(keyword (get % "key")))
         set)))

(defn navigate-reset [state]
  (when (can-be-called?)
    (let [state'     (mapv navigate state)
          {:keys [index actions]} state
          route-name (get-in actions [index :routeName])]
      (if (contains? (get-available-routes) route-name)
        (.dispatch
         @navigator-ref
         (.reset
          stack-actions
          (clj->js state')))
        (navigate-to route-name)))))

(defn navigate-back []
  (when (can-be-called?)
    (.dispatch
     @navigator-ref
     (.back navigation-actions))))
