(ns status-im.test.group-chats.core
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.utils.clocks :as utils.clocks]
            [status-im.utils.config :as config]
            [status-im.group-chats.core :as group-chats]))

(def random-id "685a9351-417e-587c-8bc1-191ac2a57ef8")
(def chat-name "chat-name")

(def member-1 "member-1")
(def member-2 "member-2")

(def admin member-1)

(def chat-id (str random-id admin))

(def invitation-m1 {:id "m-1"
                    :user member-1})
(def invitation-m2 {:id "m-2"
                    :user member-2})

(def initial-message {:chat-id chat-id
                      :chat-name chat-name
                      :admin admin
                      :participants [invitation-m1
                                     invitation-m2]
                      :leaves []
                      :signature "some"
                      :version 1})

(deftest handle-group-membership-update
  (with-redefs [config/group-chats-enabled? true]
    (testing "a brand new chat"
      (let [actual   (->
                      (group-chats/handle-membership-update {:db {}} initial-message admin)
                      :db
                      :chats
                      (get chat-id))]
        (testing "it creates a new chat"
          (is actual))
        (testing "it sets the right participants"
          (is (= [invitation-m1
                  invitation-m2]
                 (:contacts actual))))
        (testing "it sets the right version"
          (is (= 1
                 (:membership-version actual))))))
    (testing "a chat with the wrong id"
      (let [bad-chat-id (str random-id member-2)
            actual      (->
                         (group-chats/handle-membership-update
                          {:db {}}
                          (assoc initial-message :chat-id bad-chat-id)
                          admin)
                         :db
                         :chats
                         (get bad-chat-id))]
        (testing "it does not create a chat"
          (is (not actual)))))
    (testing "an already existing chat"
      (let [cofx {:db {:chats {chat-id {:contacts [invitation-m1
                                                   invitation-m2]
                                        :group-admin admin
                                        :membership-version 2}}}}]
        (testing "an update from the admin is received"
          (testing "the message is an older version"
            (let [actual (group-chats/handle-membership-update cofx initial-message admin)]
              (testing "it noops"
                (is (= actual cofx)))))
          (testing "the message is a more recent version"
            (testing "it sets the right participants")))
        (testing "a leave from a member is received"
          (testing "the user is removed"))))))

