(ns status-im.group-chats.core
  (:require
   [clojure.string :as string]
   [status-im.utils.config :as config]
   [status-im.transport.utils :as transport.utils]
   [status-im.transport.db :as transport.db]
   [status-im.transport.utils :as transport.utils]
   [status-im.transport.message.core :as protocol.message]
   [status-im.transport.message.v1.core :as transport]
   [status-im.transport.message.v1.protocol :as transport.protocol]
   [status-im.utils.fx :as fx]
   [status-im.chat.models :as models.chat]))

(defn signature-material [{:keys [admin signature participants]}]
  [(apply str
          (conj (sort participants)
                admin))
   signature
   admin])

(defn valid-chat-id?
  ;; We need to make sure the chat-id ends with the admin pk.
  ;; this is due to prevent an attack whereby a non-admin user would
  ;; send out a message with identical chat-id and themselves as admin to other members,
  ;; who would then have to trust the first of the two messages received, possibly
  ;; resulting in a situation where some of the members in the chat trust a different admin.
  [chat-id admin]
  (string/ends-with? chat-id admin))

(defn wrap-group-message [cofx chat-id message]
  (when-let [chat (get-in cofx [:db :chats chat-id])]
    (transport/GroupMembershipUpdate.
     chat-id
     (:name chat)
     (:group-admin chat)
     (:contacts chat)
     nil
     nil
     message)))

(defn update-membership [cofx previous-chat {:keys [chat-id chat-name participants leaves signature version]}]
  (when (< (:membership-version previous-chat)
           version)
    (models.chat/upsert-chat cofx
                             {:chat-id chat-id
                              :membership-version version})))

(defn send-membership-update [cofx payload chat-id]
  (let [{:keys [participants]} payload
        {:keys [current-public-key web3]} (:db cofx)]
    (fx/merge
     cofx
     {:shh/send-group-message {:web3 web3
                               :src     current-public-key

                               :dsts    (disj participants current-public-key)
                               :success-event [:transport/set-message-envelope-hash
                                               chat-id
                                               (transport.utils/message-id (:message payload))
                                               :group-user-message]
                               :payload payload}})))

(defn handle-group-leave [payload chat-id cofx]
  (transport.protocol/send cofx
                           {:chat-id       chat-id
                            :payload       payload
                            :success-event [:group/unsubscribe-from-chat chat-id]}))

(fx/defn handle-membership-update-received [cofx membership-update]
  {:group-chats/check-signature [(signature-material membership-update)]})

(fx/defn handle-membership-update [cofx {:keys [chat-id
                                                chat-name
                                                participants
                                                signature
                                                leaves
                                                message
                                                admin
                                                version] :as membership-update} sender-signature]
  (when (and config/group-chats-enabled?
             (valid-chat-id? chat-id admin))
    (let [chat-fx (if-let [previous-chat (get-in cofx [:db :chats chat-id])]
                    (update-membership cofx previous-chat membership-update)
                    (models.chat/upsert-chat
                     cofx
                     {:chat-id chat-id
                      :name chat-name
                      :is-active true
                      :group-chat true
                      :signature   signature
                      :group-admin admin
                      :contacts participants
                      :membership-version version}))]
      (if message
        (fx/merge cofx
                  chat-fx
                  #(protocol.message/receive message chat-id sender-signature nil %))
        chat-fx))))

(fx/defn send-group-update [cofx group-update chat-id]
  (protocol.message/send group-update chat-id cofx))

(fx/defn send-membership-update [group-update]
  (fx/merge cofx
            {:db (assoc db :group/selected-contacts #{})}
            (models.chat/navigate-to-chat chat-id {})
            (handle-membership-update group-update my-public-key)
            (send-group-update group-update chat-id)))

(fx/defn create [{:keys [db random-guid-generator] :as cofx} group-name]
  (let [my-public-key             (:current-public-key db)
        chat-id           (str (random-guid-generator) my-public-key)
        selected-contacts (conj (:group/selected-contacts db)
                                my-public-key)
        group-update (transport/GroupMembershipUpdate. chat-id group-name my-public-key selected-contacts nil nil nil)]
    (fx/merge cofx
              {:db (assoc db :group/selected-contacts #{})}
              (models.chat/navigate-to-chat chat-id {})
              (handle-membership-update group-update my-public-key)
              (send-group-update group-update chat-id))))
