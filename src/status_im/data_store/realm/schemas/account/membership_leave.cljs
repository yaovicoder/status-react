(ns status-im.data-store.realm.schemas.account.membership-leave)

(def v1 {:name       :membership-leave
         :primaryKey :signature
         :properties {:contact          :string
                      :invitation-id    :string
                      :signature        :string}})
