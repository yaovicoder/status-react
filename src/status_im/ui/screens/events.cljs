(ns status-im.ui.screens.events
  (:require status-im.chat.events
            status-im.network.events
            [status-im.transport.handlers :as transport.handlers]
            status-im.protocol.handlers
            [status-im.models.protocol :as models.protocol]
            [status-im.models.account :as models.account]
            [status-im.ui.screens.accounts.models :as accounts.models]
            status-im.ui.screens.accounts.login.events
            [status-im.ui.screens.accounts.login.models :as login]
            status-im.ui.screens.accounts.recover.events
            [status-im.models.contacts :as models.contacts]
            status-im.ui.screens.add-new.new-chat.events
            status-im.ui.screens.group.chat-settings.events
            status-im.ui.screens.group.events
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.dimensions :as dimensions]
            status-im.ui.screens.accounts.events
            status-im.utils.universal-links.events
            status-im.init.events
            status-im.node.events
            status-im.signals.events
            status-im.web3.events
            status-im.notifications.events
            status-im.ui.screens.add-new.new-chat.navigation
            status-im.ui.screens.network-settings.events
            status-im.ui.screens.profile.events
            status-im.ui.screens.qr-scanner.events
            status-im.ui.screens.wallet.events
            [status-im.models.wallet :as models.wallet]
            status-im.ui.screens.wallet.collectibles.events
            status-im.ui.screens.wallet.send.events
            status-im.ui.screens.wallet.settings.events
            status-im.ui.screens.wallet.transactions.events
            status-im.ui.screens.wallet.choose-recipient.events
            status-im.ui.screens.wallet.collectibles.cryptokitties.events
            status-im.ui.screens.wallet.collectibles.cryptostrikers.events
            status-im.ui.screens.wallet.collectibles.etheremon.events
            status-im.ui.screens.browser.events
            status-im.ui.screens.offline-messaging-settings.events
            status-im.ui.screens.privacy-policy.events
            status-im.ui.screens.bootnodes-settings.events
            status-im.ui.screens.currency-settings.events
            status-im.utils.keychain.events
            [re-frame.core :as re-frame]
            [status-im.native-module.core :as status]
            [status-im.ui.components.permissions :as permissions]
            [status-im.constants :as constants]
            [status-im.data-store.core :as data-store]
            [status-im.data-store.realm.core :as realm]
            [status-im.utils.keychain.core :as keychain]
            [status-im.i18n :as i18n]
            [status-im.react-native.js-dependencies :as js-dependencies]
            [status-im.js-dependencies :as dependencies]
            [status-im.ui.components.react :as react]
            [status-im.transport.core :as transport]
            [status-im.transport.inbox :as inbox]
            [status-im.ui.screens.db :refer [app-db]]
            [status-im.utils.datetime :as time]
            [status-im.utils.random :as random]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.utils.http :as http]
            [status-im.utils.utils :as utils]
            [taoensso.timbre :as log]))

;;;; COFX

(re-frame/reg-cofx
 :now
 (fn [coeffects _]
   (assoc coeffects :now (time/timestamp))))

(re-frame/reg-cofx
 :random-id
 (fn [coeffects _]
   (assoc coeffects :random-id (random/id))))

(re-frame/reg-cofx
 :random-id-seq
 (fn [coeffects _]
   (assoc coeffects :random-id-seq (repeatedly random/id))))

;;;; FX

(defn- http-get [{:keys [url response-validator success-event-creator failure-event-creator timeout-ms]}]
  (let [on-success #(re-frame/dispatch (success-event-creator %))
        on-error   #(re-frame/dispatch (failure-event-creator %))
        opts       {:valid-response? response-validator
                    :timeout-ms      timeout-ms}]
    (http/get url on-success on-error opts)))

(re-frame/reg-fx
 :http-get
 http-get)

(re-frame/reg-fx
 :http-get-n
 (fn [calls]
   (doseq [call calls]
     (http-get call))))

