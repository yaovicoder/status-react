(ns status-im.ui.screens.fleet-settings.subs
  (:require [re-frame.core :as re-frame]
            [status-im.utils.config :as config]))

(re-frame/reg-sub
 :settings/current-fleet
 (fn [db _]
   (or (get-in db [:account/account :settings :fleet])
       config/fleet)))
