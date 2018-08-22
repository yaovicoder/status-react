(ns status-im.ui.screens.views
  (:require-macros [status-im.utils.views :refer [defview letsubs] :as views])
  (:require [re-frame.core :refer [dispatch]]
            [status-im.utils.platform :refer [android?]]
            [status-im.utils.universal-links.core :as utils.universal-links]
            [status-im.ui.components.react :refer [view modal create-main-screen-view] :as react]
            [status-im.ui.components.styles :as common-styles]
            [status-im.ui.screens.main-tabs.views :as main-tabs]

            [status-im.ui.screens.accounts.login.views :refer [login]]
            [status-im.ui.screens.accounts.recover.views :refer [recover]]
            [status-im.ui.screens.accounts.views :refer [accounts]]

            [status-im.chat.screen :refer [chat]]
            [status-im.ui.screens.add-new.views :refer [add-new]]
            [status-im.ui.screens.add-new.new-chat.views :refer [new-chat]]
            [status-im.ui.screens.add-new.new-public-chat.view :refer [new-public-chat]]

            [status-im.ui.screens.qr-scanner.views :refer [qr-scanner]]

            [status-im.ui.screens.group.views :refer [new-group]]
            [status-im.ui.screens.group.add-contacts.views :refer [contact-toggle-list
                                                                   add-participants-toggle-list]]
            [status-im.ui.screens.profile.user.views :as profile.user]
            [status-im.ui.screens.profile.contact.views :as profile.contact]
            [status-im.ui.screens.profile.group-chat.views :as profile.group-chat]
            [status-im.ui.screens.profile.photo-capture.views :refer [profile-photo-capture]]
            [status-im.ui.screens.wallet.main.views :as wallet.main]
            [status-im.ui.screens.wallet.collectibles.views :refer [collectibles-list]]
            [status-im.ui.screens.wallet.send.views :refer [send-transaction send-transaction-modal sign-message-modal]]
            [status-im.ui.screens.wallet.choose-recipient.views :refer [choose-recipient]]
            [status-im.ui.screens.wallet.request.views :refer [request-transaction send-transaction-request]]
            [status-im.ui.screens.wallet.components.views :as wallet.components]
            [status-im.ui.screens.wallet.onboarding.setup.views :as wallet.onboarding.setup]
            [status-im.ui.screens.wallet.transaction-fee.views :as wallet.transaction-fee]
            [status-im.ui.screens.wallet.settings.views :as wallet-settings]
            [status-im.ui.screens.wallet.transactions.views :as wallet-transactions]
            [status-im.ui.screens.wallet.transaction-sent.views :refer [transaction-sent transaction-sent-modal]]
            [status-im.ui.screens.wallet.components.views :refer [contact-code recent-recipients recipient-qr-code]]
            [status-im.ui.screens.network-settings.views :refer [network-settings]]
            [status-im.ui.screens.network-settings.network-details.views :refer [network-details]]
            [status-im.ui.screens.network-settings.edit-network.views :refer [edit-network]]
            [status-im.ui.screens.offline-messaging-settings.views :refer [offline-messaging-settings]]
            [status-im.ui.screens.offline-messaging-settings.edit-mailserver.views :refer [edit-mailserver]]
            [status-im.ui.screens.bootnodes-settings.views :refer [bootnodes-settings]]
            [status-im.ui.screens.bootnodes-settings.edit-bootnode.views :refer [edit-bootnode]]
            [status-im.ui.screens.currency-settings.views :refer [currency-settings]]
            [status-im.ui.screens.help-center.views :refer [help-center]]
            [status-im.ui.screens.browser.views :refer [browser]]
            [status-im.ui.screens.add-new.open-dapp.views :refer [open-dapp dapp-description]]
            [status-im.ui.screens.intro.views :refer [intro]]
            [status-im.ui.screens.accounts.create.views :refer [create-account]]
            [status-im.ui.screens.profile.seed.views :refer [backup-seed]]
            [status-im.ui.screens.about-app.views :as about-app]
            [status-im.utils.navigation :as navigation]
            [reagent.core :as reagent]
            [cljs-react-navigation.reagent :as nav-reagent]
            [status-im.utils.random :as rand]
            [re-frame.core :as re-frame]))

