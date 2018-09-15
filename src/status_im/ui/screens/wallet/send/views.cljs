(ns status-im.ui.screens.wallet.send.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [status-im.i18n :as i18n]
            [status-im.ui.components.animation :as animation]
            [status-im.ui.components.bottom-buttons.view :as bottom-buttons]
            [status-im.ui.components.button.view :as button]
            [status-im.ui.components.common.common :as common]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.status-bar.view :as status-bar]
            [status-im.ui.components.styles :as components.styles]
            [status-im.ui.components.toolbar.actions :as act]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.ui.components.tooltip.views :as tooltip]
            [status-im.ui.screens.wallet.components.styles
             :as
             wallet.components.styles]
            [status-im.ui.screens.wallet.components.views :as wallet.components]
            [status-im.ui.screens.wallet.send.animations :as send.animations]
            [status-im.ui.screens.wallet.send.styles :as styles]
            [status-im.ui.screens.wallet.styles :as wallet.styles]
            [status-im.ui.components.colors :as colors]
            [status-im.utils.ethereum.core :as ethereum]
            [status-im.utils.ethereum.tokens :as tokens]
            [status-im.utils.money :as money]
            [status-im.utils.platform :as platform]
            [status-im.utils.security :as security]
            [status-im.utils.utils :as utils]
            [taoensso.timbre :as log])
  (:require-macros [status-im.utils.views :refer [defview letsubs]]))

