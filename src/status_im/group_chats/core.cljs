(ns status-im.group-chats.core
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as spec]
            [clojure.set :as clojure.set]
            [re-frame.core :as re-frame]
            [status-im.utils.config :as config]
            [status-im.utils.clocks :as utils.clocks]
            [status-im.native-module.core :as native-module]
            [status-im.transport.utils :as transport.utils]
            [status-im.transport.db :as transport.db]
            [status-im.transport.utils :as transport.utils]
            [status-im.transport.message.core :as protocol.message]
            [status-im.transport.message.v1.core :as transport]
            [status-im.transport.message.v1.protocol :as transport.protocol]
            [status-im.utils.fx :as fx]
            [status-im.chat.models :as models.chat]))

(defn- event->vector
  "Trasnform an event in an a vector with keys in alphabetical order"
  [event]
  (mapv
   #(vector % (get event %))
   (sort (keys event))))

(defn- parse-response [response-js]
  (-> response-js
      js/JSON.parse
      (js->clj :keywordize-keys true)))

(defn signature-material [chat-id events]
  (js/JSON.stringify
   (clj->js [(mapv event->vector events) chat-id])))

(defn signature-pairs [{:keys [chat-id events] :as payload}]
  (let [pairs (mapv (fn [{:keys [events signature from]}]
                      [(signature-material chat-id events)
                       signature
                       (subs from 2)])
                    events)]
    (js/JSON.stringify (clj->js pairs))))

