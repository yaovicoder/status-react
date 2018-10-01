(ns status-im.test.data-store.chats
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.utils.random :as utils.random]
            [status-im.data-store.chats :as chats]))

(deftest normalize-chat-test
  (testing "admins & contacts"
    (with-redefs [chats/get-last-clock-value (constantly 42)]
      (is (= {:last-clock-value 42
              :admins #{4}
              :contacts #{2}
              :membership-updates []}
             (chats/normalize-chat {:admins [4]
                                    :contacts [2]})))))
  (testing "membership-updates"
    (with-redefs [chats/get-last-clock-value (constantly 42)]
      (let [raw-events {"1" {:id "1" :type "members-added" :clock-value 10 :members [1 2] :signature "a" :from "id-1"}
                        "2" {:id "2" :type "member-removed" :clock-value 11 :member 1 :signature "a" :from "id-1"}
                        "3" {:id "3" :type "chat-created" :clock-value 0 :name "blah" :signature "b" :from "id-2"}}
            expected    #{{:chat-id "chat-id"
                           :from "id-2"
                           :signature "b"
                           :events [{:type "chat-created" :clock-value 0 :name "blah"}]}
                          {:chat-id "chat-id"
                           :signature "a"
                           :from "id-1"
                           :events [{:type "members-added" :clock-value 10 :members [1 2]}
                                    {:type "member-removed" :clock-value 11 :member 1}]}}
            actual      (->> (chats/normalize-chat {:chat-id "chat-id"
                                                    :membership-updates raw-events})
                             :membership-updates
                             (into #{}))]
        (is (= expected
               actual))))))

(deftest marshal-membership-updates-test
  (with-redefs [utils.random/guid (constantly "id")]
    (let [raw-updates [{:chat-id "chat-id"
                        :signature "b"
                        :from   "id-1"
                        :events [{:type "chat-created" :clock-value 0 :name "blah"}]}
                       {:chat-id "chat-id"
                        :signature "a"
                        :from   "id-2"
                        :events [{:type "members-added" :clock-value 10 :members [1 2]}
                                 {:type "member-removed" :clock-value 11 :member 1}]}]
          expected    #{{:type "members-added" :clock-value 10 :from "id-2" :members [1 2] :signature "a" :id "id"}
                        {:type "member-removed" :clock-value 11 :from "id-2" :member 1 :signature "a" :id "id"}
                        {:type "chat-created" :clock-value 0 :from "id-1" :name "blah" :signature "b" :id "id"}}
          actual      (into #{} (chats/marshal-membership-updates raw-updates))]
      (is (= expected actual)))))