(re-frame/reg-fx
 :request-permissions-fx
 (fn [options]
   (permissions/request-permissions options)))

(re-frame/reg-fx
 :ui/listen-to-window-dimensions-change
 (fn []
   (dimensions/add-event-listener)))

(re-frame/reg-fx
 :show-error
 (fn [content]
   (utils/show-popup "Error" content)))

(re-frame/reg-fx
 :show-confirmation
 (fn [{:keys [title content confirm-button-text on-accept on-cancel]}]
   (utils/show-confirmation title content confirm-button-text on-accept on-cancel)))

(re-frame/reg-fx
 :close-application
 (fn [_]
   (status/close-application)))

(re-frame/reg-fx
 ::app-state-change-fx
 (fn [state]
   (status/app-state-change state)))

(defn persist-chat-ui-props
  ([cofx]
   (persist-chat-ui-props "background" cofx))
  ([state {{:keys [chat-ui-props current-public-key]} :db}]
   (when (#{"background" "inactive"} state)
     (->> chat-ui-props
          (reduce-kv
           (fn [acc k v] (assoc acc k (dissoc v :input-ref)))
           {})
          clj->js
          js/JSON.stringify
          (.setItem js-dependencies/async-storage
                    (str "@StatusIm:" current-public-key ":chat-ui-props")))
     nil)))

;;;; Handlers

(handlers/register-handler-db
 :set
 (fn [db [_ k v]]
   (assoc db k v)))

(handlers/register-handler-db
 :set-in
 (fn [db [_ path v]]
   (assoc-in db path v)))

(defn logout
  [{:keys [db] :as cofx}]
  (let [{:transport/keys [chats]} db]
    (handlers-macro/merge-fx cofx
                             {:dispatch [:init/initialize-keychain]
                              :clear-user-password (get-in db [:account/account :address])}
                             (persist-chat-ui-props)
                             (navigation/navigate-to-clean nil)
                             (transport/stop-whisper))))

(handlers/register-handler-fx
 :logout
 (fn [cofx _]
   (logout cofx)))

(handlers/register-handler-db
 :initialize-account-db
 (fn [{:keys [accounts/accounts accounts/create contacts/contacts networks/networks
              network network-status peers-count peers-summary view-id navigation-stack
              status-module-initialized? status-node-started? device-UUID
              push-notifications/initial? semaphores]
       :or   [network (get app-db :network)]} [_ address]]
   (let [console-contact (get contacts constants/console-chat-id)
         current-account (accounts address)
         account-network-id (get current-account :network network)
         account-network (get-in current-account [:networks account-network-id])]
     (cond-> (assoc app-db
                    :current-public-key (:public-key current-account)
                    :view-id view-id
                    :navigation-stack navigation-stack
                    :status-module-initialized? (or platform/ios? js/goog.DEBUG status-module-initialized?)
                    :status-node-started? status-node-started?
                    :accounts/create create
                    :networks/networks networks
                    :account/account current-account
                    :network-status network-status
                    :network network
                    :chain (ethereum/network->chain-name account-network)
                    :push-notifications/initial? initial?
                    :peers-summary peers-summary
                    :peers-count peers-count
                    :device-UUID device-UUID
                    :semaphores semaphores)
       console-contact
       (assoc :contacts/contacts {constants/console-chat-id console-contact})))))

(handlers/register-handler-fx
 :initialize-account
 (fn [cofx [_ address events-after]]
   {:dispatch-n (cond-> [[:initialize-account-db address]
                         [:initialize-protocol address]
                         [:fetch-web3-node-version]
                         [:start-check-sync-state]
                         [:load-contacts]
                         [:initialize-chats]
                         [:initialize-browsers]
                         [:initialize-dapp-permissions]
                         [:send-account-update-if-needed]
                         [:process-pending-messages]
                         [:update-wallet]
                         [:update-transactions]
                         (when platform/mobile? [:get-fcm-token])
                         [:start-wallet-transactions-sync]
                         [:update-sign-in-time]]
                  (seq events-after) (into events-after))}))

(handlers/register-handler-fx
 :initialize-geth
 (fn [{db :db} _]
   (when-not (:status-node-started? db)
     (let [default-networks (:networks/networks db)
           default-network  (:network db)]
       {:initialize-geth-fx (get-in default-networks [default-network :config])}))))

(handlers/register-handler-fx
 :fetch-web3-node-version-callback
 (fn [{:keys [db]} [_ resp]]
   (when-let [git-commit (nth (re-find #"-([0-9a-f]{7,})/" resp) 1)]
     {:db (assoc db :web3-node-version git-commit)})))

(handlers/register-handler-fx
 :fetch-web3-node-version
 (fn [{{:keys [web3] :as db} :db} _]
   (.. web3 -version (getNode (fn [err resp]
                                (when-not err
                                  (re-frame/dispatch [:fetch-web3-node-version-callback resp])))))
   nil))

(handlers/register-handler-fx
 :get-fcm-token
 (fn [_ _]
   {::get-fcm-token-fx nil}))

(handlers/register-handler-fx
 :discovery/summary
 (fn [{:keys [db] :as cofx} [_ peers-summary]]
   (let [previous-summary (:peers-summary db)
         peers-count      (count peers-summary)]
     (handlers-macro/merge-fx cofx
                              {:db (assoc db
                                          :peers-summary peers-summary
                                          :peers-count peers-count)}
                              (transport.handlers/resend-contact-messages previous-summary)
                              (inbox/peers-summary-change-fx previous-summary)))))

(handlers/register-handler-fx
 :signal-event
 (fn [_ [_ event-str]]
   (log/debug :event-str event-str)
   (instabug/log (str "Signal event: " event-str))
   (let [{:keys [type event]} (types/json->clj event-str)
         to-dispatch (case type
                       "node.started"        [:status-node-started]
                       "node.stopped"        [:status-node-stopped]
                       "module.initialized"  [:status-module-initialized]
                       "envelope.sent"       [:signals/envelope-status (:hash event) :sent]
                       "envelope.expired"    [:signals/envelope-status (:hash event) :not-sent]
                       "discovery.summary"   [:discovery/summary event]
                       (log/debug "Event " type " not handled"))]
     (when to-dispatch
       {:dispatch to-dispatch}))))

(handlers/register-handler-fx
 :status-module-initialized
 (fn [{:keys [db]} _]
   {:db                            (assoc db :status-module-initialized? true)
    ::status-module-initialized-fx nil}))

(defn app-state-change [state {:keys [db] :as cofx}]
  (let [app-coming-from-background? (= state "active")]
    (handlers-macro/merge-fx cofx
                             {::app-state-change-fx state
                              :db                   (assoc db :app-state state)}
                             (persist-chat-ui-props state)
                             (inbox/request-messages app-coming-from-background?))))

(handlers/register-handler-fx
 :app-state-change
 (fn [cofx [_ state]]
   (app-state-change state cofx)))

(handlers/register-handler-fx
 :request-permissions
 (fn [_ [_ options]]
   {:request-permissions-fx options}))

(handlers/register-handler-db
 :set-swipe-position
 [re-frame/trim-v]
 (fn [db [item-id value]]
   (assoc-in db [:chat-animations item-id :delete-swiped] value)))

(handlers/register-handler-db
 :show-tab-bar
 (fn [db _]
   (assoc db :tab-bar-visible? true)))

(handlers/register-handler-db
 :hide-tab-bar
 (fn [db _]
   (assoc db :tab-bar-visible? false)))

(handlers/register-handler-db
 :update-window-dimensions
 (fn [db [_ dimensions]]
   (assoc db :dimensions/window (dimensions/window dimensions))))
