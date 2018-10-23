(ns status-im.data-store.realm.schemas.base.migrations
  (:require [taoensso.timbre :as log]
            [cognitect.transit :as transit]
            [clojure.set :as set]
            [clojure.string :as string]
            [status-im.utils.random :as random]
            [status-im.utils.types :as types]))

(def reader (transit/reader :json))
(def writer (transit/writer :json))

(defn serialize [o] (transit/write writer o))
(defn deserialize [o] (try (transit/read reader o) (catch :default e nil)))

(defn v1 [old-realm new-realm]
  (log/debug "migrating base database v1: " old-realm new-realm))

(defn v2 [old-realm new-realm]
  (log/debug "migrating base database v2: " old-realm new-realm))

(defn v3 [old-realm new-realm]
  (log/debug "migrating base database v3: " old-realm new-realm))

(defn v4 [old-realm new-realm]
  (log/debug "migrating base database v4: " old-realm new-realm))

(def removed-tokens-v5
  #{:ATMChain :Centra :ROL})

(def removed-fiat-currencies
  #{:bmd :bzd :gmd :gyd :kyd :lak :lrd :ltl :mkd :mnt :nio :sos :srd :yer})

(defn v5 [old-realm new-realm]
  (log/debug "migrating accounts schema v4")
  (let [accounts (.objects new-realm "account")]
    (dotimes [i (.-length accounts)]
      (let [account      (aget accounts i)
            old-settings (deserialize (aget account "settings"))
            new-settings (-> old-settings
                             (update-in [:wallet :visible-tokens :mainnet]
                                        #(set/difference % removed-tokens-v5))
                             (update-in [:wallet :currency]
                                        #(if (removed-fiat-currencies %) :usd %)))
            updated      (serialize new-settings)]
        (aset account "settings" updated)))))

(defn v6 [old-realm new-realm]
  (log/debug "migrating base database v6: " old-realm new-realm))

(defn v7 [old-realm new-realm]
  (log/debug "migrating base database v7: " old-realm new-realm))

(def removed-tokens-v8
  #{:ATT})

(defn v8 [old-realm new-realm]
  (log/debug "migrating accounts schema v8")
  (let [accounts (.objects new-realm "account")]
    (dotimes [i (.-length accounts)]
      (let [account      (aget accounts i)
            old-settings (deserialize (aget account "settings"))
            new-settings (-> old-settings
                             (update-in [:wallet :visible-tokens :testnet]
                                        #(set/difference % removed-tokens-v8)))
            updated      (serialize new-settings)]
        (aset account "settings" updated)))))

(defn v9 [old-realm new-realm]
  (log/debug "migrating accounts schema v9")
  (let [accounts (.objects new-realm "account")]
    (dotimes [i (.-length accounts)]
      (let [account      (aget accounts i)
            old-settings (deserialize (aget account "settings"))
            new-settings (-> old-settings
                             (dissoc :wnode))
            updated      (serialize new-settings)]
        (aset account "settings" updated)))))

(defn v10 [old-realm new-realm]
  (log/debug "migrating base database v10: " old-realm new-realm))

(defn v11 [old-realm new-realm]
  (log/debug "migrating accounts schema v11")
  (let [accounts (.objects new-realm "account")]
    (dotimes [i (.-length accounts)]
      (let [account             (aget accounts i)
            old-installation-id (aget account "installation-id")
            installation-id     (random/guid)]
        (when (string/blank? old-installation-id)
          (aset account "installation-id" installation-id))))))

(defn v12 [old-realm new-realm]
  (log/debug "migrating base database v12: " old-realm new-realm))

(defn v13 [old-realm new-realm]
  (log/debug "migrating base database v13: " old-realm new-realm))

(defn- deserialize-networks [networks]
  (reduce-kv
   (fn [acc network-id props]
     (assoc acc network-id (update props :config types/json->clj)))
   {}
   networks))

(defn- serialize-networks [networks]
  (map (fn [[_ props]]
         (update props :config types/clj->json))
       networks))

(def a (atom nil))

(defn v14 [old-realm new-realm]
  (log/debug "migrating base database v14: " old-realm new-realm)
  (let [accounts (.objects new-realm "account")]
    (dotimes [i (.-length accounts)]
      (let [account (aget accounts i)
            networks (aget account "networks")]
        (when (not (aget networks "xdai_rpc"))
          (aset networks "xdai_rpc"
                (clj->js
                 {:id      "xdai_rpc",
                  :name    "xDai Chain",
                  :config  (types/clj->json {:NetworkId      100,
                                             :DataDir        "/ethereum/xdai_rpc",
                                             :UpstreamConfig {:Enabled true, :URL "https://dai.poa.network"}}),
                  :rpc-url nil})))
        (when (not (aget networks "poa_rpc"))
          (aset networks "poa_rpc"
                (clj->js
                 {:id      "poa_rpc",
                  :name    "POA Network",
                  :config  (types/clj->json {:NetworkId      99,
                                             :DataDir        "/ethereum/poa_rpc",
                                             :UpstreamConfig {:Enabled true, :URL "https://poa.infura.io"}}),
                  :rpc-url nil})))))))