(defn wrap [view-id component]
  (fn []
    (let [main-view (create-main-screen-view view-id)]
      [main-view common-styles/flex
       [component]
       [:> navigation/navigation-events
        {:on-will-focus
         (fn []
           (re-frame/dispatch [:set :view-id view-id]))}]])))

(defn stack-screens [screens-map]
  (->> screens-map
       (map (fn [[k v]]
              (let [screen (cond
                             (map? v)
                             (let [{:keys [screens config]} v]
                               (nav-reagent/stack-navigator
                                (stack-screens screens)
                                config))

                             :else
                             (nav-reagent/stack-screen (wrap k v)))]
                [k {:screen screen}])))
       (into {})))

(defn wrap-modal [modal-view component]
  (fn []
    [react/main-screen-modal-view modal-view
     [component]]))

(defn get-main-component2 [view-id]
  (nav-reagent/switch-navigator
   {:intro-login-stack
    {:screen
     (nav-reagent/stack-navigator
      (stack-screens
       {:intro          intro
        :login          login
        :create-account create-account
        :recover        recover
        :accounts       accounts})
      (cond-> {:headerMode "none"}
        (#{:intro :login} view-id)
        (assoc :initialRouteName (name view-id))))}
    :chat-stack
    {:screen
     (nav-reagent/stack-navigator
      (stack-screens
       {:main-stack
        {:screens
         {:home                         (main-tabs/get-main-tab :home)
          :chat                         chat
          :profile                      profile.contact/profile
          :wallet-onboarding-setup      wallet.onboarding.setup/screen
          :wallet-send-transaction-chat send-transaction
          :wallet-transaction-sent      transaction-sent
          :new                          add-new
          :new-chat                     new-chat
          :qr-scanner                   qr-scanner
          :new-public-chat              new-public-chat
          :open-dapp                    open-dapp
          :dapp-description             dapp-description
          :browser                      browser}
         :config
         {:headerMode       "none"
          :initialRouteName "home"}}

        :wallet-modal
        wallet.main/wallet-modal

        :wallet-send-modal-stack
        {:screens
         {:wallet-send-transaction-modal
          (wrap-modal :wallet-send-transaction-modal send-transaction-modal)

          :wallet-transaction-sent-modal
          (wrap-modal :wallet-transaction-sent-modal transaction-sent-modal)

          :wallet-transaction-fee
          (wrap-modal :wallet-transaction-fee wallet.transaction-fee/transaction-fee)}
         :config
         {:headerMode       "none"
          :initialRouteName "wallet-send-transaction-modal"}}

        :wallet-send-modal-stack-with-onboarding
        {:screens
         {:wallet-onboarding-setup-modal
          (wrap-modal :wallet-onboarding-setup-modal wallet.onboarding.setup/modal)

          :wallet-send-transaction-modal
          (wrap-modal :wallet-send-transaction-modal send-transaction-modal)

          :wallet-transaction-sent-modal
          (wrap-modal :wallet-transaction-sent-modal transaction-sent-modal)

          :wallet-transaction-fee
          (wrap-modal :wallet-transaction-fee wallet.transaction-fee/transaction-fee)}
         :config
         {:headerMode       "none"
          :initialRouteName "wallet-onboarding-setup-modal"}}

        :wallet-sign-message-modal
        (wrap-modal :wallet-sign-message-modal sign-message-modal)})
      {:mode             "modal"
       :headerMode       "none"
       :initialRouteName "main-stack"})}
    :wallet-stack
    {:screen
     (nav-reagent/stack-navigator
      {:main-stack
       {:screen
        (nav-reagent/stack-navigator
         (stack-screens
          {:wallet                          (main-tabs/get-main-tab :wallet)
           :collectibles-list               collectibles-list
           :wallet-onboarding-setup         wallet.onboarding.setup/screen
           :wallet-send-transaction         send-transaction
           :recent-recipients               recent-recipients
           :recipient-qr-code               recipient-qr-code
           :wallet-send-transaction-chat    send-transaction
           :contact-code                    contact-code
           :wallet-transaction-sent         transaction-sent
           :wallet-request-transaction      request-transaction
           :wallet-send-transaction-request send-transaction-request
           :unsigned-transactions           wallet-transactions/transactions
           :transactions-history            wallet-transactions/transactions
           :wallet-transaction-details      wallet-transactions/transaction-details
           :wallet-send-assets              wallet.components/send-assets
           :wallet-request-assets           wallet.components/request-assets})
         {:headerMode       "none"
          :initialRouteName "wallet"})}
       :wallet-settings-assets
       {:screen (nav-reagent/stack-screen
                 (wrap :wallet-settings-assets wallet-settings/manage-assets))}

       :wallet-transaction-fee
       {:screen (nav-reagent/stack-screen
                 (wrap :wallet-transaction-fee
                       wallet.transaction-fee/transaction-fee))}

       :wallet-transactions-filter
       {:screen (nav-reagent/stack-screen
                 (wrap :wallet-transactions-filter
                       wallet-transactions/filter-history))}}

      {:mode             "modal"
       :headerMode       "none"
       :initialRouteName "main-stack"})}
    :profile-stack
    {:screen
     (nav-reagent/stack-navigator
      {:main-stack
       {:screen
        (nav-reagent/stack-navigator
         (stack-screens
          {:my-profile                 (main-tabs/get-main-tab :my-profile)
           :profile-photo-capture      profile-photo-capture
           :about-app                  about-app/about-app
           :bootnodes-settings         bootnodes-settings
           :edit-bootnode              edit-bootnode
           :offline-messaging-settings offline-messaging-settings
           :edit-mailserver            edit-mailserver
           :help-center                help-center
           :network-settings           network-settings
           :network-details            network-details
           :edit-network               edit-network
           :currency-settings          currency-settings
           :backup-seed                backup-seed
           :login                      login
           :create-account             create-account
           :recover                    recover
           :accounts                   accounts
           :qr-scanner                 qr-scanner})
         {:headerMode       "none"
          :initialRouteName "my-profile"})}
       :profile-qr-viewer
       {:screen (nav-reagent/stack-screen (wrap :profile-qr-viewer profile.user/qr-viewer))}}
      {:mode             "modal"
       :headerMode       "none"
       :initialRouteName "main-stack"})}}
   {:initialRouteName (if (= view-id :home)
                        "chat-stack"
                        "intro-login-stack")}))

(defn get-main-component [view-id]
  (case view-id
    :new-group new-group
    :add-participants-toggle-list add-participants-toggle-list
    :contact-toggle-list contact-toggle-list
    :group-chat-profile profile.group-chat/group-chat-profile
    :contact-code contact-code
    [react/view [react/text (str "Unknown view: " view-id)]]))

(defonce rand-label (rand/id))

(defn main []
  (let [view-id        (re-frame/subscribe [:get :view-id])
        main-component (atom nil)]
    (reagent/create-class
     {:component-did-mount
      utils.universal-links/initialize
      :component-will-mount
      (fn []
        (when (and @view-id (not @main-component))
          (reset! main-component (get-main-component2 @view-id))))
      :component-will-unmount
      utils.universal-links/finalize
      :component-will-update
      (fn []
        (when (and @view-id (not @main-component))
          (reset! main-component (get-main-component2 @view-id)))
        (react/dismiss-keyboard!))
      :reagent-render
      (fn []
        (when (and @view-id main-component)
          [:> @main-component
           {:ref            navigation/set-navigator-ref
            ;; see https://reactnavigation.org/docs/en/state-persistence.html#development-mode
            :persistenceKey (when js/goog.DEBUG rand-label)}]))})))
