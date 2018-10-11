(ns status-im.test.chat.models.message-content
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.chat.models.message-content :as message-content]))

(deftest enrich-string-content-test
  (testing "Text content of the message is enriched correctly"
    (is (not (:metadata (message-content/enrich-content {:text "Plain message"}))))
    (is (= {:bold [[5 14]]}
           (:metadata (message-content/enrich-content {:text "Some *styling* present"}))))
    (is (= {:bold [[5 14]]
            :tag  [[28 33] [38 43]]}
           (:metadata (message-content/enrich-content {:text "Some *styling* present with #tag1 and #tag2 as well"}))))))

(deftest build-render-tree-test
  (testing "Render tree is build from text"
    (is (= [[[5 12] {:type :tag}]
            [[23 49] {:type :bold,
                      :children [[[1 11] {:type :tag}]
                                 [[13 23] {:type :mention}]]}]
            [[62 91] {:type :italic,
                      :children [[[11 27] {:type :link}]]}]]
           (message-content/build-render-tree
            (:metadata (message-content/enrich-content {:text "Test #status one three *#core-chat (@developer)!* By the way, ~nice link(https://link.com)~"})))))))
