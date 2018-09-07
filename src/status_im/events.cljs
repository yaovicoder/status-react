(ns status-im.events
  (:require status-im.ui.screens.accounts.create.navigation
            [pluto.registry :as registry]
            [re-frame.core :as re-frame]
            [status-im.chat.events :as chat.events]
            [status-im.chat.models.message :as models.message]
            [status-im.constants :as constants]
            [status-im.data-store.browser :as browser-store]
            [status-im.data-store.chats :as chats-store]
            [status-im.data-store.core :as data-store]
            [status-im.extensions.registry :as extensions]
            [status-im.i18n :as i18n]
            [status-im.init.core :as init]
            [status-im.models.bootnode :as models.bootnode]
            [status-im.models.browser :as browser.models]
            [status-im.models.contact :as models.contact]
            [status-im.models.fleet :as fleet]
            [status-im.models.network :as models.network]
            [status-im.models.protocol :as protocol]
            [status-im.native-module.core :as status]
            [status-im.network.net-info :as net-info]
            [status-im.notifications.core :as notifications]
            [status-im.signals.core :as signals]
            [status-im.transport.core :as transport]
            [status-im.transport.inbox :as inbox]
            [status-im.transport.message.core :as transport.message]
            [status-im.transport.message.v1.group-chat :as group-chat]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.ui.components.permissions :as permissions]
            [status-im.ui.components.react :as react]
            [status-im.ui.screens.accounts.models :as accounts.models]
            [status-im.accounts.core :as accounts]
            [status-im.ui.screens.accounts.utils :as accounts.utils]
            [status-im.ui.screens.add-new.new-chat.db :as new-chat.db]
            [status-im.ui.screens.browser.default-dapps :as default-dapps]
            [status-im.ui.screens.currency-settings.models
             :as
             currency-settings.models]
            [status-im.mailserver.core :as mailserver]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.ui.screens.profile.models :as profile.models]
            [status-im.utils.config :as config]
            [status-im.utils.datetime :as time]
            [status-im.utils.dimensions :as dimensions]
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

;; status-im.ui.screens.accounts.create.navigation

;; utils module

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

(defn- http-post [{:keys [url data response-validator success-event-creator failure-event-creator timeout-ms opts]}]
  (let [on-success #(re-frame/dispatch (success-event-creator %))
        on-error   #(re-frame/dispatch (failure-event-creator %))
        all-opts   (assoc opts
                          :valid-response? response-validator
                          :timeout-ms      timeout-ms)]
    (http/post url data on-success on-error all-opts)))

(re-frame/reg-fx
 :http-post
 http-post)

(re-frame/reg-fx
 :request-permissions-fx
 (fn [options]
   (permissions/request-permissions options)))

;; init module

(re-frame/reg-fx
 :init/init-store
 init/init-store!)

(re-frame/reg-fx
 :init/status-module-initialized
 status/module-initialized!)

(re-frame/reg-fx
 :init/testfairy-alert
 init/testfairy-alert!)

