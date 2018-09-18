(ns status-im.hardwallet.core
  (:require [status-im.utils.platform :as platform]
            [status-im.utils.config :as config]
            [status-im.react-native.js-dependencies :as js-dependencies]
            [re-frame.core :as re-frame]))

(defn check-nfc-support []
  (when config/hardwallet-enabled?
    (.. js-dependencies/nfc-manager
        -default
        isSupported
        (then #(re-frame/dispatch [:hardwallet.callback/set-nfc-support %])))))

(defn check-nfc-enabled []
  (when platform/android?
    (.. js-dependencies/nfc-manager
        -default
        isEnabled
        (then #(re-frame/dispatch [:hardwallet.callback/set-nfc-enabled %])))))

(defn set-nfc-support [supported? {:keys [db]}]
  {:db (assoc-in db [:hardwallet :nfc-supported?] supported?)})

(defn set-nfc-enabled [enabled? {:keys [db]}]
  {:db (assoc-in db [:hardwallet :nfc-enabled?] enabled?)})

(defn go-to-settings []
  (when platform/android?
    (.. js-dependencies/nfc-manager
        -default
        goToNfcSetting)))

(defn navigate-to-connect-screen []
  {:dispatch                     [:navigate-to :hardwallet/connect]
   :hardwallet/check-nfc-enabled nil})

(defn hardwallet-supported? [db]
  (and config/hardwallet-enabled?
       platform/android?
       (get-in db [:hardwallet :nfc-supported?])))

(defn return-back-from-nfc-settings [app-coming-from-background? {:keys [db]}]
  (when (and app-coming-from-background?
             (= :hardwallet/connect (:view-id db)))
    {:hardwallet/check-nfc-enabled nil}))

(re-frame/reg-fx
 :hardwallet/check-nfc-support
 check-nfc-support)

(re-frame/reg-fx
 :hardwallet/check-nfc-enabled
 check-nfc-enabled)

(re-frame/reg-fx
 :hardwallet/go-to-settings
 go-to-settings)
