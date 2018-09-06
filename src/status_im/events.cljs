(ns status-im.events
  (:require status-im.ui.screens.accounts.create.navigation
            status-im.ui.screens.accounts.recover.navigation
            [pluto.registry :as registry]
            [re-frame.core :as re-frame]
            [status-im.accounts.core :as accounts]
            [status-im.accounts.create.core :as accounts.create]
            [status-im.accounts.login.core :as accounts.login]
            [status-im.accounts.logout.core :as accounts.logout]
            [status-im.accounts.recover.core :as accounts.recover]
            [status-im.accounts.update.core :as accounts.update]
            [status-im.bootnodes.core :as bootnodes]
            [status-im.chat.events :as chat.events]
            [status-im.chat.models.message :as models.message]
            [status-im.constants :as constants]
            [status-im.data-store.browser :as browser-store]
            [status-im.data-store.chats :as chats-store]
            [status-im.data-store.core :as data-store]
            [status-im.extensions.registry :as extensions]
            [status-im.fleet.core :as fleet]
            [status-im.i18n :as i18n]
            [status-im.init.core :as init]
            [status-im.log-level.core :as log-level]
            [status-im.mailserver.core :as mailserver]
            [status-im.models.browser :as browser.models]
            [status-im.models.contact :as models.contact]
            [status-im.models.protocol :as protocol]
            [status-im.native-module.core :as status]
            [status-im.network.core :as network]
            [status-im.network.net-info :as net-info]
            [status-im.notifications.core :as notifications]
            [status-im.privacy-policy.core :as privacy-policy]
            [status-im.qr-scanner.core :as qr-scanner]
            [status-im.signals.core :as signals]
            [status-im.transport.inbox :as inbox]
            [status-im.transport.message.core :as transport.message]
            [status-im.transport.message.v1.group-chat :as group-chat]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.ui.screens.add-new.new-chat.db :as new-chat.db]
            [status-im.ui.screens.browser.default-dapps :as default-dapps]
            [status-im.ui.screens.currency-settings.models
             :as
             currency-settings.models]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.ui.screens.profile.models :as profile.models]
            [status-im.utils.ethereum.resolver :as resolver]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.utils.http :as http]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.instabug :as instabug]
            [status-im.utils.js-resources :as js-res]
            [status-im.utils.platform :as platform]
            [status-im.utils.random :as random]
            [status-im.utils.types :as types]
            [status-im.utils.universal-links.core :as utils.universal-links]
            [status-im.utils.utils :as utils]
            [taoensso.timbre :as log]))

;; init module

(handlers/register-handler-fx
 :init.ui/data-reset-accepted
 (fn [cofx _]
   {:init/reset-data nil}))

(handlers/register-handler-fx
 :init.ui/data-reset-cancelled
 (fn [cofx [_ encryption-key]]
   (init/initialize-app encryption-key cofx)))

(handlers/register-handler-fx
 :init/app-started
 (fn [cofx _]
   (init/initialize-keychain cofx)))

(handlers/register-handler-fx
 :init.callback/get-encryption-key-success
 (fn [cofx [_ encryption-key]]
   (init/initialize-app encryption-key cofx)))

(handlers/register-handler-fx
 :init.callback/get-device-UUID-success
 (fn [cofx [_ device-uuid]]
   (init/set-device-uuid device-uuid cofx)))

(handlers/register-handler-fx
 :init.callback/init-store-success
 [(re-frame/inject-cofx :data-store/get-all-accounts)]
 (fn [cofx _]
   (init/load-accounts-and-initialize-views cofx)))

(handlers/register-handler-fx
 :init.callback/init-store-error
 (fn [cofx [_ encryption-key error]]
   (init/handle-init-store-error encryption-key cofx)))

(handlers/register-handler-fx
 :init.callback/account-change-success
 [(re-frame/inject-cofx :web3/get-web3)
  (re-frame/inject-cofx :get-default-contacts)
  (re-frame/inject-cofx :get-default-dapps)
  (re-frame/inject-cofx :data-store/all-chats)
  (re-frame/inject-cofx :data-store/get-messages)
  (re-frame/inject-cofx :data-store/get-user-statuses)
  (re-frame/inject-cofx :data-store/get-unviewed-messages)
  (re-frame/inject-cofx :data-store/message-ids)
  (re-frame/inject-cofx :data-store/get-local-storage-data)
  (re-frame/inject-cofx :data-store/get-all-contacts)
  (re-frame/inject-cofx :data-store/get-all-mailservers)
  (re-frame/inject-cofx :data-store/transport)
  (re-frame/inject-cofx :data-store/all-browsers)
  (re-frame/inject-cofx :data-store/all-dapp-permissions)]
 (fn [cofx [_ address]]
   (init/initialize-account address cofx)))

