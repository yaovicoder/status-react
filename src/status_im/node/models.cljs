(ns status-im.node.models
  (:require [status-im.utils.config :as config]
            [status-im.utils.types :as types]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [status-im.models.fleet :as fleet]))

(defn- add-custom-bootnodes [config network all-bootnodes]
  (let [bootnodes (as-> all-bootnodes $
                    (get $ network)
                    (vals $)
                    (map :address $))]
    (if (seq bootnodes)
      (assoc config :ClusterConfig {:Enabled   true
                                    :BootNodes bootnodes})
      config)))

(defn- add-les-config [config]
  (assoc config :LightEthConfig {:Enabled true}))

(defn- add-upstream-config [config]
  (assoc config :UpstreamConfig {:Enabled true
                                 :URL "https://mainnet.infura.io/z6GCTmjdP3FETEJmMBI4"}))

(defn- add-log-level [config log-level]
  (if (empty? log-level)
    (assoc config
           :LogEnabled false)
    (assoc config
           :LogLevel log-level
           :LogEnabled true)))

(defn get-account-network [db address]
  (get-in db [:accounts/accounts address :network]))

(defn- get-account-node-config [db address]
  (let [accounts (get db :accounts/accounts)
        {:keys [network
                settings
                bootnodes
                networks]} (get accounts address)
        use-custom-bootnodes (get-in settings [:bootnodes network])
        log-level (or (:log-level settings)
                      config/log-level-status-go)]
    (cond-> (get-in networks [network :config])
      ; TODO: Add default bootnodes
      (and
       config/bootnodes-settings-enabled?
       use-custom-bootnodes)
      (add-custom-bootnodes network bootnodes)

      :always
      (add-log-level log-level))))

(defn get-node-config [db network]
  (-> (get-in (:networks/networks db) [network :config])
      (assoc config
             :NoDiscovery false
             :Rendezvous false
             :ClusterConfig {:Enabled true
                             :Fleet "eth.beta"}
             :WhisperConfig {:Enabled true
                             :LightClient true
                             :MinimumPoW 0.001
                             :EnableNTPSync true}
             :RequireTopics {"LES2@d4e56740f876aef8" {:Min 2, :Max 2}
                             "whisper" {:Min 2, :Max 2}})
      (add-les-config)
      (add-upstream-config)
      (add-log-level config/log-level-status-go)))

(defn start
  ([cofx]
   (start nil cofx))
  ([address {:keys [db]}]
   (let [network     (if address
                       (get-account-network db address)
                       (:network db))
         node-config (if address
                       (get-account-node-config db address)
                       (get-node-config db network))
         node-config-json (types/clj->json node-config)
         fleet (name (fleet/current-fleet db address))]
     (log/info "Node config: " node-config-json)
     {:db         (assoc db :network network)
      :node/start [node-config-json]})))

(defn restart
  []
  {:node/stop nil})

(defn initialize
  [address {{:keys [status-node-started?] :as db} :db :as cofx}]
  (if (not status-node-started?)
    (start address cofx)
    (restart)))
