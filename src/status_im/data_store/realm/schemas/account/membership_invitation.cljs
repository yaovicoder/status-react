(ns status-im.data-store.realm.schemas.account.membership-invitation)

(def v1 {:name       :membership-invitation
         :primaryKey :invitation-id
         :properties {:invitation-id    :string
                      :contact          :string}})
