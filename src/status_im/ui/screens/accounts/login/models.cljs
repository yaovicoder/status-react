(ns status-im.ui.screens.accounts.login.models
  (:require [re-frame.core :as re-frame]
            [status-im.data-store.core :as data-store]
            [status-im.init.core :as init]
            [status-im.native-module.core :as status]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.utils.keychain.core :as keychain]
            [status-im.utils.types :as types]))

;; login flow:
;;
;; - event `:ui/login` is dispatched
;; - node is initialized with user config or default config
;; - `node.started` signal is received, applying `:login` fx
;; - `:callback/login` event is dispatched, account is changed in datastore, web-data is cleared
;; - `:init/initialize-account` event is dispatched

(defn credentials [cofx]
  (select-keys (get-in cofx [:db :accounts/login]) [:address :password]))

;;;; FX
(defn login! [address password]
  (status/login address password #(re-frame/dispatch [:callback/login % address])))

(defn clear-web-data! []
  (status/clear-web-data))

(defn change-account! [address]
  ;; No matter what is the keychain we use, as checks are done on decrypting base
  (.. (keychain/safe-get-encryption-key)
      (then (fn [encryption-key]
              (data-store/change-account address encryption-key)
              (re-frame/dispatch [:init/initialize-account address])))
      (catch (fn [error]
               ;; If all else fails we fallback to showing initial error
               (re-frame/dispatch [:init/initialize-app "" :decryption-failed])))))

;;;; Handlers
(defn login [cofx]
  (let [{:keys [address password]} (credentials cofx)]
    {:login [address password]}))

(defn open-login [address photo-path name {db :db :as cofx}]
  (handlers-macro/merge-fx cofx
                           {:db       (-> db
                                          (update :accounts/login assoc
                                                  :address address
                                                  :photo-path photo-path
                                                  :name name)
                                          (update :accounts/login dissoc
                                                  :error
                                                  :password))}
                           (navigation/navigate-to-cofx :login nil)))

(defn user-login [address password {:keys [db] :as cofx}]
  (handlers-macro/merge-fx cofx
                           {:db (update db :accounts/login assoc
                                        :processing true
                                        :address    address
                                        :password   password)}
                           (init/initialize-node address)))

(defn login-callback [login-result address {db :db}]
  (let [data    (types/json->clj login-result)
        error   (:error data)
        success (zero? (count error))
        db'     (assoc-in db [:accounts/login :processing] false)]
    (if success
      {:db                        db'
       :clear-web-data            nil
       :data-store/change-account address}
      {:db (assoc-in db' [:accounts/login :error] error)})))
