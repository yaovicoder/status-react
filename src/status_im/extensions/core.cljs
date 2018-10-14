(ns status-im.extensions.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [pluto.reader :as reader]
            [pluto.storages :as storages]
            [status-im.chat.commands.core :as commands]
            [status-im.chat.commands.impl.transactions :as transactions]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.button.view :as button]
            [status-im.utils.handlers :as handlers]))

(re-frame/reg-fx
  ::alert
  (fn [value] (js/alert value)))

(re-frame/reg-event-fx
  :alert
  (fn [_ [_ {:keys [value]}]]
    {::alert value}))

(re-frame/reg-fx
  ::log
  (fn [value] (js/console.log value)))

(re-frame/reg-event-fx
  :log
  (fn [_ [_ {:keys [value]}]]
    {::log value}))

(re-frame/reg-sub
  :store/get
  (fn [db [_ {:keys [key]}]]
    (get-in db [:extensions-store :collectible key])))

(handlers/register-handler-fx
  :store/put
  (fn [{:keys [db]} [_ {:keys [key value]}]]
    {:db (assoc-in db [:extensions-store :collectible key] value)}))

(re-frame/reg-event-fx
  :http/get
  (fn [_ [_ {:keys [url on-success on-failure timeout]}]]
    {:http-get {:url url
                :success-event-creator (fn [o] (into on-success (vector o)))
                :failure-event-creator (when on-failure (fn [o] (into on-failure (vector o))))
                :timeout-ms timeout}}))

(defn button [{:keys [on-click]} label]
  [button/secondary-button {:on-press on-click} label])

(def capacities
  {:components {'view               {:value react/view}
                'text               {:value react/text}
                'text-input         {:value react/text-input}
                'button             {:value button :properties {:on-click :event}}
                'nft-token-viewer   {:value transactions/nft-token :properties {:token :string}}
                'transaction-status {:value transactions/transaction-status :properties {:outgoing :string :tx-hash :string}}
                'asset-selector     {:value transactions/choose-nft-asset-suggestion}
                'token-selector     {:value transactions/choose-nft-token-suggestion}}
   :queries    {'store/get {:value :store/get}
                'get-collectible-token {:value :get-collectible-token}}
   :events     {'alert
                {:permissions [:read]
                 :value       :alert}
                'log
                {:permissions [:read]
                 :value       :log}
                'store/put
                {:permissions [:read]
                 :value       :store/put}
                'http/get
                {:permissions [:read]
                 :value       :http/get}}
   :hooks {:commands commands/command-hook}})

(defn read-extension [{:keys [value]}]
  (when (seq value)
    (let [{:keys [content]} (first value)]
      (reader/read content))))

(defn parse [{:keys [data]}]
  (try
    (let [{:keys [errors] :as extension-data} (reader/parse {:capacities capacities} data)]
      (when errors
        (println "Failed to parse status extensions" errors))
      extension-data)
    (catch :default e (println "EXC" e))))

(def uri-prefix "https://get.status.im/extension/")

(defn valid-uri? [s]
  (when s
    (string/starts-with? s uri-prefix)))

(defn url->uri [s]
  (when s
    (string/replace s uri-prefix "")))

(defn load-from [url f]
  (when-let [uri (url->uri url)]
    (storages/fetch uri f)))
