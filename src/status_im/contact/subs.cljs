(ns status-im.contact.subs
  (:require [re-frame.core :as re-frame :refer [reg-sub subscribe]]
            [status-im.utils.contacts :as utils.contacts]
            [status-im.utils.identicon :as identicon]
            [status-im.contact.db :as contact.db]))

(reg-sub :contact/public-key :contacts/identity)

(reg-sub :get-contacts :contacts/contacts)

(reg-sub :get-dapps
         (fn [db]
           (:contacts/dapps db)))

(re-frame/reg-sub
 :contacts/blocked
 :<- [:get-contacts]
 (fn [contacts]
   (contact.db/blocked-contacts contacts)))

(reg-sub :get-current-chat-contact
         :<- [:get-contacts]
         :<- [:get-current-chat-id]
         (fn [[contacts chat-id]]
           (get contacts chat-id)))

(defn sort-contacts [contacts]
  (sort (fn [c1 c2]
          (let [name1 (or (:name c1) (:address c1) (:public-key c1))
                name2 (or (:name c2) (:address c2) (:public-key c2))]
            (compare (clojure.string/lower-case name1)
                     (clojure.string/lower-case name2))))
        (vals contacts)))

(reg-sub :all-added-contacts
         :<- [:get-contacts]
         (fn [contacts]
           (->> contacts
                (remove (fn [[_ {:keys [pending? hide-contact?]}]]
                          (or pending? hide-contact?)))
                (sort-contacts))))

(reg-sub :all-added-people-contacts
         :<- [:all-added-contacts]
         (fn [contacts]
           (remove :dapp? contacts)))

(re-frame/reg-sub
 :contacts/added
 :<- [:get-contacts]
 :<- [:contacts/blocked]
 (fn [[contacts blocked-contacts]]
   (->> contacts
        (remove (fn [[_ {:keys [dapp? pending? hide-contact? public-key]}]]
                  (or dapp? pending? hide-contact?
                      (blocked-contacts public-key))))
        (sort-contacts))))

(defn- filter-dapps [v dev-mode?]
  (remove #(when-not dev-mode? (true? (:developer? %))) v))

(reg-sub :all-dapps
         :<- [:get-dapps]
         :<- [:account/account]
         (fn [[dapps {:keys [dev-mode?]}]]
           (map (fn [m] (update m :data #(filter-dapps % dev-mode?))) dapps)))

(reg-sub :get-people-in-current-chat
         :<- [:get-current-chat-contacts]
         (fn [contacts]
           (remove #(true? (:dapp? %)) contacts)))

(defn filter-group-contacts [group-contacts contacts]
  (let [group-contacts' (into #{} group-contacts)]
    (filter #(group-contacts' (:public-key %)) contacts)))

(re-frame/reg-sub
 :contact/current
 :<- [:get-contacts]
 :<- [:contact/public-key]
 (fn [[contacts public-key]]
   (when public-key
     (contact.db/enrich-contact contacts public-key))))

(reg-sub :contacts/dapps-by-name
         :<- [:all-dapps]
         (fn [dapps]
           (reduce (fn [dapps-by-name category]
                     (merge dapps-by-name
                            (reduce (fn [acc {:keys [name] :as dapp}]
                                      (assoc acc name dapp))
                                    {}
                                    (:data category))))
                   {}
                   dapps)))

(defn query-chat-contacts [[{:keys [contacts]} all-contacts] [_ query-fn]]
  (let [participant-set (into #{} (filter identity) contacts)]
    (query-fn (comp participant-set :public-key) (vals all-contacts))))

(reg-sub :query-current-chat-contacts
         :<- [:get-current-chat]
         :<- [:get-contacts]
         query-chat-contacts)

(reg-sub :get-all-contacts-not-in-current-chat
         :<- [:query-current-chat-contacts remove]
         (fn [contacts]
           (->> contacts
                (remove :dapp?)
                (sort-by (comp clojure.string/lower-case :name)))))

(defn get-all-contacts-in-group-chat
  [members contacts current-account]
  (let [current-account-contact (-> current-account
                                    (select-keys [:name :photo-path :public-key]))
        all-contacts            (assoc contacts (:public-key current-account-contact) current-account-contact)]
    (->> members
         (map #(or (get all-contacts %)
                   (utils.contacts/public-key->new-contact %)))
         (remove :dapp?)
         (sort-by (comp clojure.string/lower-case :name)))))

(reg-sub :get-current-chat-contacts
         :<- [:get-current-chat]
         :<- [:get-contacts]
         :<- [:account/account]
         (fn [[{:keys [contacts]} all-contacts current-account]]
           (get-all-contacts-in-group-chat contacts all-contacts current-account)))

(reg-sub :get-contacts-by-chat
         (fn [[_ _ chat-id] _]
           [(subscribe [:get-chat chat-id])
            (subscribe [:get-contacts])])
         query-chat-contacts)

(reg-sub :get-chat-photo
         (fn [[_ chat-id] _]
           [(subscribe [:get-chat chat-id])
            (subscribe [:get-contacts-by-chat filter chat-id])])
         (fn [[chat contacts] [_ chat-id]]
           (when (and chat (not (:group-chat chat)))
             (cond
               (:photo-path chat)
               (:photo-path chat)

               (pos? (count contacts))
               (:photo-path (first contacts))

               :else
               (identicon/identicon chat-id)))))

(reg-sub :get-contact-by-address
         :<- [:get-contacts]
         (fn [contacts [_ address]]
           (utils.contacts/find-contact-by-address contacts address)))

(reg-sub :get-contacts-by-address
         :<- [:get-contacts]
         (fn [contacts]
           (reduce (fn [acc [_ {:keys [address] :as contact}]]
                     (if address
                       (assoc acc address contact)
                       acc))
                   {}
                   contacts)))
