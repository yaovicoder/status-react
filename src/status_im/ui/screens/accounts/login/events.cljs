(ns status-im.ui.screens.accounts.login.events
  (:require [re-frame.core :as re-frame]
            [status-im.ui.screens.accounts.login.models :as models]
            [status-im.utils.handlers :as handlers]))

;;;; FX
(re-frame/reg-fx
 :login
 (fn [[address password]]
   (models/login! address password)))

(re-frame/reg-fx
 :clear-web-data
 models/clear-web-data!)

(re-frame/reg-fx
 :data-store/change-account
 (fn [address]
   (models/change-account! address)))

;;;; Handlers
(handlers/register-handler-fx
 :ui/open-login
 (fn [cofx [_ address photo-path name]]
   (models/open-login address photo-path name cofx)))

(handlers/register-handler-fx
 :ui/login
 (fn [cofx [_ address password]]
   (models/user-login address password cofx)))

(handlers/register-handler-fx
 :callback/login
 (fn [cofx [_ login-result address]]
   (models/login-callback login-result address cofx)))