(handlers/register-handler-fx
 :init.callback/account-change-error
 (fn [cofx _]
   (init/handle-change-account-error cofx)))

(handlers/register-handler-fx
 :init.callback/keychain-reset
 (fn [cofx _]
   (init/initialize-keychain cofx)))

;; accounts module

(handlers/register-handler-fx
 :accounts.ui/mainnet-warning-shown
 (fn [cofx _]
   (accounts.update/account-update {:mainnet-warning-shown? true} cofx)))

(handlers/register-handler-fx
 :accounts.ui/dev-mode-switched
 (fn [cofx [_ dev-mode?]]
   (accounts/switch-dev-mode dev-mode? cofx)))

(handlers/register-handler-fx
 :accounts.ui/wallet-set-up-confirmed
 (fn [cofx [_ modal?]]
   (accounts/confirm-wallet-set-up modal? cofx)))

;; accounts create module

(handlers/register-handler-fx
 :accounts.create.ui/next-step-pressed
 (fn [cofx [_ step password password-confirm]]
   (accounts.create/next-step step password password-confirm cofx)))

(handlers/register-handler-fx
 :accounts.create.ui/step-back-pressed
 (fn [cofx [_ step password password-confirm]]
   (accounts.create/step-back step cofx)))

(handlers/register-handler-fx
 :accounts.create.ui/input-text-changed
 (fn [cofx [_ input-key text]]
   (accounts.create/account-set-input-text input-key text cofx)))

(handlers/register-handler-fx
 :accounts.create.callback/create-account-success
 [(re-frame/inject-cofx :accounts.create/get-signing-phrase)
  (re-frame/inject-cofx :accounts.create/get-status)]
 (fn [cofx [_ result password]]
   (accounts.create/on-account-created result password false cofx)))

;; accounts recover module

(handlers/register-handler-fx
 :accounts.recover/passphrase-input-changed
 (fn [cofx [_ recovery-phrase]]
   (accounts.recover/set-phrase recovery-phrase cofx)))

(handlers/register-handler-fx
 :accounts.recover/passphrase-input-blured
 (fn [cofx _]
   (accounts.recover/validate-phrase cofx)))

(handlers/register-handler-fx
 :accounts.recover/password-input-changed
 (fn [cofx [_ masked-password]]
   (accounts.recover/set-password masked-password cofx)))

(handlers/register-handler-fx
 :accounts.recover/password-input-blured
 (fn [cofx _]
   (accounts.recover/validate-password cofx)))

(handlers/register-handler-fx
 :accounts.recover/sign-in-button-pressed
 (fn [cofx _]
   (accounts.recover/recover-account-with-checks cofx)))

(handlers/register-handler-fx
 :accounts.recover/recover-account-confirmed
 (fn [cofx _]
   (accounts.recover/recover-account cofx)))

(handlers/register-handler-fx
 :accounts.recover/recover-account-success
 [(re-frame/inject-cofx :accounts.create/get-signing-phrase)
  (re-frame/inject-cofx :accounts.create/get-status)]
 (fn [cofx [_ result password]]
   (accounts.recover/on-account-recovered result password cofx)))

;; accounts login module

(handlers/register-handler-fx
 :accounts.login.ui/password-input-submitted
 (fn [cofx _]
   (accounts.login/user-login cofx)))

(handlers/register-handler-fx
 :accounts.login.callback/login-success
 (fn [cofx [_ login-result]]
   (accounts.login/user-login-callback login-result cofx)))

(handlers/register-handler-fx
 :accounts.login.ui/account-selected
 (fn [cofx [_ address photo-path name]]
   (accounts.login/open-login address photo-path name cofx)))

(handlers/register-handler-fx
 :accounts.login.callback/get-user-password-success
 (fn [cofx [_ password]]
   (accounts.login/open-login-callback password cofx)))

