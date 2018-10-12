(ns status-im.extensions.core
  (:require [clojure.string :as string]
            [pluto.reader :as reader]
            [pluto.registry :as registry]
            [pluto.storages :as storages]
            [status-im.accounts.update.core :as accounts.update]
            [status-im.chat.commands.core :as commands]
            [status-im.chat.commands.impl.transactions :as transactions]
            [status-im.ui.components.react :as react]
            [status-im.utils.fx :as fx]))

(def components
  {'view           {:value react/view}
   'text           {:value react/text}
   'nft-token      {:value transactions/nft-token}
   'send-status    {:value transactions/send-status}
   'asset-selector {:value transactions/choose-nft-asset-suggestion}
   'token-selector {:value transactions/choose-nft-token-suggestion}})

(def app-hooks #{commands/command-hook})

(def capacities
  (reduce (fn [capacities hook]
            (assoc-in capacities [:hooks :commands] hook))
          {:components components
           :queries    {'get-collectible-token {:value :get-collectible-token}}
           :events     {}}
          app-hooks))

(defn read-extension [o]
  (-> o :value first :content reader/read))

(defn parse [{:keys [data] :as m}]
  (try
    (let [{:keys [errors] :as extension-data} (reader/parse {:capacities capacities} data)]
      (when errors
        (println "Failed to parse status extensions" errors))
      extension-data)
    (catch :default e (println "EXC" e))))

(defn url->uri [s]
  (when s
    (string/replace s "https://get.status.im/extension/" "")))

(defn load-from [url f]
  (when-let [uri (url->uri url)]
    (storages/fetch uri f)))

(defn- build [id url]
  {:url url
   :id  (string/replace id "-" "")})

(fx/defn set-input
  [{:keys [db]} input-key value]
  {:db (update db :extensions/manage assoc input-key {:value value})})

(fx/defn fetch [cofx id]
  (get-in cofx [:db :account/account :extensions id]))

(fx/defn edit
  [{:keys [db] :as cofx} id]
  (let [{:keys [url]} (fetch cofx id)]
    (-> (set-input cofx :url (str url))
        (assoc :dispatch [:navigate-to :edit-extension]))))

(fx/defn upsert
  [{{:extensions/keys [manage] :account/keys [account] :as db} :db
    random-id-generator :random-id-generator :as cofx}]
  (let [{:keys [url id]} manage
        extension      (build
                        (or (:value id) (random-id-generator))
                        (:value url))
        new-extensions (assoc (:extensions account) (:id extension) extension)]
    (fx/merge cofx
              {:db       (dissoc db :extensions/manage)
               :dispatch [:navigate-back]}
              (accounts.update/account-update
               {:extensions new-extensions}
               {:success-event nil}))))