(ns status-im.group-chats.core
  (:require
   [status-im.utils.handlers-macro :as handlers-macro]
   [status-im.transport.utils :as transport.utils]
   [status-im.transport.db :as transport.db]
   [status-im.transport.message.core :as protocol.message]
   [status-im.chat.models :as models.chat]))

(defn init-chat
  "Initialises chat on protocol layer.
  If topic is not passed as argument it is derived from `chat-id`"
  [{:keys [chat-id topic resend?]
    :or   {topic   (transport.utils/get-topic chat-id)}}
   {:keys [db]}]
  {:db (assoc-in db
                 [:transport/chats chat-id]
                 (transport.db/create-chat {:topic   topic
                                            :resend? resend?}))})

(defn- init-chat-if-new [chat-id cofx]
  (if (nil? (get-in cofx [:db :transport/chats chat-id]))
    (init-chat {:chat-id chat-id} cofx)))

(defn update-membership [previous-chat {:keys [chat-id chat-name participants leaves signature version]} cofx]
  (when (< (:membership-version previous-chat)
           version)
    (models.chat/upsert-chat {:chat-id chat-id
                              :membership-version version}
                             cofx)))

(defn send-membership-update [payload chat-id cofx]
  (let [{:keys [participants]} payload
        {:keys [current-public-key web3]} (:db cofx)]
    (handlers-macro/merge-fx
     cofx
     {:shh/send-group-message {:web3 web3
                               :src     current-public-key
                               :dsts    (disj participants current-public-key)
                               :payload payload}}
     (init-chat-if-new chat-id))))

(defn handle-membership-update [{:keys [chat-id chat-name participants leaves message signature version] :as membership-update} sender-signature cofx]
  (let [chat-fx (if-let [previous-chat (get-in cofx [:db :chats chat-id])]
                  (update-membership previous-chat membership-update cofx)
                  (models.chat/upsert-chat
                   {:chat-id chat-id
                    :name chat-name
                    :is-active true
                    :group-chat true
                    :group-admin signature
                    :contacts participants
                    :membership-version version}
                   cofx))]
    (if message
      (handlers-macro/merge-fx cofx
                               chat-fx
                               (protocol.message/receive message chat-id sender-signature nil))
      chat-fx)))
