(ns status-im.ui.screens.log-level-settings.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :settings/current-log-level
 (fn [db _]
   (get-in db [:account/account :settings :log-level])))
