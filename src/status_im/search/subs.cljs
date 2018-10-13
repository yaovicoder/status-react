(ns status-im.search.subs
  (:require [re-frame.core :as re-frame]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.fx :as fx]
            [cljs.spec.alpha :as spec]
            [clojure.string :as string]))

(re-frame/reg-sub
 :search/get-tag-filter
 (fn [db]
   (get-in db [:ui/search :tag-filter] "")))

(re-frame/reg-sub
 :search/get-filtered-home-items
 :<- [:get-contacts]
 :<- [:search/get-tag-filter]
 (fn [[contacts tag-filter]]
   (if (empty? tag-filter)
     contacts
     (keep #(when (some (fn [tag]
                          (string/includes? (string/lower-case tag) (string/lower-case tag-filter)))
                        (into [(:name (val %))] (:tags (val %))))
              %)
           contacts))))

(re-frame/reg-sub
 :search/get-tags
 :<- [:get-contacts]
 (fn [contacts]
   (reduce (fn [acc {:keys [tags]}]
             (reduce (fn [acc tag]
                       (conj acc tag))
                     acc
                     tags))
           #{}
           contacts)))

(re-frame/reg-sub
 :search/get-filtered-tags
 :<- [:search/get-tags]
 :<- [:search/get-tag-filter]
 (fn [[tags tag-filter]]
   (if (empty? tag-filter)
     tags
     (keep #(when (clojure.string/includes? % tag-filter)
              %)
           tags))))

#_(re-frame/reg-sub
   :contacts/tags
   :<- [:buidl.ui/new-issue]
   (fn [issue]
     (or (:tags issue)
         #{})))

#_(re-frame/reg-sub
   :chats/tags
   :<- [:buidl.ui/new-issue]
   (fn [issue]
     (or (:tags issue)
         #{})))

#_(re-frame/reg-sub
   :search/available-tags
   :<- [:search/get-tags]
   :<- [:search/tag-input]
   :<- [:buidl.new-issue.ui/tags]
   (fn [[existing-tags tag-input issue-tags]]
     (if (empty? tag-input)
       existing-tags
       (into #{} (filter #(and (string/starts-with? % tag-input)
                               (not (issue-tags %)))
                         (conj existing-tags tag-input))))))
