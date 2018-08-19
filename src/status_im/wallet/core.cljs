(ns status-im.wallet.core)

(defn show-password-input [{:keys [db]}]
  {:db (assoc-in db [:wallet :send-transaction :show-password-input?] true)})
