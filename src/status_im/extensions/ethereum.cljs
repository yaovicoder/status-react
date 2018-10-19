(ns status-im.extensions.ethereum
  (:require [status-im.utils.handlers :as handlers]
            [re-frame.core :as re-frame]
            [status-im.models.wallet :as models.wallet]
            [status-im.utils.ethereum.abi-spec :as abi-spec]))

(handlers/register-handler-fx
 :extensions/transaction-on-result
 (fn [{db :db} [_ on-result id result method]]
   {:dispatch-n (concat
                 [[:navigate-to-clean :wallet-transaction-sent]]
                 (when on-result
                   [(on-result {:error nil :result result})]))}))

(handlers/register-handler-fx
 :extensions/transaction-on-error
 (fn [{db :db} [_ on-result message]]
   (when on-result {:dispatch (on-result {:error message :result nil})})))

(handlers/register-handler-fx
 :extensions/ethereum-send-transaction
 (fn [{db :db} [_ {:keys [method params on-result] :as arguments}]]
   (let [tx-object (assoc (select-keys arguments [:to :gas :gas-price :value :nonce])
                          :data (when (and method params) (abi-spec/encode method params)))
         transaction (models.wallet/prepare-extension-transaction tx-object (:contacts/contacts db) on-result)]
     (models.wallet/open-modal-wallet-for-trasnaction db transaction tx-object))))

(handlers/register-handler-fx
 :extensions/ethereum-call
 (fn [_ [_ {:keys [to method params on-result]}]]
   (let [tx-object {:to to :data (when method (abi-spec/encode method params))}]
     {:browser/call-rpc [{"jsonrpc" "2.0"
                          "method"  "eth_call"
                          "params"  [tx-object "latest"]}
                         #(when on-result
                            (re-frame/dispatch (on-result {:error %1 :result (when %2
                                                                               (get (js->clj %2) "result"))})))]})))