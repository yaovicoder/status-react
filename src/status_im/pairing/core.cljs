(ns status-im.pairing.core
  (:require
   [re-frame.core :as re-frame]
   [status-im.utils.fx :as fx]
   [status-im.utils.config :as config]
   [status-im.transport.message.protocol :as protocol]
   [status-im.data-store.installations :as data-store.installations]
   [status-im.native-module.core :as native-module]
   [status-im.utils.identicon :as identicon]
   [status-im.data-store.contacts :as data-store.contacts]
   [status-im.transport.message.pairing :as transport.pairing]))

(defn- parse-response [response-js]
  (-> response-js
      js/JSON.parse
      (js->clj :keywordize-keys true)))

(defn start [cofx]
  (let [{:keys [current-public-key web3]} (:db cofx)]
    {:shh/send-pairing-message {:web3    web3
                                :src     current-public-key
                                :payload []}}))

(defn merge-contact [local remote]
  (let [[old-contact new-contact] (sort-by :last-updated [local remote])]
    (-> local
        (merge new-contact)
        (assoc :photo-path
               (or (:photo-path new-contact)
                   (:photo-path old-contact)
                   (identicon/identicon (:whisper-identity local))))
        (assoc :pending? (boolean
                          (and (:pending? local true)
                               (:pending? remote true)))))))

(def merge-contacts (partial merge-with merge-contact))

(defn handle-bundles-added [{:keys [db]} bundle]
  (let [dev-mode? (get-in db [:account/account :dev-mode?])]
    (when (config/pairing-enabled? dev-mode?)
      (let [installation-id  (:installationID bundle)
            new-installation {:installation-id installation-id
                              :confirmed? false}]
        (when
         (and (= (:identity bundle)
                 (:current-public-key db))
              (not (get-in db [:pairing/installations installation-id])))
          {:db (assoc-in db
                         [:pairing/installations installation-id]
                         new-installation)
           :data-store/tx [(data-store.installations/save new-installation)]})))))

(defn sync-installation-messages [{:keys [db]}]
  (let [contacts (:contacts/contacts db)]
    (map
     (fn [[k v]] (transport.pairing/SyncInstallation. {k (dissoc v :photo-path)}))
     contacts)))

(defn enable [{:keys [db]} installation-id]
  {:db (assoc-in db
                 [:pairing/installations installation-id :confirmed?]
                 true)
   :data-store/tx [(data-store.installations/enable installation-id)]})

(defn disable [{:keys [db]} installation-id]
  {:db (assoc-in db
                 [:pairing/installations installation-id :confirmed?]
                 false)
   :data-store/tx [(data-store.installations/disable installation-id)]})

(defn handle-enable-installation-response
  "Callback to dispatch on enable signature response"
  [installation-id response-js]
  (let [{:keys [error]} (parse-response response-js)]
    (if error
      (re-frame/dispatch [:pairing.callback/enable-installation-failed  error])
      (re-frame/dispatch [:pairing.callback/enable-installation-success installation-id]))))

(defn handle-disable-installation-response
  "Callback to dispatch on disable signature response"
  [installation-id response-js]
  (let [{:keys [error]} (parse-response response-js)]
    (if error
      (re-frame/dispatch [:pairing.callback/disable-installation-failed  error])
      (re-frame/dispatch [:pairing.callback/disable-installation-success installation-id]))))

(defn enable-installation! [installation-id]
  (native-module/enable-installation installation-id
                                     (partial handle-enable-installation-response installation-id)))

(defn disable-installation! [installation-id]
  (native-module/disable-installation installation-id
                                      (partial handle-disable-installation-response installation-id)))

(defn enable-fx [_ installation-id]
  {:pairing/enable-installation installation-id})

(defn disable-fx [_ installation-id]
  {:pairing/disable-installation installation-id})

(re-frame/reg-fx
 :pairing/enable-installation
 enable-installation!)

(re-frame/reg-fx
 :pairing/disable-installation
 disable-installation!)

(defn send-installation-message [cofx]
  ;; The message needs to be broken up in chunks as we hit the whisper size limit
  (let [{:keys [current-public-key web3]} (:db cofx)
        sync-messages (sync-installation-messages cofx)]
    {:shh/send-direct-message
     (map #(hash-map :web3 web3
                     :src current-public-key
                     :dst current-public-key
                     :payload %) sync-messages)}))

(defn handle-sync-installation [{:keys [db] :as cofx} {:keys [contacts]} sender]
  (let [dev-mode? (get-in db [:account/account :dev-mode?])]
    (when (and (config/pairing-enabled? dev-mode?)
               (= sender (get-in cofx [:db :current-public-key])))
      (let [new-contacts  (merge-contacts (:contacts/contacts db) contacts)]
        {:db (assoc db :contacts/contacts new-contacts)
         :data-store/tx [(data-store.contacts/save-contacts-tx (vals new-contacts))]}))))

(fx/defn load-installations [{:keys [db all-installations]}]
  {:db (assoc db :pairing/installations (reduce
                                         (fn [acc {:keys [installation-id] :as i}]
                                           (assoc acc installation-id i))
                                         {}
                                         all-installations))})