;; accounts logout module

(handlers/register-handler-fx
 :accounts.logout.ui/logout-pressed
 (fn [cofx _]
   (accounts.logout/show-logout-confirmation)))

(handlers/register-handler-fx
 :accounts.logout.ui/logout-confirmed
 (fn [cofx _]
   (accounts.logout/logout cofx)))

;; accounts update module

(handlers/register-handler-fx
 [:accounts.update.callback/save-settings-success]
 (fn [cofx _]
   (accounts.logout/logout cofx)))

;; mailserver module

(handlers/register-handler-fx
 :mailserver.ui/user-defined-mailserver-selected
 (fn [cofx [_ mailserver-id]]
   (mailserver/edit mailserver-id cofx)))

(handlers/register-handler-fx
 :mailserver.ui/default-mailserver-selected
 (fn [cofx [_ mailserver-id]]
   (mailserver/show-connection-confirmation mailserver-id cofx)))

(handlers/register-handler-fx
 :mailserver.ui/add-pressed
 (fn [cofx _]
   (navigation/navigate-to-cofx :edit-mailserver nil cofx)))

(handlers/register-handler-fx
 :mailserver.ui/save-pressed
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx _]
   (mailserver/upsert cofx)))

(handlers/register-handler-fx
 :mailserver.ui/input-changed
 (fn [cofx [_ input-key value]]
   (mailserver/set-input input-key value cofx)))

(handlers/register-handler-fx
 :mailserver.ui/delete-confirmed
 (fn [cofx [_ mailserver-id]]
   (mailserver/delete mailserver-id cofx)))

(handlers/register-handler-fx
 :mailserver.ui/delete-pressed
 (fn [cofx [_ mailserver-id]]
   (mailserver/show-delete-confirmation mailserver-id cofx)))

(handlers/register-handler-fx
 :mailserver.callback/qr-code-scanned
 (fn [cofx [_ _ url]]
   (mailserver/set-url-from-qr url cofx)))

(handlers/register-handler-fx
 :mailserver.ui/connect-confirmed
 (fn [cofx [_ current-fleet mailserver-id]]
   (mailserver/save-settings current-fleet mailserver-id cofx)))

;; network module

(handlers/register-handler-fx
 :network.ui/save-network-pressed
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx]
   (network/save-network cofx)))

(handlers/register-handler-fx
 :network.ui/input-changed
 (fn [cofx [_ input-key value]]
   (network/set-input input-key value cofx)))

(handlers/register-handler-fx
 :network.ui/add-network-pressed
 (fn [cofx]
   (network/edit cofx)))

(handlers/register-handler-fx
 :network.callback/non-rpc-network-saved
 (fn [_ _]
   {:ui/close-application nil}))

(handlers/register-handler-fx
 :network.ui/save-non-rpc-network-pressed
 (fn [cofx [_ network]]
   (network/save-non-rpc-network network cofx)))

(handlers/register-handler-fx
 :network.ui/remove-network-confirmed
 (fn [cofx [_ network]]
   (network/remove-network network cofx)))

(handlers/register-handler-fx
 :network.ui/connect-network-pressed
 (fn [cofx [_ network]]
   (network/connect cofx {:network network})))

(handlers/register-handler-fx
 :network.ui/delete-network-pressed
 (fn [cofx [_ network]]
   (network/delete cofx {:network network})))

;; fleet module

(handlers/register-handler-fx
 :fleet.ui/save-fleet-confirmed
 (fn [cofx [_ fleet]]
   (fleet/save fleet cofx)))

(handlers/register-handler-fx
 :fleet.ui/fleet-selected
 (fn [cofx [_ fleet]]
   (fleet/show-save-confirmation fleet cofx)))

;; bootnodes module

(handlers/register-handler-fx
 :bootnodes.ui/custom-bootnodes-switch-toggled
 (fn [cofx [_ value]]
   (bootnodes/toggle-custom-bootnodes value cofx)))

(handlers/register-handler-fx
 :bootnodes.ui/add-bootnode-pressed
 (fn [cofx [_ bootnode-id]]
   (bootnodes/edit bootnode-id cofx)))

(handlers/register-handler-fx
 :bootnodes/qr-code-scanned
 (fn [cofx [_ _ url]]
   (bootnodes/set-bootnodes-from-qr url cofx)))

