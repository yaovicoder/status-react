(ns status-im.group-chats.core
  (:refer-clojure :exclude [remove])
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as spec]
            [clojure.set :as clojure.set]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
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
  "Transform an event in an a vector with keys in alphabetical order"
  [event]
  (mapv
   #(vector % (get event %))
   (sort (keys event))))

(defn get-last-clock-value
  "Given a chat id get the last clock value of an event"
  [cofx chat-id]
  (->> (get-in cofx [:db :chats chat-id :membership-updates])
       (mapcat :events)
       (map :clock-value)
       sort
       last))

(defn- parse-response [response-js]
  (-> response-js
      js/JSON.parse
      (js->clj :keywordize-keys true)))

(defn- prepare-system-message [added-participants removed-participants contacts]
  (let [added-participants-names   (map #(get-in contacts [% :name] %) added-participants)
        removed-participants-names (map #(get-in contacts [% :name] %) removed-participants)]
    (cond
      (and (seq added-participants) (seq removed-participants))
      (str (i18n/label :t/invited) " " (apply str (interpose ", " added-participants-names))
           " and "
           (i18n/label :t/removed) " " (apply str (interpose ", " removed-participants-names)))

      (seq added-participants)
      (str (i18n/label :t/invited) " " (apply str (interpose ", " added-participants-names)))

      (seq removed-participants)
      (str (i18n/label :t/removed) " " (apply str (interpose ", " removed-participants-names))))))

(defn membership-changes
  "Output a list of changes so that system messages can be derived"
  [old-group new-group]
  (let [admin-removed  (clojure.set/difference (:admins old-group) (:admins new-group))
        admin-added   (clojure.set/difference (:admins new-group) (:admins old-group))
        member-removed (clojure.set/difference (:contacts old-group) (:contacts new-group))
        members-added   (clojure.set/difference (:contacts new-group) (:contacts old-group))]
    (concat
     (map #(hash-map :type "admin-removed" :member %) admin-removed)
     (map #(hash-map :type "member-removed" :member %) member-removed)
     [{:type "members-added" :members members-added}]
     (map #(hash-map :type "admin-added" :member %) admin-added))))

(defn signature-material
  "Transform an update into a signable string"
  [chat-id events]
  (js/JSON.stringify
   (clj->js [(mapv event->vector events) chat-id])))

(defn signature-pairs
  "Transform a bunch of updates into signable pairs to be verified"
  [{:keys [chat-id membership-updates] :as payload}]
  (let [pairs (mapv (fn [{:keys [events signature]}]
                      [(signature-material chat-id events)
                       signature])
                    membership-updates)]
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

(defn valid-event?
  "Check if event can be applied to current group"
  [{:keys [admins contacts]} {:keys [chat-id from member] :as new-event}]
  (when from
    (case (:type new-event)
      "chat-created"   (and (empty? admins)
                            (empty? contacts))
      "name-changed"   (and (admins from)
                            (not (string/blank? (:name new-event))))
      "members-added"   (admins from)
      "admin-added"     (and (admins from)
                             (contacts member))
      "member-removed" (or
                        ;; An admin removing a member
                        (and (admins from)
                             (not (admins member)))
                        ;; Members can remove themselves
                        (and (not (admins member))
                             (contacts member)
                             (= from member)))
      "admin-removed" (and (admins from)
                           (= from member)
                           (not= #{from} admins))
      false)))

(defn wrap-group-message
  "Wrap a group message in a membership update"
  [cofx chat-id message]
  (when-let [chat (get-in cofx [:db :chats chat-id])]
    (transport/map->GroupMembershipUpdate.
     {:chat-id              chat-id
      :membership-updates   (:membership-updates chat)
      :message              message})))

(defn send-membership-update
  "Send a membership update to all participants but the sender"
  [cofx payload chat-id]
  (let [members (get-in cofx [:db :chats chat-id :contacts])
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

(fx/defn handle-membership-update-received
  "Extract signatures in status-go and act if successful"
  [cofx membership-update signature]
  {:group-chats/extract-membership-signature [membership-update signature]})

(defn chat->group-update
  "Transform a chat in a GroupMembershipUpdate"
  [chat-id {:keys [membership-updates]}]
  (transport/map->GroupMembershipUpdate. {:chat-id            chat-id
                                          :membership-updates membership-updates}))

(defn handle-sign-response
  "Callback to dispatch on sign response"
  [payload response-js]
  (let [{:keys [error signature]} (parse-response response-js)]
    (if error
      (re-frame/dispatch [:group-chats.callback/sign-failed  error])
      (re-frame/dispatch [:group-chats.callback/sign-success (assoc payload :signature signature)]))))

(defn add-identities
  "Add verified identities extracted from the signature to the updates"
  [payload identities]
  (update payload :membership-updates (fn [updates]
                                        (map
                                         #(assoc %1 :from (str "0x" %2))
                                         updates
                                         identities))))

(defn handle-extract-signature-response
  "Callback to dispatch on extract signature response"
  [payload sender-signature response-js]
  (let [{:keys [error identities]} (parse-response response-js)]
    (if error
      (re-frame/dispatch [:group-chats.callback/extract-signature-failed  error])
      (re-frame/dispatch [:group-chats.callback/extract-signature-success (add-identities payload identities) sender-signature]))))

(defn sign-membership [{:keys [chat-id events] :as payload}]
  (native-module/sign-group-membership (signature-material chat-id events)
                                       (partial handle-sign-response payload)))

(defn extract-membership-signature [payload sender]
  (native-module/extract-group-membership-signatures (signature-pairs payload)
                                                     (partial handle-extract-signature-response payload sender)))

(defn- members-added-event [last-clock-value members]
  {:type "members-added"
   :clock-value (utils.clocks/send last-clock-value)
   :members members})

(fx/defn create
  "Format group update message and sign membership"
  [{:keys [db random-guid-generator] :as cofx} group-name]
  (let [my-public-key     (:current-public-key db)
        chat-id           (str (random-guid-generator) my-public-key)
        selected-contacts (:group/selected-contacts db)
        clock-value       (utils.clocks/send 0)
        create-event      {:type        "chat-created"
                           :name        group-name
                           :clock-value clock-value}
        events            [create-event
                           (members-added-event clock-value selected-contacts)]]

    {:group-chats/sign-membership {:chat-id chat-id
                                   :from   my-public-key
                                   :events events}
     :db (assoc db :group/selected-contacts #{})}))

(fx/defn remove-member
  "Format group update message and sign membership"
  [{:keys [db] :as cofx} chat-id public-key]
  (let [my-public-key     (:current-public-key db)
        last-clock-value  (get-last-clock-value cofx chat-id)
        chat              (get-in cofx [:db :chats chat-id])
        remove-event       {:type        "member-removed"
                            :member      public-key
                            :clock-value (utils.clocks/send last-clock-value)}]
    (when (valid-event? chat (assoc remove-event
                                    :from
                                    my-public-key))
      {:group-chats/sign-membership {:chat-id chat-id
                                     :from    my-public-key
                                     :events  [remove-event]}})))
(fx/defn add-members
  "Add members to a group chat"
  [{{:keys [current-chat-id selected-participants current-public-key]} :db :as cofx}]
  (let [last-clock-value  (get-last-clock-value cofx current-chat-id)
        events            (members-added-event last-clock-value selected-participants)]
    {:group-chats/sign-membership {:chat-id current-chat-id
                                   :from    current-public-key
                                   :events  events}}))
(fx/defn remove
  "Remove & leave chat"
  [{:keys [db] :as cofx} chat-id]
  (let [my-public-key     (:current-public-key db)]
    (fx/merge cofx
              (remove-member chat-id my-public-key)
              (models.chat/remove-chat chat-id))))

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

(defn process-event
  "Add/remove an event to a group"
  [group {:keys [type member members chat-id from name] :as event}]
  (if (valid-event? group event)
    (case type
      "chat-created"   {:name     name
                        :admins   #{from}
                        :contacts #{from}}
      "name-changed"   (assoc group :name name)
      "members-added"  (update group :contacts clojure.set/union (into #{} members))
      "admin-added"    (update group :admins conj member)
      "member-removed" (update group :contacts disj member)
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
         :contacts #{}})))

(fx/defn update-membership
  "Upsert chat when version is greater or not existing"
  [cofx previous-chat {:keys [chat-id] :as new-chat}]
  (let [all-updates        (clojure.set/union (into #{} (:membership-updates previous-chat))
                                              (into #{} (:membership-updates new-chat)))
        unwrapped-events   (mapcat
                            (fn [{:keys [events from]}]
                              (map #(assoc % :from from) events))
                            all-updates)
        new-group          (build-group unwrapped-events)]
    (models.chat/upsert-chat cofx
                             {:chat-id              chat-id
                              :name                 (:name new-group)
                              :is-active            (get previous-chat :is-active true)
                              :group-chat           true
                              :membership-updates   (into [] all-updates)
                              :admins               (:admins new-group)
                              :contacts              (:contacts new-group)})))

(fx/defn handle-membership-update
  "Upsert chat and receive message if valid"
  ;; Care needs to be taken here as chat-id is not coming from a whisper filter
  ;; so can be manipulated by the sending user.
  [cofx {:keys [chat-id
                message
                membership-updates] :as membership-update}
   sender-signature]
  (when (and config/group-chats-enabled?
             (valid-chat-id? chat-id (-> membership-updates first :from)))
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
        updated-chat  (update chat :membership-updates conj signed-events)
        my-public-key (:current-public-key db)
        group-update (chat->group-update chat-id updated-chat)]
    (fx/merge cofx
              (handle-membership-update group-update my-public-key)
              (models.chat/navigate-to-chat chat-id {:navigation-reset? true})
              #(protocol.message/send group-update chat-id %))))

(re-frame/reg-fx
 :group-chats/sign-membership
 sign-membership)

(re-frame/reg-fx
 :group-chats/extract-membership-signature
 (fn [[payload sender]]
   (extract-membership-signature payload sender)))
