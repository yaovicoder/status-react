(ns status-im.extensions.registry
  (:require [clojure.string :as string]
            [pluto.reader :as reader]
            [pluto.registry :as registry]
            [pluto.host :as host]
            [pluto.storages :as storages]
            [status-im.chat.commands.core :as commands]
            [status-im.chat.commands.impl.transactions :as transactions]
            [status-im.ui.components.react :as react]))

(def components
  {'view           react/view
   'text           react/text
   'nft-token      transactions/nft-token
   'send-status    transactions/send-status
   'asset-selector transactions/choose-nft-asset-suggestion
   'token-selector transactions/choose-nft-token-suggestion})

(def app-hooks #{commands/command-hook})

(def capacities
  (reduce (fn [capacities hook]
            (assoc-in capacities [:hooks (host/id hook)] hook))
          {:components    components
           :queries       #{:get-in :get-collectible-token}
           :events        #{:set-in}
           :permissions   {:read  {:include-paths #{[:chats #".*"]}}
                           :write {:include-paths #{}}}}
          app-hooks))

(defn parse [{:keys [data]}]
  (try
    (let [{:keys [errors] :as extension-data} (reader/parse {:capacities capacities} data)]
      (when errors
        (println "Failed to parse status extensions" errors))
      extension-data)
    (catch :default e (println "EXC" e))))

(defn read-extension [o]
  (-> o :value first :content reader/read))

(defn url->uri [s]
  (when s
    (string/replace s "https://get.status.im/extension/" "")))

(defn load-from [url f]
  (when-let [uri (url->uri url)]
    (storages/fetch uri #(do (println "GOT" %) (f %)))))
