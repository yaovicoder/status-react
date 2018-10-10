(ns status-im.pairing.core)

(defn start [cofx]
  (let [{:keys [current-public-key web3]} (:db cofx)]
    {:shh/send-pairing-message {:web3          web3
                                :src           current-public-key
                                :payload       []}}))
