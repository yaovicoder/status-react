(ns status-im.group-chats.core
  (:require
   [status-im.utils.handlers-macro :as handlers-macro]
   [status-im.transport.message.core :as protocol.message]
   [status-im.transport.message.v1.group-chat :as protocol.group-chat]
   [status-im.chat.models :as models.chat]))

(defn update-membership [previous-chat {:keys [chat-id chat-name participants leaves signature version]} cofx]
  (println "PREVIOUS" (:membership-version previous-chat)
           version
           (< (:membership-version previous-chat)
              version))
  (when (< (:membership-version previous-chat)
           version)
    (models.chat/upsert-chat {:chat-id chat-id
                              :membership-version version}
                             cofx)))
(defn wrap-group-message [chat-id message cofx]
  (when-let [chat (get-in cofx [:db :chats chat-id])]
    (protocol.group-chat/GroupMembershipUpdate.
     chat-id
     (:chat-name chat)
     (:group-admin chat)
     (:contacts chat)
     nil
     nil
     message)))

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
    (handlers-macro/merge-fx cofx
                             chat-fx
                             (protocol.message/receive message chat-id sender-signature nil))))
