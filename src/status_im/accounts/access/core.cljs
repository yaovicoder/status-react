(ns status-im.accounts.access.core
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [status-im.accounts.create.core :as accounts.create]
            [status-im.accounts.db :as db]
            [status-im.i18n :as i18n]
            [status-im.native-module.core :as status]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.ethereum.mnemonic :as mnemonic]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.security :as security]
            [status-im.utils.types :as types]))

(defn check-password-errors [password]
  (cond (string/blank? password) :required-field
        (not (db/valid-length? password)) :recover-password-too-short))

(defn check-phrase-errors [recovery-phrase]
  (cond (string/blank? recovery-phrase) :required-field
        (not (mnemonic/valid-phrase? recovery-phrase)) :recovery-phrase-invalid))

(defn check-phrase-warnings [recovery-phrase]
  (when (not (mnemonic/status-generated-phrase? recovery-phrase))
    :recovery-phrase-unknown-words))

(defn access-account! [masked-passphrase password]
  (status/access-account
   (mnemonic/sanitize-passphrase (security/unmask masked-passphrase))
   password
   (fn [result]
     ;; here we deserialize result, dissoc mnemonic and serialize the result again
     ;; because we want to have information about the result printed in logs, but
     ;; don't want secure data to be printed
     (let [data (-> (types/json->clj result)
                    (dissoc :mnemonic)
                    (types/clj->json))]
       (re-frame/dispatch [:accounts.access.callback/access-account-success data password])))))

(defn set-phrase [masked-recovery-phrase {:keys [db]}]
  (let [recovery-phrase (security/unmask masked-recovery-phrase)]
    {:db (update db :accounts/access assoc
                 :passphrase (string/lower-case recovery-phrase)
                 :passphrase-valid? (not (check-phrase-errors recovery-phrase)))}))

(defn validate-phrase [{:keys [db]}]
  (let [recovery-phrase (get-in db [:accounts/access :passphrase])]
    {:db (update db :accounts/access assoc
                 :passphrase-error (check-phrase-errors recovery-phrase)
                 :passphrase-warning (check-phrase-warnings recovery-phrase))}))

(defn set-password [masked-password {:keys [db]}]
  (let [password (security/unmask masked-password)]
    {:db (update db :accounts/access assoc
                 :password password
                 :password-valid? (not (check-password-errors password)))}))

(defn validate-password [{:keys [db]}]
  (let [password (get-in db [:accounts/access :password])]
    {:db (assoc-in db [:accounts/access :password-error] (check-password-errors password))}))

(defn validate-access-result [{:keys [error pubkey address]} password {:keys [db] :as cofx}]
  (if (empty? error)
    (let [account {:pubkey     pubkey
                   :address    address
                   :photo-path (identicon/identicon pubkey)
                   :mnemonic   ""}]
      (accounts.create/on-account-created account password true cofx))
    {:db (assoc-in db [:accounts/access :password-error] :recover-password-invalid)}))

(defn on-account-accessed [result password {:keys [db] :as cofx}]
  (let [data (types/json->clj result)]
    (handlers-macro/merge-fx cofx
                             {:db (dissoc db :accounts/access)}
                             (validate-access-result data password))))

(defn access-account [{:keys [db]}]
  (let [{:keys [password passphrase]} (:accounts/access db)]
    {:db (assoc-in db [:accounts/access :processing?] true)
     :accounts.access/access-account [(security/mask-data passphrase) password]}))

(defn access-account-with-checks [{:keys [db] :as cofx}]
  (let [{:keys [passphrase processing?]} (:accounts/access db)]
    (when-not processing?
      (if (mnemonic/status-generated-phrase? passphrase)
        (access-account cofx)
        {:ui/show-confirmation
         {:title               (i18n/label :recovery-typo-dialog-title)
          :content             (i18n/label :recovery-typo-dialog-description)
          :confirm-button-text (i18n/label :recovery-confirm-phrase)
          :on-accept           #(re-frame/dispatch [:accounts.access.ui/access-account-confirmed])}}))))

(defn next-step [step {:keys [db] :as cofx}]
  (case step
    :passphrase {:db (assoc-in db [:accounts/access :step] :enter-password)}
    :enter-password {:db (assoc-in db [:accounts/access :step] :confirm-password)}
    :confirm-password (access-account-with-checks cofx)))

(defn step-back [step {:keys [db] :as cofx}]
  (case step
    :passphrase (navigation/navigate-back cofx)
    :enter-password {:db (assoc-in db [:accounts/access :step] :passphrase)}
    :confirm-password {:db (assoc-in db [:accounts/access :step] :enter-password)}))

(defn account-set-input-text [input-key text {db :db}]
  {:db (update db :accounts/access merge {input-key text :error nil})})

(defn navigate-to-access-account-screen [{:keys [db] :as cofx}]
  (handlers-macro/merge-fx cofx
                           {:db (assoc db :accounts/access {:step :passphrase})}
                           (navigation/navigate-to-cofx :access-account nil)))

(re-frame/reg-fx
 :accounts.access/access-account
 (fn [[masked-passphrase password]]
   (access-account! masked-passphrase password)))