(handlers/register-handler-fx
 :bootnodes.ui/input-changed
 (fn [cofx [_ input-key value]]
   (bootnodes/set-input input-key value cofx)))

(handlers/register-handler-fx
 :bootnodes.callback/save-new-bootnode
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx _]
   (bootnodes/upsert cofx)))

(handlers/register-handler-fx
 :bootnodes.ui/delete-pressed
 (fn [_ [_ id]]
   (bootnodes/show-delete-bootnode-confirmation id)))

(handlers/register-handler-fx
 :bootnodes.ui/delete-confirmed
 (fn [cofx [_ bootnode-id]]
   (bootnodes/delete-bootnode bootnode-id cofx)))

;; log-level module

(handlers/register-handler-fx
 :log-level.callback/save-log-level
 (fn [cofx [_ log-level]]
   (log-level/save-log-level log-level cofx)))

(handlers/register-handler-fx
 :log-level.ui/log-level-selected
 (fn [cofx [_ log-level]]
   (log-level/show-change-log-level-confirmation log-level cofx)))

;; qr-code module

(handlers/register-handler-fx
 :qr-code.ui/scan-qr-code-pressed
 (fn [cofx [_ identifier handler]]
   (qr-scanner/scan-qr-code identifier handler cofx)))

(handlers/register-handler-fx
 :qr-scanner.callback/scan-qr-code-success
 (fn [cofx [_ context data]]
   (qr-scanner/set-qr-code context data cofx)))

;; privacy-policy module

(handlers/register-handler-fx
 :privacy-policy/privacy-policy-button-pressed
 (fn [] {:privacy-policy/open-privacy-policy-link nil}))

;; extension module TODO(yenda) WIP

