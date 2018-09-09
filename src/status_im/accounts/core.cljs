(ns status-im.accounts.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.constants :as constants]
            [status-im.data-store.accounts :as accounts-store]
            [status-im.i18n :as i18n]
            [status-im.native-module.core :as status]
            [status-im.accounts.statuses :as statuses]
            [status-im.accounts.update.core :as accounts.update]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.ui.screens.wallet.settings.models :as wallet.settings.models]
            [status-im.utils.config :as config]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.utils.hex :as utils.hex]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.signing-phrase.core :as signing-phrase]
            [status-im.utils.types :as types]
            [status-im.utils.utils :as utils]
            [taoensso.timbre :as log]))

;;;; COFX


;;;; FX


;;;; Handlers
(defn show-mainnet-is-default-alert [{:keys [db]}]
  (let [enter-name-screen? (= :enter-name (get-in db [:accounts/create :step]))
        shown? (get-in db [:account/account :mainnet-warning-shown?])]
    (when (and config/mainnet-warning-enabled?
               (not shown?)
               (not enter-name-screen?))
      (utils/show-popup
       (i18n/label :mainnet-is-default-alert-title)
       (i18n/label :mainnet-is-default-alert-text)
       #(re-frame/dispatch [:accounts.ui/update-mainnet-warning-shown])))))

(defn- chat-send? [transaction]
  (and (seq transaction)
       (not (:in-progress? transaction))
       (:from-chat? transaction)))

(defn continue-after-wallet-onboarding [db modal? cofx]
  (let [transaction (get-in db [:wallet :send-transaction])]
    (cond modal? {:dispatch [:navigate-to-modal :wallet-send-transaction-modal]}
          (chat-send? transaction) {:db       (navigation/navigate-back db)
                                    :dispatch [:navigate-to :wallet-send-transaction-chat]}
          :else {:db (navigation/navigate-back db)})))

(defn confirm-wallet-set-up [modal? {:keys [db] :as cofx}]
  (handlers-macro/merge-fx
   cofx
   (continue-after-wallet-onboarding db modal?)
   (wallet.settings.models/wallet-autoconfig-tokens)
   (accounts.update/account-update {:wallet-set-up-passed? true})))

(defn switch-dev-mode [dev-mode? cofx]
  (merge (accounts.update/account-update {:dev-mode? dev-mode?} cofx)
         (if dev-mode?
           {:dev-server/start nil}
           {:dev-server/stop nil})))
