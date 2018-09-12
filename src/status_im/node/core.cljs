(ns status-im.node.core
  (:require [re-frame.core :as re-frame]
            [status-im.fleet.core :as fleet]
            [status-im.native-module.core :as status]
            [status-im.utils.config :as config]
            [status-im.utils.types :as types]
            [taoensso.timbre :as log]))

(defn- add-custom-bootnodes [config network all-bootnodes]
  (let [bootnodes (as-> all-bootnodes $
                    (get $ network)
                    (vals $)
                    (map :address $))]
    (if (seq bootnodes)
      (assoc-in config [:ClusterConfig :BootNodes] bootnodes)
      config)))

(defn- add-log-level [config log-level]
  (if (empty? log-level)
    (assoc config
           :LogEnabled false)
    (assoc config
           :LogLevel log-level
           :LogEnabled true)))

(defn get-network-genesis-hash-prefix
  "returns the hex representation of the first 8 bytes of a network's genesis hash"
  [network]
  (cond
    (= network "1") "d4e56740f876aef8"
    (= network "3") "41941023680923e0"
    (= network "4") "6341fd3daf94b748"))

(defn get-les-topic
  "returns discovery v5 topic derived from genesis of the provided network"
  [network]
  (let [les-discovery-identifier "LES2@"
        hash-prefix (get-network-genesis-hash-prefix network)]
    (when  hash-prefix
      (str les-discovery-identifier hash-prefix))))

(defn get-topics
  [network]
  (let [les-topic (get-les-topic network)]
    (cond-> {"whisper" {:Min 2, :Max 2}}
      les-topic (assoc les-topic {:Min 2, :Max 2}))))

(defn get-account-network [db address]
  (get-in db [:accounts/accounts address :network]))

(defn- get-base-node-config [config]
  (assoc config
         :Name "StatusIM"))

(defn- get-account-node-config [db address]
  (let [accounts (get db :accounts/accounts)
        current-fleet-key (fleet/current-fleet db address)
        current-fleet (current-fleet-key fleet/fleets)
        {:keys [network
                settings
                bootnodes
                networks]} (get accounts address)
        use-custom-bootnodes (get-in settings [:bootnodes network])
        log-level (or (:log-level settings)
                      config/log-level-status-go)]
    (cond-> (get-in networks [network :config])
      :always
      (get-base-node-config)

      (boolean current-fleet)
      (assoc :NoDiscovery false
             :ClusterConfig {:Enabled true
                             :Fleet              (name current-fleet-key)
                             :BootNodes          (vals (get-in current-fleet [:boot]))
                             :TrustedMailServers (vals (get-in current-fleet [:mail]))
                             :StaticNodes        (vals (get-in current-fleet [:whisper]))}
             :WhisperConfig {:Enabled true
                             :LightClient true
                             :MinimumPoW 0.001
                             :EnableNTPSync true}
             :RequireTopics (get-topics network))

      (and
       config/bootnodes-settings-enabled?
       use-custom-bootnodes)
      (add-custom-bootnodes network bootnodes)

      :always
      (add-log-level log-level))))

(defn get-node-config [db network]
  (-> (get-in (:networks/networks db) [network :config])
      (get-base-node-config)
      (assoc :NoDiscovery true)
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
         node-config-json (types/clj->json node-config)]
     (log/info "Node config: " node-config-json)
     {:db         (assoc db :network network)
      :node/start node-config-json})))

(defn restart
  []
  {:node/stop nil})

(defn initialize
  [address {{:keys [status-node-started?] :as db} :db :as cofx}]
  (if (not status-node-started?)
    (start address cofx)
    (restart)))

(re-frame/reg-fx
 :node/start
 (fn [config]
   (status/start-node config)))

(re-frame/reg-fx
 :node/stop
 (fn []
   (status/stop-node)))