(deftest build-group-test
  (testing "only adds"
    (let [events [{:type    "chat-created"
                   :clock-value 0
                   :chat-id "something-1"
                   :from    "1"}
                  {:type    "member-added"
                   :clock-value 2
                   :from    "1"
                   :member  "2"}
                  {:type    "admin-added"
                   :clock-value 3
                   :from    "1"
                   :member  "2"}
                  {:type    "member-added"
                   :clock-value 3
                   :from    "2"
                   :member  "3"}]
          expected {:created-by "1"
                    :chat-id "something-1"
                    :admins #{"1" "2"}
                    :members #{"1" "2" "3"}}]
      (is (= expected (group-chats/build-group events)))))
  (testing "adds and removes"
    (let [events [{:type    "chat-created"
                   :clock-value 0
                   :chat-id "something-1"
                   :from    "1"}
                  {:type    "member-added"
                   :clock-value 1
                   :from    "1"
                   :member  "2"}
                  {:type    "admin-added"
                   :clock-value 2
                   :from    "1"
                   :member  "2"}
                  {:type    "admin-removed"
                   :clock-value 3
                   :from    "2"
                   :member  "2"}
                  {:type    "member-removed"
                   :clock-value 4
                   :from   "2"
                   :member "2"}]
          expected {:created-by "1"
                    :chat-id "something-1"
                    :admins #{"1"}
                    :members #{"1"}}]
      (is (= expected (group-chats/build-group events)))))
  (testing "invalid events"
    (let [events [{:type    "chat-created"
                   :clock-value 0
                   :chat-id "something-1"
                   :from    "1"}
                  {:type    "admin-added" ; can't make an admin a user not in the group
                   :clock-value 1
                   :from    "1"
                   :member  "non-existing"}
                  {:type    "member-added"
                   :clock-value 2
                   :from    "1"
                   :member  "2"}
                  {:type    "admin-added"
                   :clock-value 3
                   :from    "1"
                   :member  "2"}
                  {:type    "member-added"
                   :clock-value 4
                   :from    "2"
                   :member  "3"}
                  {:type    "admin-removed" ; can't remove an admin from admins unless it's the same user
                   :clock-value 5
                   :from    "1"
                   :member  "2"}
                  {:type    "member-removed" ; can't remove an admin from the group
                   :clock-value 6
                   :from    "1"
                   :member  "2"}]
          expected {:created-by "1"
                    :chat-id "something-1":admins #{"1" "2"}
                    :members #{"1" "2" "3"}}]
      (is (= expected (group-chats/build-group events)))))
  (testing "out of order-events"
    (let [events [{:type    "chat-created"
                   :clock-value 0
                   :chat-id "something-1"
                   :from    "1"}
                  {:type    "admin-added"
                   :clock-value 2
                   :from    "1"
                   :member  "2"}
                  {:type    "member-added"
                   :clock-value 1
                   :from    "1"
                   :member  "2"}
                  {:type    "member-added"
                   :clock-value 3
                   :from    "2"
                   :member  "3"}]
          expected {:created-by "1"
                    :chat-id "something-1"
                    :admins #{"1" "2"}
                    :members #{"1" "2" "3"}}]
      (is (= expected (group-chats/build-group events))))))

(deftest valid-event-test
  (let [multi-admin-group {:admins #{"1" "2"}
                           :members #{"1" "2" "3"}}
        single-admin-group {:admins #{"1"}
                           :members #{"1" "2" "3"}}]
  (testing "member-addeds"
    (testing "admins can add members"
      (is (group-chats/valid-event? multi-admin-group
                                      {:type "member-added" :clock-value 6 :from "1" :member "4"})))
    (testing "non-admin members cannot add members"
      (is (not (group-chats/valid-event? multi-admin-group
                                           {:type "member-added" :clock-value 6 :from "3" :member "4"})))))
  (testing "admin-addeds"
    (testing "admins can make other member admins"
      (is (group-chats/valid-event? multi-admin-group
                                      {:type "admin-added" :clock-value 6 :from "1" :member "3"})))
    (testing "non-admins can't make other member admins"
      (is (not (group-chats/valid-event? multi-admin-group
                                           {:type "admin-added" :clock-value 6 :from "3" :member "3"}))))
    (testing "non-existing users can't be made admin"
      (is (not (group-chats/valid-event? multi-admin-group
                                           {:type "admin-added" :clock-value 6 :from "1" :member "not-existing"})))))
  (testing "member-removed"
    (testing "admins can remove non-admin members"
      (is (group-chats/valid-event? multi-admin-group
                                      {:type "member-removed" :clock-value 6 :from "1" :member "3"})))
    (testing "admins can't remove themselves"
      (is (not (group-chats/valid-event? multi-admin-group
                                      {:type "member-removed" :clock-value 6 :from "1" :member "1"}))))
    (testing "participants non-admin can remove themselves"
      (is (group-chats/valid-event? multi-admin-group
                                      {:type "member-removed" :clock-value 6 :from "3" :member "3"})))
    (testing "non-admin can't remove other members"
      (is (not (group-chats/valid-event? multi-admin-group
                                      {:type "member-removed" :clock-value 6 :from "3" :member "1"})))))
  (testing "admin-removed"
    (testing "admins can remove themselves"
      (is (group-chats/valid-event? multi-admin-group
                                      {:type "admin-removed" :clock-value 6 :from "1" :member "1"})))
    (testing "admins can't remove other admins"
      (is (not (group-chats/valid-event? multi-admin-group
                                      {:type "admin-removed" :clock-value 6 :from "1" :member "2"}))))
    (testing "participants non-admin can't remove other admins"
      (is (not (group-chats/valid-event? multi-admin-group
                                      {:type "admin-removed" :clock-value 6 :from "3" :member "1"}))))
    (testing "the last admin can't be removed"
      (is (not (group-chats/valid-event? single-admin-group
                                      {:type "admin-removed" :clock-value 6 :from "1" :member "1"})))))))

(deftest create-test
  (testing "create a new chat"
    (with-redefs [utils.clocks/send inc]
      (let [cofx {:random-guid-generator (constantly "random")
                  :db {:current-public-key "me"
                       :group/selected-contacts #{"1" "2"}}}]
        (is (= {:chat-id "randomme"
                :events [{:type "chat-created"
                          :from "me"
                          :clock-value 1
                          :chat-name "group-name"}
                         {:type "member-added"
                          :from "me"
                          :clock-value 2
                          :member "1"}
                         {:type "member-added"
                          :from "me"
                          :clock-value 3
                          :member "2"}]}
               (:group-chats/sign-membership (group-chats/create cofx "group-name"))))))))

(deftest membership-changes-test
  (testing "addition and removals"
    (let [old-group {:admins #{"1" "2"}
                     :members #{"1" "2"}}
          new-group {:admins #{"1" "3"}
                     :members #{"1" "3"}}]
      (is (= [{:type "admin-removed" :member "2"}
              {:type "member-removed" :member "2"}
              {:type "member-added" :member "3"}
              {:type "admin-added" :member "3"}]
             (group-chats/membership-changes old-group new-group))))))
