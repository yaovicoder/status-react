(ns status-im.debug.events
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.data-store.accounts :as data-store.accounts]
            [status-im.models.network :as models.network]
            [status-im.debug.http-server :as http-server]
            [status-im.utils.handlers :as handlers]))

;; FX

(re-frame/reg-fx
 :http-server/start
 (fn []
   (http-server/start!)))

(re-frame/reg-fx
 :http-server/stop
 (fn []
   (http-server/stop!)))

(re-frame/reg-fx
 :http-server/respond
 (fn [[status-code data]]
   (http-server/respond! status-code data)))

;; Specific server operations

(defmulti process-request! (fn [{:keys [url type]}] [type (first url) (second url)]))

(defmethod process-request! [:POST "ping" nil]
  [_]
  {:http-server/respond [200 {:message "Pong!"}]})

(defmethod process-request! [:POST "dapp" "open"]
  [{{:keys [url]} :data}]
  {:dispatch            [:open-url-in-browser url]
   :http-server/respond [200 {:message "URL has been opened."}]})

(defmethod process-request! [:POST "network" nil]
  [{:keys [cofx data]}]
  (let [data (->> data
                  (map (fn [[k v]] [k {:value v}]))
                  (into {}))]
    (models.network/save
     cofx
     {:data       data
      :on-success (fn [network _]
                    {:http-server/respond [200 {:message    "Network has been added."
                                                :network-id network}]})
      :on-failure (fn [_ _]
                    {:http-server/respond [400 {:message "Please, check the validity of network information."}]})})))

(defmethod process-request! [:POST "network" "connect"]
  [{:keys [cofx data]}]
  (models.network/connect
   cofx
   {:network    (:id data)
    :on-success (fn [network _]
                  {:http-server/respond [200 {:message    "Network has been connected."
                                              :network-id network}]})
    :on-failure (fn [_ _]
                  {:http-server/respond [400 {:message "The network id you provided doesn't exist."}]})}))

(defmethod process-request! [:DELETE "network" nil]
  [{:keys [cofx data]}]
  (models.network/delete
   cofx
   {:network    (:id data)
    :on-success (fn [network _]
                  {:http-server/respond [200 {:message    "Network has been deleted."
                                              :network-id network}]})
    :on-failure (fn [_ _]
                  {:http-server/respond [400 {:message "Cannot delete the provided network."}]})}))

(defmethod process-request! :default
  [{:keys [type url]}]
  {:http-server/respond [404 {:message (str "Not found (" (name type) " " (string/join "/" url) ")")}]})

;; Handlers

(handlers/register-handler-fx
 :start-http-server-if-needed
 [re-frame/trim-v]
 (fn [_ [{:keys [address]}]]
   (let [{:keys [dev-mode?]} (data-store.accounts/get-by-address address)]
     (when dev-mode?
       {:http-server/start nil}))))

(handlers/register-handler-fx
 :process-http-request
 [re-frame/trim-v (re-frame/inject-cofx :random-id)]
 (fn [cofx [url type data]]
   (try
     (process-request! {:cofx cofx
                        :url  (rest (string/split url "/"))
                        :type (keyword type)
                        :data data})
     (catch js/Error e
       {:http-server/respond [400 {:message (str "Unsupported operation: " e)}]}))))