(defn- toolbar [modal? title]
  (let [action (if modal? act/close-white act/back-white)]
    [toolbar/toolbar {:style wallet.styles/toolbar}
     [toolbar/nav-button (action (if modal?
                                   #(re-frame/dispatch [:wallet/discard-transaction-navigate-back])
                                   #(act/default-handler)))]
     [toolbar/content-title {:color :white} title]]))

(defn- advanced-cartouche [{:keys [max-fee gas gas-price]}]
  [react/view
   [wallet.components/cartouche {:on-press  #(do (re-frame/dispatch [:wallet.send/clear-gas])
                                                 (re-frame/dispatch [:navigate-to-modal :wallet-transaction-fee]))}
    (i18n/label :t/wallet-transaction-fee)
    [react/view {:style               styles/advanced-options-text-wrapper
                 :accessibility-label :transaction-fee-button}
     [react/text {:style styles/advanced-fees-text}
      (str max-fee  " " (i18n/label :t/eth))]
     [react/text {:style styles/advanced-fees-details-text}
      (str (money/to-fixed gas) " * " (money/to-fixed (money/wei-> :gwei gas-price)) (i18n/label :t/gwei))]]]])

(defn- advanced-options [advanced? transaction scroll]
  [react/view {:style styles/advanced-wrapper}
   [react/touchable-highlight {:on-press (fn []
                                           (re-frame/dispatch [:wallet.send/toggle-advanced (not advanced?)])
                                           (when (and scroll @scroll) (utils/set-timeout #(.scrollToEnd @scroll) 350)))}
    [react/view {:style styles/advanced-button-wrapper}
     [react/view {:style               styles/advanced-button
                  :accessibility-label :advanced-button}
      [react/i18n-text {:style (merge wallet.components.styles/label
                                      styles/advanced-label)
                        :key   :wallet-advanced}]
      [vector-icons/icon (if advanced? :icons/up :icons/down) {:color :white}]]]]
   (when advanced?
     [advanced-cartouche transaction])])

(defn send-button [spinning? sign-handler]
  [react/view {:flex 1}
   [button/secondary-button {:style              styles/password-button
                             :on-press            sign-handler
                             :disabled?           spinning?
                             :accessibility-label :sign-transaction-button}
    (i18n/label :t/command-button-send)]])

(def cancel-password-event #(re-frame/dispatch [:wallet/cancel-entering-password]))

(defview password-input-drawer [{:keys [transaction sign-handler] :as opt}]
  (letsubs [account         [:get-current-account]
            wrong-password? [:wallet.send/wrong-password?]
            signing-phrase  (:signing-phrase @account) ;;TODO
            bottom-value    (animation/create-value -250)
            opacity-value   (animation/create-value 0)]
    {:component-did-mount #(send.animations/animate-sign-panel opacity-value bottom-value)}
    (let [{:keys [in-progress? show-password-input? symbol amount-text]} transaction]
      [react/animated-view {:style (styles/animated-sign-panel bottom-value)}
       [react/animated-view {:style (styles/sign-panel opacity-value)}
        [react/view {:style {:flex-direction :column
                             :align-items :center
                             :justify-content :center
                             :padding-horizontal 15}}
         [react/view styles/signing-phrase-container
          [react/text {:style               styles/signing-phrase
                       :accessibility-label :signing-phrase-text}
           signing-phrase]]
         (when (and amount-text symbol)
           [react/text {:style styles/transaction-amount}
            (str "Send " amount-text " " (name symbol))])
         [react/view {:style                       styles/password-container
                      :important-for-accessibility :no-hide-descendants}
          [react/text-input
           {:auto-focus             true
            :secure-text-entry      true
            :placeholder            (i18n/label :t/enter-password-placeholder)
            :placeholder-text-color components.styles/color-gray4
            :on-change-text         #(re-frame/dispatch [:wallet.send/set-password (security/mask-data %)])
            :style                  styles/password
            :accessibility-label    :enter-password-input
            :auto-capitalize        :none}]]
         [send-button in-progress? sign-handler]]]
       [tooltip/tooltip (i18n/label :t/password-input-drawer-tooltip) styles/emojis-tooltip]
       [react/view styles/spinner-container
        (when in-progress?
          [react/activity-indicator {:animating true
                                     :size      :large}])]])))

(defn opacify-background []
  [react/view {:flex 1
               :background-color :black
               :opacity 0.5
               :position :absolute
               :top 0
               :left 0
               :right 0
               :bottom 0
               :z-index 3}])

(defview sign-view-container [{:keys [modal? transaction toolbar-title-label sign-handler]} current-view]
  (let [{:keys [in-progress? show-password-input?]} transaction]
    [react/view {:flex 1
                 :flex-direction :row}
     [(if modal?
        react/view
        react/keyboard-avoiding-view) {:flex 1
                                       :background-color colors/blue}
      [status-bar/status-bar {:type (if modal? :modal-wallet :wallet)}]
      [toolbar modal? (i18n/label toolbar-title-label)]
      current-view]
     (when show-password-input?
       [opacify-background])
     (when show-password-input?
       [password-input-drawer {:transaction transaction
                               :sign-handler sign-handler}])
     (when in-progress?
       [react/view styles/processing-view])]))

(defn bottom-button [{:keys [disabled? on-press label]}]
  [bottom-buttons/bottom-buttons styles/sign-buttons
   [react/view]
   [button/button {:style               (wallet.styles/button-container disabled?)
                   :on-press            on-press
                   :disabled?           disabled?
                   :accessibility-label :sign-transaction-button}
    (i18n/label label)
    [vector-icons/icon :icons/forward {:color (if disabled? :gray :white)}]]])

(defn valid-transaction? [amount-error modal? amount sufficient-funds? sufficient-gas? to]
  (and (nil? amount-error)
       (or modal? (not (empty? to))) ;;NOTE(goranjovic) - contract creation will have empty `to`
       (not (nil? amount))
       sufficient-funds?
       sufficient-gas?))

(defn- render-send-transaction-view [{:keys [modal? transaction scroll advanced? network amount-input]}]
  (let [{:keys [amount amount-text amount-error asset-error show-password-input? to to-name sufficient-funds?
                sufficient-gas? in-progress? from-chat? symbol]} transaction
        {:keys [decimals] :as token} (tokens/asset-for (ethereum/network->chain-keyword network) symbol)]
    [sign-view-container {:modal? modal?
                          :in-progress? in-progress?
                          :show-password-input? show-password-input?
                          :transaction transaction
                          :toolbar-title-label :t/send-transaction
                          :sign-handler #(re-frame/dispatch [:wallet/send-transaction])}
     [react/view components.styles/flex
      [common/network-info {:text-color :white}]
      [react/scroll-view {:keyboard-should-persist-taps :always
                          :ref                          #(reset! scroll %)
                          :on-content-size-change       #(when (and (not modal?) scroll @scroll)
                                                           (.scrollToEnd @scroll))}
       [react/view styles/send-transaction-form
        [wallet.components/recipient-selector {:disabled? (or from-chat? modal?)
                                               :address   to
                                               :name      to-name
                                               :modal?    modal?}]
        [wallet.components/asset-selector {:disabled? (or from-chat? modal?)
                                           :error     asset-error
                                           :type      :send
                                           :symbol    symbol}]
        [wallet.components/amount-selector {:disabled?     (or from-chat? modal?)
                                            :error         (or amount-error
                                                               (when-not sufficient-funds? (i18n/label :t/wallet-insufficient-funds))
                                                               (when-not sufficient-gas? (i18n/label :t/wallet-insufficient-gas)))
                                            :amount        amount
                                            :amount-text   amount-text
                                            :input-options {:on-change-text #(re-frame/dispatch [:wallet.send/set-and-validate-amount % symbol decimals])
                                                            :ref            (partial reset! amount-input)}} token]
        [advanced-options advanced? transaction scroll]]]
      [bottom-button {:disabled? (not (valid-transaction? amount-error modal? amount sufficient-funds? sufficient-gas? to))
                      :on-press #(re-frame/dispatch [:wallet.send.ui/sign-button-pressed])
                      :label :t/transactions-sign-transaction}]]]))

;; MAIN SEND TRANSACTION VIEW
(defn- send-transaction-view [{:keys [scroll] :as opts}]
  (let [amount-input (atom nil)
        handler      (fn [_]
                       (when (and scroll @scroll @amount-input
                                  (.isFocused @amount-input))
                         (log/debug "Amount field focused, scrolling down")
                         (.scrollToEnd @scroll)))]
    (reagent/create-class
     {:component-will-mount (fn [_]
                              (when platform/android?
                                (.addListener react/keyboard "keyboardDidShow" handler))
                              (when platform/ios?
                                (.addListener react/keyboard "keyboardWillShow" handler)))
      :reagent-render       (fn [opts] (render-send-transaction-view
                                        (assoc opts :amount-input amount-input)))})))

;; SEND TRANSACTION FROM WALLET (CHAT)
(defview send-transaction []
  (letsubs [transaction [:wallet.send/transaction]
            advanced?   [:wallet.send/advanced?]
            network     [:get-current-account-network]
            scroll      (atom nil)]
    [send-transaction-view {:modal? false
                            :transaction transaction
                            :scroll scroll
                            :advanced? advanced?
                            :network network}]))

;; SEND TRANSACTION FROM DAPP
(defview send-transaction-modal []
  (letsubs [transaction [:wallet.send/transaction]
            advanced?   [:wallet.send/advanced?]
            network     [:get-current-account-network]
            scroll      (atom nil)]
    (if transaction
      [send-transaction-view {:modal? true
                              :transaction transaction
                              :scroll scroll
                              :advanced? advanced?
                              :network network}]
      [react/view wallet.styles/wallet-modal-container
       [react/view components.styles/flex
        [status-bar/status-bar {:type :modal-wallet}]
        [toolbar true (i18n/label :t/send-transaction)]
        [react/i18n-text {:style styles/empty-text
                          :key   :unsigned-transaction-expired}]]])))

;; SIGN MESSAGE FROM DAPP
(defview sign-message-modal []
  (letsubs [transaction [:wallet.send/transaction]]
    [sign-view-container {:modal? true
                          :transaction transaction
                          :toolbar-title-label :t/sign-message
                          :sign-handler #(re-frame/dispatch [:wallet/sign-message])}
     [react/view components.styles/flex
      [react/scroll-view
       [react/view styles/send-transaction-form
        [wallet.components/cartouche {:disabled? true}
         (i18n/label :t/message)
         [wallet.components/amount-input
          {:disabled?     true
           :input-options {:multiline true}
           :amount-text   (:data transaction)}
          nil]]]]
      [bottom-button {:on-press #(re-frame/dispatch [:wallet.send.ui/sign-button-pressed])
                      :label :t/transactions-sign}]]]))
