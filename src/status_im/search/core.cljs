(ns status-im.search.core
  (:require [re-frame.core :as re-frame]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.fx :as fx]
            [cljs.spec.alpha :as spec]))

(handlers/register-handler-fx
 :search/tag-filter-changed
 (fn [cofx [_ tag-filter]]
   {:db (assoc-in (:db cofx) [:ui/search :tag-filter] tag-filter)}))

(handlers/register-handler-fx
 :buidl/add-tag
 (fn [cofx [_ tag]]
   (when (spec/valid? :buidl/tag tag)
     {:db (update-in (:db cofx) [:ui/buidl :new-issue :tags] #(if % (conj % tag) #{tag}))})))
