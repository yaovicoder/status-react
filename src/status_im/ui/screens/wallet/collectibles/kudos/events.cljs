(ns status-im.ui.screens.wallet.collectibles.kudos.events
  (:require [status-im.ui.screens.wallet.collectibles.events :as collectibles]
            [status-im.utils.http :as http]
            [status-im.utils.ethereum.erc721 :as erc721]
            [status-im.utils.ethereum.tokens :as tokens]
            [re-frame.core :as re-frame]
            [status-im.utils.handlers :as handlers]))

(def kudos :KUDOS)

(defmethod collectibles/load-collectible-fx kudos [{db :db} symbol id]
  {:erc721-token-uri [(:web3 db) symbol id]})

(re-frame/reg-fx
 :erc721-token-uri
 (fn [[web3 symbol tokenId]]
   (let [contract (:address (tokens/symbol->token :mainnet symbol))]
     (erc721/token-uri web3 contract tokenId #(re-frame/dispatch [:token-uri-success tokenId %])))))

(handlers/register-handler-fx
 :token-uri-success
 (fn [cofx [_ tokenId token-uri]]
   {:http-get {:url                   token-uri
               :success-event-creator (fn [o]
                                        [:load-collectible-success kudos {tokenId (http/parse-payload o)}])
               :failure-event-creator (fn [o]
                                        [:load-collectible-failure kudos {tokenId (http/parse-payload o)}])}}))
