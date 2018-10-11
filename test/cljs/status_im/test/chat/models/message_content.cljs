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

(deftest build-render-recipe-test
  (testing "Render tree is build from text"
    (is (not (message-content/build-render-recipe (message-content/enrich-content {:text "Plain message"}))))
    (is (= '({:text "Test ", :kinds #{:text}}
             {:text "#status", :kinds #{:tag :text}}
             {:text " one three ", :kinds #{:text}}
             {:text "*", :kinds #{:bold :text}}
             {:text "#core-chat", :kinds #{:tag :bold :text}}
             {:text " (", :kinds #{:bold :text}}
             {:text "@developer", :kinds #{:mention :bold :text}}
             {:text ")!*", :kinds #{:bold :text}}
             {:text " By the way, ", :kinds #{:text}}
             {:text "~nice link(", :kinds #{:italic :text}}
             {:text "https://link.com", :kinds #{:link :italic :text}}
             {:text ")~", :kinds #{:italic :text}})
           (message-content/build-render-recipe
            (message-content/enrich-content {:text "Test #status one three *#core-chat (@developer)!* By the way, ~nice link(https://link.com)~"}))))))

