(ns status-im.ui.screens.currency-settings.subs
  (:require [re-frame.core :as re-frame]
            [status-im.ui.screens.currency-settings.models :as models]))

(re-frame/reg-sub
 :wallet.settings/currency
 (fn [db]
   (models/get-currency db)))
