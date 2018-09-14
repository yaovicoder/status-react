(ns status-im.test.chat.events
  (:require [cljs.test :refer [deftest is testing]]
            [status-im.chat.events :as chat-events]))

(deftest show-profile-test
  (testing "default behaviour"
    (testing "it navigates to profile but forgets the navigation"
      (let [{:keys [db]} (chat-events/show-profile
                          "a"
                          false
                          {:db {:navigation-stack '(:home)}})]
        (is (= "a" (:contacts/identity db)))
        (is (= '(:home) (:navigation-stack db)))
        (is (= :profile (:view-id db))))))
  (testing "keep-navigation? on"
    (testing "it navigates to profile and keeps the navigation"
      (let [{:keys [db]} (chat-events/show-profile
                          "a"
                          true
                          {:db {:navigation-stack '(:home)}})]
        (is (= "a" (:contacts/identity db)))
        (is (= '(:profile :home) (:navigation-stack db)))
        (is (= :profile (:view-id db)))))))