(defn valid-chat-id?
  ;; We need to make sure the chat-id ends with the admin pk (and it's not the same).
  ;; this is due to prevent an attack whereby a non-admin user would
  ;; send out a message with identical chat-id and themselves as admin to other members,
  ;; who would then have to trust the first of the two messages received, possibly
  ;; resulting in a situation where some of the members in the chat trust a different admin.
  [chat-id admin]
  (and (string/ends-with? chat-id admin)
       (not= chat-id admin)))

(defn wrap-group-message
  "Wrap a group message in a membership update"
  [cofx chat-id message]
  (when-let [chat (get-in cofx [:db :chats chat-id])]
    (transport/map->GroupMembershipUpdate.
     {:chat-id      chat-id
      :events       (:events chat)
      :message      message})))

(defn send-membership-update
  "Send a membership update to all participants but the sender"
  [cofx payload chat-id]
  (let [members (get-in cofx [:db :chats chat-id :members])
        {:keys [current-public-key web3]} (:db cofx)]
    (fx/merge
     cofx
     {:shh/send-group-message {:web3          web3
                               :src           current-public-key
                               :dsts          (disj members current-public-key)
                               :success-event [:transport/set-message-envelope-hash
                                               chat-id
                                               (transport.utils/message-id (:message payload))
                                               :group-user-message]
                               :payload       payload}})))

(defn send-group-leave [payload chat-id cofx]
  (transport.protocol/send cofx
                           {:chat-id       chat-id
                            :payload       payload
                            :success-event [:group/unsubscribe-from-chat chat-id]}))

(fx/defn handle-membership-update-received
  "Verify signatures in status-go and act if successful"
  [cofx membership-update signature]
  {:group-chats/verify-membership-signature [membership-update signature]})

(defn chat->group-update
  "Transform a chat in a GroupMembershipUpdate"
  [chat-id {:keys [events]}]
  (transport/map->GroupMembershipUpdate. {:chat-id chat-id
                                          :events events}))

(defn handle-sign-response
  "Callback to dispatch on sign response"
  [payload response-js]
  (let [{:keys [error signature]} (parse-response response-js)]
    (if error
      (re-frame/dispatch [:group-chats.callback/sign-failed  error])
      (re-frame/dispatch [:group-chats.callback/sign-success (assoc payload :signature signature)]))))

(defn handle-verify-signature-response
  "Callback to dispatch on verify signature response"
  [payload sender-signature response-js]
  (let [{:keys [error]} (parse-response response-js)]
    (if error
      (re-frame/dispatch [:group-chats.callback/verify-signature-failed  error])
      (re-frame/dispatch [:group-chats.callback/verify-signature-success payload sender-signature]))))

(defn sign-membership [{:keys [chat-id events] :as payload}]
  (native-module/sign-group-membership (signature-material chat-id events)
                                       (partial handle-sign-response payload)))

(defn verify-membership-signature [payload sender]
  (native-module/verify-group-membership-signatures (signature-pairs payload)
                                                    (partial handle-verify-signature-response payload sender)))

(defn- member-added-event [from events member]
  (conj
   events
   {:type "member-added"
    :clock-value (utils.clocks/send (-> events peek :clock-value))
    :member member}))

(fx/defn create
  "Format group update message and sign membership"
  [{:keys [db random-guid-generator] :as cofx} group-name]
  (let [my-public-key     (:current-public-key db)
        chat-id           (str (random-guid-generator) my-public-key)
        selected-contacts (:group/selected-contacts db)
        create-event      {:type        "chat-created"
                           :name        group-name
                           :clock-value (utils.clocks/send 0)}
        events            (reduce (partial member-added-event my-public-key)
                                  [create-event]
                                  selected-contacts)]

    {:group-chats/sign-membership {:chat-id chat-id
                                   :from   my-public-key
                                   :events events}
     :db (assoc db :group/selected-contacts #{})}))

(defn- valid-name? [name]
  (spec/valid? :profile/name name))

(fx/defn update-name [{:keys [db]} name]
  {:db (-> db
           (assoc-in [:group-chat-profile/profile :valid-name?] (valid-name? name))
           (assoc-in [:group-chat-profile/profile :name] name))})

(fx/defn handle-name-changed
  "Store name in profile scratchpad"
  [cofx new-chat-name]
  (update-name cofx new-chat-name))

(fx/defn save
  "Save chat from edited profile"
  [{:keys [db] :as cofx}]
  (let [current-chat-id (get-in cofx [:db :current-chat-id])
        new-name (get-in cofx [:db :group-chat-profile/profile :name])]
    (when (valid-name? new-name)
      (fx/merge cofx
                {:db (assoc db :group-chat-profile/editing? false)}
                (models.chat/upsert-chat {:chat-id current-chat-id
                                          :name new-name})))))

(defn valid-event?
  "Check if event can be applied to current group"
  [{:keys [admins members]} {:keys [chat-id from member] :as new-event}]
  (when from
    (case (:type new-event)
      "chat-created"   (and (empty? admins)
                            (empty? members))
      "name-changed"   (and (admins from)
                            (not (string/blank? (:name new-event))))
      "member-added"    (admins from)
      "admin-added"     (and (admins from)
                             (members member))
      "member-removed" (or
                        ;; An admin removing a member
                        (and (admins from)
                             (not (admins member)))
                        ;; Members can remove themselves
                        (and (not (admins member))
                             (members member)
                             (= from member)))
      "admin-removed" (and (admins from)
                           (= from member)
                           (not= #{from} admins))
      false)))

(defn process-event
  "Add/remove an event to a group"
  [group {:keys [type member chat-id from name] :as event}]
  (if (valid-event? group event)
    (case type
      "chat-created"   {:name       name
                        :admins     #{from}
                        :members    #{from}}
      "name-changed"   (assoc group :name name)
      "member-added"   (update group :members conj member)
      "admin-added"    (update group :admins conj member)
      "member-removed" (update group :members disj member)
      "admin-removed"  (update group :admins disj member))
    group))

(defn build-group
  "Given a list of already authenticated events build a group with contats/admin"
  [events]
  (->> events
       (sort-by :clock-value)
       (reduce
        process-event
        {:admins #{}
         :members #{}})))

(defn membership-changes
  "Output a list of changes so that system messages can be derived"
  [old-group new-group]
  (let [admin-removed  (clojure.set/difference (:admins old-group) (:admins new-group))
        admin-added   (clojure.set/difference (:admins new-group) (:admins old-group))
        member-removed (clojure.set/difference (:members old-group) (:members new-group))
        member-added   (clojure.set/difference (:members new-group) (:members old-group))]
    (concat
     (map #(hash-map :type "admin-removed" :member %) admin-removed)
     (map #(hash-map :type "member-removed" :member %) member-removed)
     (map #(hash-map :type "member-added" :member %)  member-added)
     (map #(hash-map :type "admin-added" :member %) admin-added))))

(re-frame/reg-fx
 :group-chats/sign-membership
 sign-membership)

(re-frame/reg-fx
 :group-chats/verify-membership-signature
 (fn [[payload sender]]
   (verify-membership-signature payload sender)))

(fx/defn update-membership
  "Upsert chat when version is greater or not existing"
  [cofx previous-chat {:keys [chat-id] :as new-chat}]
  (let [all-events         (clojure.set/union (into #{} (:events previous-chat))
                                              (into #{} (:events new-chat)))
        unwrapped-events   (mapcat
                            (fn [{:keys [events from]}]
                              (map #(assoc % :from from) events))
                            all-events)
        new-group          (build-group unwrapped-events)]
    (models.chat/upsert-chat cofx
                             {:chat-id              chat-id
                              :name                 (:name new-group)
                              :is-active            (get previous-chat :is-active true)
                              :group-chat           true
                              :events               (into [] all-events)
                              :admins               (:admins new-group)
                              :members              (:members new-group)})))


(fx/defn handle-membership-update
  "Upsert chat and receive message if valid"
  ;; Care needs to be taken here as chat-id is not coming from a whisper filter
  ;; so can be manipulated by the sending user.
  [cofx {:keys [chat-id
                message
                events] :as membership-update}
   sender-signature]
  (when (and config/group-chats-enabled?
             (valid-chat-id? chat-id (-> events first :from)))
    (let [previous-chat (get-in cofx [:db :chats chat-id])]
      (fx/merge cofx
                (update-membership previous-chat membership-update)
                #(when (and message
                            ;; don't allow anything but group messages
                            (instance? transport.protocol/Message message)
                            (= :group-user-message (:message-type message)))
                   (protocol.message/receive message chat-id sender-signature nil %))))))

(defn handle-sign-success
  "Upsert chat and send signed payload to group members"
  [{:keys [db] :as cofx} {:keys [chat-id] :as signed-events}]
  (let [chat          (get-in db [:chats chat-id])
        updated-chat  (update chat :events conj signed-events)
        my-public-key (:current-public-key db)
        group-update (chat->group-update chat-id updated-chat)]
    (fx/merge cofx
              (handle-membership-update group-update my-public-key)
              (models.chat/navigate-to-chat chat-id {:navigation-reset? true})
              #(protocol.message/send group-update chat-id %))))
