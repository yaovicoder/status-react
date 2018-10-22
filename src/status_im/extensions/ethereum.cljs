(ns status-im.extensions.ethereum
  (:require [status-im.extensions.core :as extensions]
            [status-im.models.wallet :as models.wallet]
            [status-im.ui.screens.navigation :as navigation]
            [status-im.utils.ethereum.abi-spec :as abi-spec]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.fx :as fx]))

(handlers/register-handler-fx
 :extensions/transaction-on-result
 (fn [cofx [_ on-result _ result _]]
   (fx/merge cofx
             (when on-result
               {:dispatch (on-result {:result result})})
             (navigation/navigate-to-clean :wallet-transaction-sent nil))))

(handlers/register-handler-fx
 :extensions/transaction-on-error
 (fn [_ [_ on-result message]]
   (when (ifn? on-result)
     (let [res (on-result {:error message})]
       {:dispatch res}))))

(handlers/register-handler-fx
 :extensions/ethereum-send-transaction
 (fn [{db :db} [_ {:keys [method params on-result] :as arguments}]]
   (let [tx-object (assoc (select-keys arguments [:to :gas :gas-price :value :nonce])
                          :data (when (and method params) (abi-spec/encode method params)))
         transaction (models.wallet/prepare-extension-transaction tx-object (:contacts/contacts db) on-result)]
     (models.wallet/open-modal-wallet-for-transaction db transaction tx-object))))

(handlers/register-handler-fx
 :extensions/ethereum-call
 (fn [_ [_ {:keys [to method params on-result]}]]
   (let [tx-object {:to to :data (when method (abi-spec/encode method params))}]
     {:browser/call-rpc [{"jsonrpc" "2.0"
                          "method"  "eth_call"
                          "params"  [tx-object "latest"]}
                         #(extensions/dispatch (on-result {:error %1 :result (when %2
                                                                               (get (js->clj %2) "result"))}))]})))
