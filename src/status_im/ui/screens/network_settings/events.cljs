(ns status-im.ui.screens.network-settings.events
  (:require [re-frame.core :as re-frame]
            [taoensso.timbre :as log]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.fleet.core :as fleet-core]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.fx :as fx]))

(defn- show-popup [title msg]
  {:utils/show-popup [title msg nil]})

(defn les-not-supported-warning [fleet]
  (let [message
        (str (name fleet)
             " does not support LES!\n"
             "Please, select one of the supported fleets:"
             (map name fleet-core/fleets-with-les))]
    (show-popup "LES not supported" message)))

(defn experimental-warning []
  (let [message "Use at your own risk!"]
    (show-popup "LES support is experimental!" message)))

(fx/defn open-network-details
  [cofx network show-warning?]
  (fx/merge cofx
            (when show-warning?
              (experimental-warning))
            (navigation/navigate-to-cofx :network-details {:networks/selected-network network})))

(handlers/register-handler-fx
 :ui/network-entry-clicked
 (fn [cofx [_ network]]
   (let [db                  (:db cofx)
         rpc-network?        (get-in network [:config :UpstreamConfig :Enabled] false)
         fleet               (fleet-core/current-fleet db nil)
         fleet-supports-les? (fleet-core/fleet-supports-les? fleet)]
     (if (or rpc-network? fleet-supports-les?)
       (open-network-details cofx network (not rpc-network?))
       ;; Otherwise, we show an explanation dialog to a user if the current fleet does not suport LES
       (les-not-supported-warning fleet)))))

