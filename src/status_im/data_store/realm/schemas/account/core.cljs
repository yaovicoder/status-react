(ns status-im.data-store.realm.schemas.account.core
  (:require [status-im.data-store.realm.schemas.account.chat :as chat]
            [status-im.data-store.realm.schemas.account.transport :as transport]
            [status-im.data-store.realm.schemas.account.contact :as contact]
            [status-im.data-store.realm.schemas.account.message :as message]
            [status-im.data-store.realm.schemas.account.user-status :as user-status]
            [status-im.data-store.realm.schemas.account.local-storage :as local-storage]
            [status-im.data-store.realm.schemas.account.mailserver :as mailserver]
            [status-im.data-store.realm.schemas.account.browser :as browser]
            [status-im.data-store.realm.schemas.account.dapp-permissions :as dapp-permissions]
            [status-im.data-store.realm.schemas.account.request :as request]
            [status-im.data-store.realm.schemas.account.migrations :as migrations]
            [taoensso.timbre :as log]))

(def v1 [chat/v1
         transport/v1
         contact/v1
         message/v1
         request/v1
         user-status/v1
         local-storage/v1
         browser/v1])

(def v2 [chat/v1
         transport/v1
         contact/v1
         message/v1
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v1])

(def v3 [chat/v3
         transport/v1
         contact/v1
         message/v1
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v1])

(def v4 [chat/v3
         transport/v4
         contact/v1
         message/v1
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v1])

(def v5 [chat/v5
         transport/v4
         contact/v1
         message/v1
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v1])

(def v6 [chat/v5
         transport/v6
         contact/v1
         message/v1
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v1])

(def v7 [chat/v5
         transport/v6
         contact/v1
         message/v7
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v1])

(def v8 [chat/v5
         transport/v6
         contact/v1
         message/v7
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v8])

(def v9 [chat/v5
         transport/v6
         contact/v1
         message/v7
         request/v1
         mailserver/v2
         user-status/v1
         local-storage/v1
         browser/v8
         dapp-permissions/v9])

(def v10 [chat/v5
          transport/v6
          contact/v1
          message/v7
          mailserver/v2
          user-status/v1
          local-storage/v1
          browser/v8
          dapp-permissions/v9])

(def v11 [chat/v5
          transport/v6
          contact/v1
          message/v7
          mailserver/v11
          user-status/v1
          local-storage/v1
          browser/v8
          dapp-permissions/v9])

(def v12 [chat/v6
          transport/v6
          contact/v1
          message/v7
          mailserver/v11
          user-status/v1
          local-storage/v1
          browser/v8
          dapp-permissions/v9])
