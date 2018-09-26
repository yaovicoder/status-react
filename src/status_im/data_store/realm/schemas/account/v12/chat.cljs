(ns status-im.data-store.realm.schemas.account.v12.chat
  (:require [status-im.data-store.realm.schemas.account.v5.chat :as v11]))

(defn update-schema [previous-schema]
  (update previous-schema :properties assoc
          :membership-version   {:type :int
                                 :optional true}
          :membership-signature {:type :string
                                 :optional true}))

(def schema (update-schema v11/schema))