(re-frame/reg-fx
 :extension/load
 (fn [[url follow-up-event]]
   (extensions/load-from url #(re-frame/dispatch [follow-up-event (-> % extensions/read-extension extensions/parse)]))))

(handlers/register-handler-fx
 :extension/install
 (fn [cofx [_ extension-data]]
   (let [extension-key (get-in extension-data ['meta :name])]
     (handlers-macro/merge-fx cofx
                              {:ui/show-confirmation {:title     (i18n/label :t/success)
                                                      :content   (i18n/label :t/extension-installed)
                                                      :on-accept #(re-frame/dispatch [:navigate-to-clean :home])
                                                      :on-cancel nil}}
                              (registry/add extension-data)
                              (registry/activate extension-key)))))

(handlers/register-handler-db
 :extension/edit-address
 (fn [db [_ address]]
   (assoc db :extension-url address)))

(handlers/register-handler-db
 :extension/stage
 (fn [db [_ extension-data]]
   (-> db
       (assoc :staged-extension extension-data)
       (navigation/navigate-to :show-extension))))

(handlers/register-handler-fx
 :extension/show
 (fn [cofx [_ uri]]
   {:extension/load [uri :extension/stage]}))

(handlers/register-handler-fx
 :extension/toggle-activation
 (fn [cofx [_ id state]]
   (when-let [toggle-fn (get {true  registry/activate
                              false registry/deactivate}
                             state)]
     (toggle-fn id cofx))))

(handlers/register-handler-db
 :extensions/toggle-activation
 (fn [db [_ id m]]
   nil))

;; wallet modules TODO(yenda) WIP

(handlers/register-handler-fx
 :wallet.settings/set-currency
 (fn [cofx [_ currency]]
   (currency-settings.models/set-currency currency cofx)))

;; contacts module TODO(yenda) WIP

(defn add-contact-and-open-chat [whisper-id cofx]
  (handlers-macro/merge-fx cofx
                           (navigation/navigate-to-clean :home)
                           (models.contact/add-contact whisper-id)
                           (chat.events/start-chat whisper-id {})))

(re-frame/reg-cofx
 :get-default-contacts
 (fn [coeffects _]
   (assoc coeffects :default-contacts js-res/default-contacts)))

(re-frame/reg-cofx
 :get-default-dapps
 (fn [coeffects _]
   (assoc coeffects :default-dapps default-dapps/all)))

(handlers/register-handler-fx
 :add-contact
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx [_ whisper-id]]
   (models.contact/add-contact whisper-id cofx)))

(handlers/register-handler-fx
 :hide-contact
 (fn [{:keys [db]} [_ whisper-id]]
   (when (get-in db [:contacts/contacts whisper-id])
     {:db (assoc-in db [:contacts/contacts whisper-id :hide-contact?] true)})))

(handlers/register-handler-fx
 :set-contact-identity-from-qr
 [(re-frame/inject-cofx :random-id)]
 (fn [{:keys [db] :as cofx} [_ _ contact-identity]]
   (let [current-account (:account/account db)
         fx              {:db (assoc db :contacts/new-identity contact-identity)}
         validation-result (new-chat.db/validate-pub-key db contact-identity)]
     (if (some? validation-result)
       (utils/show-popup (i18n/label :t/unable-to-read-this-code) validation-result #(re-frame/dispatch [:navigate-to-clean :home]))
       (handlers-macro/merge-fx cofx
                                fx
                                (add-contact-and-open-chat contact-identity))))))

(handlers/register-handler-db
 :open-contact-toggle-list
 (fn [db _]
   (-> (assoc db
              :group/selected-contacts #{}
              :new-chat-name "")
       (navigation/navigate-to :contact-toggle-list))))

(handlers/register-handler-fx
 :open-chat-with-contact
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx [_ {:keys [whisper-identity]}]]
   (add-contact-and-open-chat whisper-identity cofx)))

(handlers/register-handler-fx
  :add-contact-handler
  [(re-frame/inject-cofx :random-id)]
  (fn [{{:contacts/keys [new-identity]} :db :as cofx} _]
    (when (seq new-identity)
      (add-contact-and-open-chat new-identity cofx))))

;; browser module

;; chat module

(handlers/register-handler-fx
  :chat.ui/clear-history-pressed
  (fn [_ _]
    {:ui/show-confirmation {:title (i18n/label :t/clear-history-title)
                            :content (i18n/label :t/clear-history-confirmation-content)
                            :confirm-button-text (i18n/label :t/clear-history-action)
                            :on-accept #(re-frame/dispatch [:clear-history])}}))

(handlers/register-handler-fx
 :chat.ui/delete-chat-pressed
 (fn [_ [_ chat-id]]
   {:ui/show-confirmation {:title (i18n/label :t/delete-chat-confirmation)
                           :content ""
                           :confirm-button-text (i18n/label :t/delete-chat-action)
                           :on-accept #(re-frame/dispatch [:remove-chat-and-navigate-home chat-id])}}))

;; image picker module
(re-frame/reg-fx
 :open-image-picker
  ;; the image picker is only used here for now, this effect can be use in other scenarios as well
 (fn [callback-event]
   (profile.models/open-image-picker! callback-event)))

;; profile module

(handlers/register-handler-fx
 :profile/send-transaction
 (fn [cofx [_ chat-id]]
   (profile.models/send-transaction chat-id cofx)))

(handlers/register-handler-fx
 :my-profile/update-name
 (fn [cofx [_ name]]
   (profile.models/update-name name cofx)))

(handlers/register-handler-fx
 :my-profile/update-picture
 (fn [cofx [this-event base64-image]]
   (profile.models/update-picture this-event base64-image cofx)))

(handlers/register-handler-fx
 :my-profile/remove-current-photo
 (fn [{:keys [db]}]
   {:db       (-> db
                  (assoc-in [:my-profile/profile :photo-path]
                            (identicon/identicon (:current-public-key db)))
                  (assoc :my-profile/editing? true))}))

(handlers/register-handler-fx
 :my-profile/start-editing-profile
 (fn [cofx _]
   (profile.models/start-editing cofx)))

(handlers/register-handler-fx
 :my-profile/save-profile
 (fn [cofx _]
   (profile.models/save cofx)))

(handlers/register-handler-fx
 :group-chat-profile/start-editing
 (fn [cofx _]
   (profile.models/start-editing-group-chat-profile cofx)))

(handlers/register-handler-fx
 :group-chat-profile/save-profile
 (fn [cofx _]
   (profile.models/save-group-chat-profile cofx)))

(handlers/register-handler-fx
 :my-profile/enter-two-random-words
 (fn [cofx _]
   (profile.models/enter-two-random-words cofx)))

(handlers/register-handler-fx
 :my-profile/set-step
 (fn [cofx [_ step]]
   (profile.models/set-step step cofx)))

(handlers/register-handler-fx
 :my-profile/finish
 (fn [cofx _]
   (profile.models/finish cofx)))

(re-frame/reg-fx
 :copy-to-clipboard
 (fn [value]
   (profile.models/copy-to-clipboard! value)))

(handlers/register-handler-fx
 :copy-to-clipboard
 (fn [_ [_ value]]
   {:copy-to-clipboard value}))

(re-frame/reg-fx
 :show-tooltip
 profile.models/show-tooltip!)

(handlers/register-handler-fx
 :show-tooltip
 (fn [_ [_ tooltip-id]]
   {:show-tooltip tooltip-id}))

;; signal module

(handlers/register-handler-fx
 :signal-event
 (fn [cofx [_ event-str]]
   (log/debug :event-str event-str)
   (instabug/log (str "Signal event: " event-str))
   (signals/process event-str cofx)))

(handlers/register-handler-fx
 :protocol.ui/close-app-confirmed
 (fn [_ _]
   {:ui/close-application nil}))

(re-frame/reg-fx
 :protocol/assert-correct-network
 (fn [{:keys [web3 network-id]}]
    ;; ensure that node was started correctly
   (when (and network-id web3) ; necessary because of the unit tests
     (.getNetwork (.-version web3)
                  (fn [error fetched-network-id]
                    (when (and (not error) ; error most probably means we are offline
                               (not= network-id fetched-network-id))
                      (utils/show-popup
                       "Ethereum node started incorrectly"
                       "Ethereum node was started with incorrect configuration, application will be stopped to recover from that condition."
                       #(re-frame/dispatch [:close-application]))))))))

(handlers/register-handler-db
 :update-sync-state
 (fn [cofx [_ error sync]]
   (protocol/update-sync-state cofx error sync)))

(handlers/register-handler-fx
 :check-sync-state
 (fn [cofx _]
   (protocol/check-sync-state cofx)))

(handlers/register-handler-fx
 :start-check-sync-state
 (fn [cofx _]
   (protocol/start-check-sync-state cofx)))

;; notifications module

(re-frame/reg-fx
 :notifications/display-notification
 notifications/display-notification)

(re-frame/reg-fx
 :notifications/handle-initial-push-notification
 notifications/handle-initial-push-notification)

(re-frame/reg-fx
 :notifications/get-fcm-token
 (fn [_]
   (when platform/mobile?
     (notifications/get-fcm-token))))

(handlers/register-handler-fx
 :notifications.callback/stored-notification-handled
 (fn [cofx _]
   (accounts.login/user-login cofx)))

(re-frame/reg-fx
 :notifications/request-notifications
 (fn [_]
   (notifications/request-permissions)))

(handlers/register-handler-fx
 :notifications/handle-push-notification
 (fn [cofx [_ event]]
   (notifications/handle-push-notification event cofx)))

(handlers/register-handler-db
 :notifications/update-fcm-token
 (fn [db [_ fcm-token]]
   (assoc-in db [:notifications :fcm-token] fcm-token)))

(handlers/register-handler-fx
 :notifications/request-notifications-granted
 (fn [cofx _]
   (accounts/show-mainnet-is-default-alert cofx)))

(handlers/register-handler-fx
 :notifications/request-notifications-denied
 (fn [cofx _]
   (accounts/show-mainnet-is-default-alert cofx)))

(re-frame/reg-fx
 :network/listen-to-network-status
 (fn [[connection-listener net-info-listener]]
   (net-info/is-connected? connection-listener)
   (net-info/net-info net-info-listener)
   (net-info/add-connection-listener connection-listener)
   (net-info/add-net-info-listener net-info-listener)))

(re-frame/reg-fx
  ::notify-status-go
  (fn [data]
    (status/connection-change data)))

(handlers/register-handler-fx
  :network/update-connection-status
  (fn [{db :db :as cofx} [_ is-connected?]]
    (handlers-macro/merge-fx
     cofx
     {:db (assoc db :network-status (if is-connected? :online :offline))}
     (inbox/request-messages))))

(handlers/register-handler-fx
  :network/update-network-status
  (fn [_ [_ data]]
    {::notify-status-go data}))
