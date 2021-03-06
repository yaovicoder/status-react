(ns status-im.data-store.accounts
  (:require [re-frame.core :as re-frame]
            [status-im.data-store.realm.core :as core]
            [status-im.utils.types :as types]))

;; TODO janherich: define as cofx once debug handlers are refactored
(defn get-by-address [address]
  (-> @core/base-realm
      (core/get-by-field :account :address address)
      (core/single-clj :account)
      (update :settings core/deserialize)))

(defn- deserialize-bootnodes [bootnodes]
  (reduce-kv
   (fn [acc id {:keys [chain] :as bootnode}]
     (assoc-in acc [chain id] bootnode))
   {}
   bootnodes))

(defn- deserialize-networks [networks]
  (reduce-kv
   (fn [acc network-id props]
     (assoc acc network-id (update props :config types/json->clj)))
   {}
   networks))

(defn- deserialize-extensions [extensions]
  (reduce-kv
   (fn [acc _ {:keys [url] :as extension}]
     (assoc acc url extension))
   {}
   extensions))

(defn- deserialize-account [account]
  (-> account
      (update :settings core/deserialize)
      (update :extensions deserialize-extensions)
      (update :bootnodes deserialize-bootnodes)
      (update :networks deserialize-networks)))

(defn- serialize-bootnodes [bootnodes]
  (->> bootnodes
       vals
       (mapcat vals)))

(defn- serialize-networks [networks]
  (map (fn [[_ props]]
         (update props :config types/clj->json))
       networks))

(defn- serialize-extensions [extensions]
  (or (vals extensions) '()))

(defn- serialize-account [account]
  (-> account
      (update :settings core/serialize)
      (update :extensions serialize-extensions)
      (update :bootnodes serialize-bootnodes)
      (update :networks serialize-networks)))

(defn save-account-tx
  "Returns tx function for saving account"
  [account]
  (fn [realm]
    (core/create realm :account (serialize-account account) true)))

(re-frame/reg-cofx
 :data-store/get-all-accounts
 (fn [coeffects _]
   (assoc coeffects :all-accounts (-> @core/base-realm
                                      (core/get-all :account)
                                      (core/all-clj :account)
                                      (as-> accounts
                                            (map deserialize-account accounts))))))
