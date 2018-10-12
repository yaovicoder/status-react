(ns status-im.data-store.realm.schemas.base.extension)

(def v12 {:name       :extension
          :primaryKey :id
          :properties {:id  :string
                       :url {:type :string}}})
