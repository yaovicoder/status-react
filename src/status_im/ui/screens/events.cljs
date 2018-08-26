(ns status-im.ui.screens.events
  (:require status-im.chat.events
            [status-im.models.chat :as chat]
            status-im.network.events
            [status-im.transport.handlers :as transport.handlers]
            status-im.protocol.handlers
            [status-im.models.protocol :as models.protocol]
            [status-im.models.account :as models.account]
            [status-im.ui.screens.accounts.models :as accounts.models]
            status-im.ui.screens.accounts.login.events
            status-im.ui.screens.accounts.recover.events
            [status-im.ui.screens.contacts.events :as contacts]
            [status-im.models.contacts :as models.contacts]
            status-im.ui.screens.add-new.new-chat.events
            status-im.ui.screens.group.chat-settings.events
            status-im.ui.screens.group.events
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.universal-links.core :as universal-links]
            [status-im.utils.dimensions :as dimensions]
            status-im.utils.universal-links.events
            status-im.init.events
            [status-im.init.core :as init]
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
            [status-im.models.transactions :as transactions]
            status-im.ui.screens.wallet.choose-recipient.events
            status-im.ui.screens.wallet.collectibles.cryptokitties.events
            status-im.ui.screens.wallet.collectibles.cryptostrikers.events
            status-im.ui.screens.wallet.collectibles.etheremon.events
            status-im.ui.screens.browser.events
            [status-im.models.browser :as browser]
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
            [status-im.js-dependencies :as dependencies]
            [status-im.ui.components.react :as react]
            [status-im.transport.core :as transport]
            [status-im.transport.inbox :as inbox]
            [status-im.ui.screens.db :refer [app-db]]
            [status-im.utils.datetime :as time]
            [status-im.utils.ethereum.core :as ethereum]
            [status-im.utils.random :as random]
            [status-im.utils.config :as config]
            [status-im.utils.notifications :as notifications]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.utils.http :as http]
            [status-im.utils.instabug :as instabug]
            [status-im.utils.platform :as platform]
            [status-im.utils.types :as types]
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
 :request-notifications-fx
 (fn [_]
   (notifications/request-permissions)))

(re-frame/reg-fx
 :ui/listen-to-window-dimensions-change
 (fn []
   (dimensions/add-event-listener)))

(re-frame/reg-fx
 :notifications/get-fcm-token
 (fn [_]
   (when platform/mobile?
     (notifications/get-fcm-token))))

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

;;;; Handlers

(handlers/register-handler-db
 :set
 (fn [db [_ k v]]
   (assoc db k v)))

(handlers/register-handler-db
 :set-in
 (fn [db [_ path v]]
   (assoc-in db path v)))

(handlers/register-handler-fx
 :logout
 (fn [{:keys [db] :as cofx} _]
   (let [{:transport/keys [chats]} db]
     (handlers-macro/merge-fx cofx
                              {:dispatch [:init/initialize-keychain]}
                              (navigation/navigate-to-clean nil)
                              (transport/stop-whisper)))))

(defn summary [peers-summary {:keys [db] :as cofx}]
  (let [previous-summary (:peers-summary db)
        peers-count      (count peers-summary)]
    (handlers-macro/merge-fx cofx
                             {:db (assoc db
                                         :peers-summary peers-summary
                                         :peers-count peers-count)}
                             (transport.handlers/resend-contact-messages previous-summary)
                             (inbox/peers-summary-change-fx previous-summary))))

(defn process [event-str cofx]
  (let [{:keys [type event]} (types/json->clj event-str)]
    (case type
      "node.started"       (init/status-node-started cofx)
      "node.stopped"       (init/status-node-stopped cofx)
      "module.initialized" (init/status-module-initialized cofx)
      "envelope.sent"      (transport.handlers/update-envelope-status (:hash event) :sent cofx)
      "envelope.expired"   (transport.handlers/update-envelope-status (:hash event) :sent cofx)
      "discovery.summary"  (summary event cofx)
      (log/debug "Event " type " not handled"))))

(handlers/register-handler-fx
 :signal-event
 (fn [cofx [_ event-str]]
   (log/debug :event-str event-str)
   (instabug/log (str "Signal event: " event-str))
   (process event-str cofx)))

(defn app-state-change [state {:keys [db] :as cofx}]
  (let [app-coming-from-background? (= state "active")]
    (handlers-macro/merge-fx cofx
                             {::app-state-change-fx state
                              :db                   (assoc db :app-state state)}
                             (inbox/request-messages app-coming-from-background?))))

(handlers/register-handler-fx
 :app-state-change
 (fn [cofx [_ state]]
   (app-state-change state cofx)))

(handlers/register-handler-fx
 :request-permissions
 (fn [_ [_ options]]
   {:request-permissions-fx options}))

(handlers/register-handler-fx
 :request-notifications
 (fn [_ _]
   {:request-notifications-fx {}}))

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
