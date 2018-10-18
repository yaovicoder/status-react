(ns status-im.ui.screens.extensions.add.events
  (:require [pluto.registry :as registry]
            [re-frame.core :as re-frame]
            [status-im.extensions.core :as extensions]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.fx :as fx]))

(re-frame/reg-fx
 :extensions/load
 (fn [[url follow-up-event]]
   (extensions/load-from url #(re-frame/dispatch [follow-up-event (-> % extensions/read-extension extensions/parse)]))))

(handlers/register-handler-fx
 :extensions/stage
 (fn [{:keys [db] :as cofx} [_ extension-data]]
   (fx/merge cofx
             {:db (assoc db :staged-extension extension-data)}
             (navigation/navigate-to-cofx :show-extension nil))))

(handlers/register-handler-fx
 :extensions/add
 (fn [cofx [_ data active?]]
   (extensions/add cofx data active?)))

(re-frame/reg-fx
 :extensions/activate-all
 (fn [extensions]
   (doseq [{:keys [url active?]} extensions]
     (extensions/load-from url #(re-frame/dispatch [:extensions/add (-> % extensions/read-extension extensions/parse :data) active?])))))

(handlers/register-handler-fx
 :extensions/deactivate
 (fn [cofx [_ name]]
   (fx/merge cofx #(registry/deactivate name %))))

(re-frame/reg-fx
 :extensions/deactivate-all
 (fn [extensions]
   (doseq [{:keys [name]} extensions]
     (re-frame/dispatch [:extensions/deactivate name]))))