(re-frame/reg-fx
 :init/get-device-UUID
 (fn []
   (status/get-device-UUID #(re-frame/dispatch [:init.callback/get-device-UUID-success %]))))

(re-frame/reg-fx
 :init/reset-data
 init/reset-data!)

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
  (re-frame/inject-cofx :data-store/get-unanswered-requests)
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

;;;; COFX

(re-frame/reg-cofx
 :accounts/get-signing-phrase
 (fn [cofx _]
   (accounts/get-signing-phrase cofx)))

(re-frame/reg-cofx
 :accounts/get-status
 (fn [cofx _]
   (accounts/get-status cofx)))

;;;; FX

(re-frame/reg-fx
 :accounts/create-account
 accounts/create-account!)

;;;; Handlers

(handlers/register-handler-fx
 :accounts.callback/account-created
 [(re-frame/inject-cofx :accounts/get-signing-phrase) (re-frame/inject-cofx :accounts/get-status)]
 (fn [cofx [_ result password]]
   (accounts/on-account-created result password false cofx)))

(handlers/register-handler-fx
 :accounts.ui/next-step-pressed
 (fn [cofx [_ step password password-confirm]]
   (accounts/next-step step password password-confirm cofx)))

(handlers/register-handler-fx
 :accounts.ui/step-back-pressed
 (fn [cofx [_ step password password-confirm]]
   (accounts/step-back step cofx)))

(handlers/register-handler-fx
 :accounts.ui/input-text-changed
 (fn [cofx [_ input-key text]]
   (accounts/account-set-input-text input-key text cofx)))

(handlers/register-handler-fx
 :accounts.ui/mainnet-warning-shown
 (fn [cofx _]
   (accounts/account-update {:mainnet-warning-shown? true} cofx)))

(handlers/register-handler-fx
 :accounts.ui/dev-mode-switched
 (fn [cofx [_ dev-mode?]]
   (accounts/switch-dev-mode dev-mode? cofx)))

(handlers/register-handler-fx
 :accounts.ui/wallet-set-up-confirmed
 (fn [cofx [_ modal?]]
   (accounts/confirm-wallet-set-up modal? cofx)))

(handlers/register-handler-fx
 :accounts.ui/logout-confirmed
 (fn [cofx _]
   (accounts/logout cofx)))

(handlers/register-handler-fx
 :accounts.ui/logout-pressed
 (fn [cofx _]
   (accounts/show-logout-confirmation)))

;; UI module events


;;;; FX

(re-frame/reg-fx
 :ui/listen-to-window-dimensions-change
 (fn []
   (dimensions/add-event-listener)))

(re-frame/reg-fx
 :ui/show-error
 (fn [content]
   (utils/show-popup "Error" content)))

(re-frame/reg-fx
 :ui/show-confirmation
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

(handlers/register-handler-db
 :set-swipe-position
 (fn [db [_ item-id value]]
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

(handlers/register-handler-fx
 :scan-qr-code
 (fn [{:keys [db]} [_ identifier handler]]
   {:db                     (assoc-in db [:qr-codes identifier] handler)
    :request-permissions-fx {:permissions [:camera]
                             :on-allowed  #(re-frame/dispatch [:navigate-to :qr-scanner {:current-qr-context identifier}])
                             :on-denied   (fn []
                                            (utils/set-timeout
                                             #(utils/show-popup (i18n/label :t/error)
                                                                (i18n/label :t/camera-access-error))
                                             50))}}))

(handlers/register-handler-fx
 :clear-qr-code
 (fn [{:keys [db]} [_ identifier]]
   {:db (update db :qr-codes dissoc identifier)}))

(handlers/register-handler-fx
 :set-qr-code
 (fn [{:keys [db]} [_ context data]]
   (merge {:db (-> db
                   (update :qr-codes dissoc context)
                   (dissoc :current-qr-context))}
          (when-let [handler (get-in db [:qr-codes context])]
            {:dispatch [handler context data]}))))

(def ^:const privacy-policy-link "https://www.iubenda.com/privacy-policy/45710059")

(re-frame/reg-fx
 ::open-privacy-policy
 (fn [] (.openURL react/linking privacy-policy-link)))

(handlers/register-handler-fx
 :open-privacy-policy-link
 (fn [] {::open-privacy-policy nil}))

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

(handlers/register-handler-fx
 :mailserver.callback/settings-saved
 (fn [cofx _]
   (accounts/logout cofx)))

(handlers/register-handler-fx
 :save-new-network
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx]
   (models.network/save cofx
                        {:data       (get-in cofx [:db :network/manage])
                         :on-success (fn []
                                       {:dispatch [:navigate-back]})})))

(handlers/register-handler-fx
 :network-set-input
 (fn [cofx [_ input-key value]]
   (models.network/set-input input-key value cofx)))

(handlers/register-handler-fx
 :edit-network
 (fn [cofx]
   (models.network/edit cofx)))

(handlers/register-handler-fx
 :close-application
 (fn [_ _]
   {:close-application nil}))

(handlers/register-handler-fx
 ::save-network
 (fn [{:keys [db now] :as cofx} [_ network]]
   (handlers-macro/merge-fx cofx
                            (accounts.utils/account-update {:network      network
                                                            :last-updated now}
                                                           [::close-application]))))

(handlers/register-handler-fx
 ::remove-network
 (fn [{:keys [db now] :as cofx} [_ network]]
   (let [networks         (dissoc (get-in db [:account/account :networks]) network)]
     (handlers-macro/merge-fx cofx
                              {:dispatch [:navigate-back]}
                              (accounts.utils/account-update {:networks     networks
                                                              :last-updated now})))))

(handlers/register-handler-fx
 :connect-network
 (fn [cofx [_ network]]
   (models.network/connect cofx {:network network})))

(handlers/register-handler-fx
 :delete-network
 (fn [cofx [_ network]]
   (models.network/delete cofx {:network network})))

(handlers/register-handler-fx
 ::save-fleet
 (fn [{:keys [db now] :as cofx} [_ fleet]]
   (let [settings (get-in db [:account/account :settings])]
     (handlers-macro/merge-fx cofx
                              (accounts.models/update-settings
                               (if fleet
                                 (assoc settings :fleet fleet)
                                 (dissoc settings :fleet))
                               [:accounts.ui/logout-confirmed])))))

(handlers/register-handler-fx
 :change-fleet
 (fn [{:keys [db]} [_ fleet]]
   {:ui/show-confirmation {:title               (i18n/label :t/close-app-title)
                           :content             (i18n/label :t/change-fleet
                                                            {:fleet fleet})
                           :confirm-button-text (i18n/label :t/close-app-button)
                           :on-accept           #(re-frame/dispatch [::save-fleet (keyword fleet)])
                           :on-cancel           nil}}))

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

(handlers/register-handler-fx
 :wallet.settings/set-currency
 (fn [cofx [_ currency]]
   (currency-settings.models/set-currency currency cofx)))

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

;;;; Handlers
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

(re-frame/reg-fx
 :browse
 (fn [link]
   (if (utils.universal-links/universal-link? link)
     (utils.universal-links/open! link)
     (list-selection/browse link))))

(re-frame/reg-fx
 :call-rpc
 (fn [[payload callback]]
   (status/call-rpc
    (types/clj->json payload)
    (fn [response]
      (if (= "" response)
        (do
          (log/warn :web3-response-error)
          (callback "web3-response-error" nil))
        (callback nil (.parse js/JSON response)))))))

(re-frame/reg-fx
 :send-to-bridge-fx
 (fn [[message webview]]
   (.sendToBridge webview (types/clj->json message))))

(re-frame/reg-fx
 :resolve-ens-multihash
 (fn [{:keys [web3 registry ens-name cb]}]
   (resolver/content web3 registry ens-name cb)))

(handlers/register-handler-fx
 :browse-link-from-message
 (fn [_ [_ link]]
   {:browse link}))

(handlers/register-handler-fx
 :ens-multihash-resolved
 (fn [{:keys [db] :as cofx} [_ hash]]
   (let [options (:browser/options db)
         browsers (:browser/browsers db)
         browser (get browsers (:browser-id options))
         history-index (:history-index browser)]
     (handlers-macro/merge-fx
      cofx
      {:db (assoc-in db [:browser/options :resolving?] false)}
      (browser.models/update-browser-fx
       (assoc-in browser [:history history-index] (str "https://ipfs.infura.io/ipfs/" hash)))))))

(handlers/register-handler-fx
 :open-url-in-browser
 (fn [cofx [_ url]]
   (let [normalized-url (http/normalize-and-decode-url url)
         host (http/url-host normalized-url)]
     (browser.models/update-new-browser-and-navigate
      host
      {:browser-id    (or host (random/id))
       :history-index 0
       :history       [normalized-url]}
      cofx))))

(handlers/register-handler-fx
 :send-to-bridge
 (fn [cofx [_ message]]
   {:send-to-bridge-fx [message (get-in cofx [:db :webview-bridge])]}))

(handlers/register-handler-fx
 :open-browser
 (fn [cofx [_ browser]]
   (browser.models/update-browser-and-navigate browser cofx)))

(handlers/register-handler-fx
 :update-browser-on-nav-change
 (fn [cofx [_ browser url loading error?]]
   (let [host (http/url-host url)]
     (handlers-macro/merge-fx
      cofx
      (browser.models/resolve-multihash-fx host loading error?)
      (browser.models/update-browser-history-fx browser url loading)))))

(handlers/register-handler-fx
 :update-browser-options
 (fn [{:keys [db]} [_ options]]
   {:db (update db :browser/options merge options)}))

(handlers/register-handler-fx
 :remove-browser
 (fn [{:keys [db]} [_ browser-id]]
   {:db            (update-in db [:browser/browsers] dissoc browser-id)
    :data-store/tx [(browser-store/remove-browser-tx browser-id)]}))

(defn nav-update-browser [cofx browser history-index]
  (browser.models/update-browser-fx (assoc browser :history-index history-index) cofx))

(handlers/register-handler-fx
 :browser-nav-back
 (fn [cofx [_ {:keys [history-index] :as browser}]]
   (when (pos? history-index)
     (nav-update-browser cofx browser (dec history-index)))))

(handlers/register-handler-fx
 :browser-nav-forward
 (fn [cofx [_ {:keys [history-index] :as browser}]]
   (when (< history-index (dec (count (:history browser))))
     (nav-update-browser cofx browser (inc history-index)))))

(handlers/register-handler-fx
 :on-bridge-message
 (fn [{:keys [db] :as cofx} [_ message]]
   (let [{:browser/keys [options browsers]} db
         {:keys [browser-id]} options
         browser (get browsers browser-id)
         data    (types/json->clj message)
         {{:keys [url]} :navState :keys [type host permissions payload messageId]} data]
     (cond

       (and (= type constants/history-state-changed) platform/ios? (not= "about:blank" url))
       (browser.models/update-browser-history-fx browser url false cofx)

       (= type constants/web3-send-async)
       (browser.models/web3-send-async payload messageId cofx)

       (= type constants/status-api-request)
       (let [{:keys [dapp? name]} browser
             dapp-name (if dapp? name host)]
         {:db       (update-in db [:browser/options :permissions-queue] conj {:dapp-name   dapp-name
                                                                              :permissions permissions})
          :dispatch [:check-permissions-queue]})))))

(handlers/register-handler-fx
 :check-permissions-queue
 (fn [{:keys [db] :as cofx} _]
   (let [{:keys [show-permission permissions-queue]} (:browser/options db)]
     (when (and (nil? show-permission) (last permissions-queue))
       (let [{:keys [dapp-name permissions]} (last permissions-queue)
             {:account/keys [account]} db]
         (handlers-macro/merge-fx
          cofx
          {:db (update-in db [:browser/options :permissions-queue] drop-last)}
          (browser.models/request-permission
           {:dapp-name             dapp-name
            :index                 0
            :user-permissions      (get-in db [:dapps/permissions dapp-name :permissions])
            :requested-permissions permissions
            :permissions-data      {constants/dapp-permission-contact-code (:public-key account)}})))))))

(handlers/register-handler-fx
 :next-dapp-permission
 (fn [cofx [_ params permission permissions-data]]
   (browser.models/next-permission {:params           params
                                    :permission       permission
                                    :permissions-data permissions-data}
                                   cofx)))

(defn toggle-custom-bootnodes [value {:keys [db] :as cofx}]
  (let [network  (get-in db [:account/account :network])
        settings (get-in db [:account/account :settings])]
    (handlers-macro/merge-fx cofx
                             (accounts.models/update-settings
                              (assoc-in settings [:bootnodes network] value)
                              [:accounts.ui/logout-confirmed]))))

(handlers/register-handler-fx
 :toggle-custom-bootnodes
 (fn [cofx [_ value]]
   (toggle-custom-bootnodes value cofx)))

(handlers/register-handler-fx
 :save-new-bootnode
 [(re-frame/inject-cofx :random-id)]
 (fn [cofx _]
   (models.bootnode/upsert cofx)))

(handlers/register-handler-fx
 :bootnode-set-input
 (fn [cofx [_ input-key value]]
   (models.bootnode/set-input input-key value cofx)))

(handlers/register-handler-fx
 :edit-bootnode
 (fn [cofx [_ bootnode-id]]
   (models.bootnode/edit bootnode-id cofx)))

(handlers/register-handler-fx
 :bootnodes-settings.ui/delete-confirmed
 (fn [cofx [_ bootnode-id]]
   (handlers-macro/merge-fx cofx
                            (models.bootnode/delete bootnode-id)
                            (navigation/navigate-back))))

(handlers/register-handler-fx
 :bootnodes-settings.ui/delete-pressed
 (fn [_ [_ id]]
   {:ui/show-confirmation {:title (i18n/label :t/delete-bootnode-title)
                           :content (i18n/label :t/delete-bootnode-are-you-sure)
                           :confirm-button-text (i18n/label :t/delete-bootnode)
                           :on-accept #(re-frame/dispatch [:bootnodes-settings.ui/delete-confirmed id])}}))

(handlers/register-handler-fx
 :set-bootnode-from-qr
 (fn [cofx [_ _ url]]
   (assoc (models.bootnode/set-input :url url cofx)
          :dispatch [:navigate-back])))

(handlers/register-handler-fx
 ::save-log-level
 (fn [{:keys [db now] :as cofx} [_ log-level]]
   (let [settings (get-in db [:account/account :settings])]
     (handlers-macro/merge-fx cofx
                              (accounts.models/update-settings
                               (if log-level
                                 (assoc settings :log-level log-level)
                                 (dissoc settings :log-level))
                               [:accounts.ui/logout-confirmed])))))

(handlers/register-handler-fx
 :change-log-level
 (fn [{:keys [db]} [_ log-level]]
   {:ui/show-confirmation {:title               (i18n/label :t/close-app-title)
                           :content             (i18n/label :t/change-log-level
                                                            {:log-level log-level})
                           :confirm-button-text (i18n/label :t/close-app-button)
                           :on-accept           #(re-frame/dispatch [::save-log-level log-level])
                           :on-cancel           nil}}))

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

(handlers/register-handler-fx
 :group-chat.ui/leave-group-pressed
 (fn [_ [_ chat-id]]
   {:ui/show-confirmation {:title (i18n/label :t/leave-group-title)
                           :content (i18n/label :t/leave-group-confirmation)
                           :confirm-button-text (i18n/label :t/leave-group-action)
                           :on-accept #(re-frame/dispatch [:remove-chat-and-navigate-home chat-id])}}))

(handlers/register-handler-db
 :deselect-contact
 (fn [db [_ id]]
   (update db :group/selected-contacts disj id)))

(handlers/register-handler-db
 :select-contact
 (fn [db [_ id]]
   (update db :group/selected-contacts conj id)))

(handlers/register-handler-db
 :deselect-participant
 (fn [db [_ id]]
   (update db :selected-participants disj id)))

(handlers/register-handler-db
 :select-participant
 (fn [db [_ id]]
   (update db :selected-participants conj id)))

(handlers/register-handler-fx
 :show-group-chat-profile
 (fn [{:keys [db]} [_ chat-id]]
   {:db (-> db
            (assoc :new-chat-name (get-in db [:chats chat-id :name])
                   :group/group-type :chat-group)
            (navigation/navigate-to :group-chat-profile))}))

(handlers/register-handler-fx
 :add-new-group-chat-participants
 [(re-frame/inject-cofx :random-id)]
 (fn [{{:keys [current-chat-id selected-participants] :as db} :db now :now message-id :random-id :as cofx} _]
   (let [participants             (concat (get-in db [:chats current-chat-id :contacts]) selected-participants)
         contacts                 (:contacts/contacts db)
         added-participants-names (map #(get-in contacts [% :name]) selected-participants)]
     (handlers-macro/merge-fx cofx
                              {:db            (-> db
                                                  (assoc-in [:chats current-chat-id :contacts] participants)
                                                  (assoc :selected-participants #{}))
                               :data-store/tx [(chats-store/add-chat-contacts-tx current-chat-id selected-participants)]}
                              (models.message/receive
                               (models.message/system-message current-chat-id message-id now
                                                              (str "You've added " (apply str (interpose ", " added-participants-names)))))
                              (transport.message/send (group-chat/GroupAdminUpdate. nil participants) current-chat-id)))))

(handlers/register-handler-fx
 :remove-group-chat-participants
 [(re-frame/inject-cofx :random-id)]
 (fn [{{:keys [current-chat-id] :as db} :db now :now message-id :random-id :as cofx} [_ removed-participants]]
   (let [participants               (remove removed-participants (get-in db [:chats current-chat-id :contacts]))
         contacts                   (:contacts/contacts db)
         removed-participants-names (map #(get-in contacts [% :name]) removed-participants)]
     (handlers-macro/merge-fx cofx
                              {:db            (assoc-in db [:chats current-chat-id :contacts] participants)
                               :data-store/tx [(chats-store/remove-chat-contacts-tx current-chat-id removed-participants)]}
                              (models.message/receive
                               (models.message/system-message current-chat-id message-id now
                                                              (str "You've removed " (apply str (interpose ", " removed-participants-names)))))
                              (transport.message/send (group-chat/GroupAdminUpdate. nil participants) current-chat-id)))))

(handlers/register-handler-fx
 :set-group-chat-name
 (fn [{{:keys [current-chat-id] :as db} :db} [_ new-chat-name]]
   {:db            (assoc-in db [:chats current-chat-id :name] new-chat-name)
    :data-store/tx [(chats-store/save-chat-tx {:chat-id current-chat-id
                                               :name    new-chat-name})]}))

(re-frame/reg-fx
 :open-image-picker
 ;; the image picker is only used here for now, this effect can be use in other scenarios as well
 (fn [callback-event]
   (profile.models/open-image-picker! callback-event)))

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

(handlers/register-handler-fx
 :signal-event
 (fn [cofx [_ event-str]]
   (log/debug :event-str event-str)
   (instabug/log (str "Signal event: " event-str))
   (signals/process event-str cofx)))

;;;; FX
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

;;; NODE SYNC STATE

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

(re-frame/reg-fx
 :node/start
 (fn [[config fleet]]
   (status/start-node config fleet)))

(re-frame/reg-fx
 :node/stop
 (fn []
   (status/stop-node)))

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
   (accounts.models/show-mainnet-is-default-alert cofx)))

(handlers/register-handler-fx
 :notifications/request-notifications-denied
 (fn [cofx _]
   (accounts.models/show-mainnet-is-default-alert cofx)))

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
