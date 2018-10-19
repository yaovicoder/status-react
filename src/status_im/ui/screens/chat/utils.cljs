(ns status-im.ui.screens.chat.utils
  (:require [re-frame.core :as re-frame]
            [status-im.utils.gfycat.core :as gfycat]
            [status-im.i18n :as i18n]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.colors :as colors]))

(defn format-author [from username]
  (str (when username (str username " :: "))
       (gfycat/generate-gfy from))) ; TODO: We defensively generate the name for now, to be revisited when new protocol is defined

(defn format-reply-author [from username current-public-key]
  (or (and (= from current-public-key) (i18n/label :t/You))
      (format-author from username)))

(def ^:private styling->style
  {:bold   {:font-weight :bold}
   :italic {:font-style  :italic}})

(def ^:private action->prop-fn
  {:link   (fn [text {:keys [outgoing]}]
             {:style    {:color                (if outgoing colors/white colors/blue)
                         :text-decoration-line :underline}
              :on-press #(re-frame/dispatch [:browser.ui/message-link-pressed text])})
   :tag    (fn [text {:keys [outgoing]}]
             {:style    {:color                (if outgoing colors/white colors/blue)
                         :text-decoration-line :underline}
              :on-press #(re-frame/dispatch [:chat.ui/start-public-chat (subs text 1)])})})

(defn- lookup-props [text-chunk message kind-set]
  (let [style   (apply merge (keep styling->style kind-set))
        prop-fn (some action->prop-fn kind-set)]
    (if prop-fn
      (update (prop-fn text-chunk message) :style merge style)
      {:style style})))

(defn render-chunks [render-recipe message]
  (map-indexed (fn [idx [text-chunk kind-set]]
                 [react/text (into {:key idx} (lookup-props text-chunk message kind-set))
                  text-chunk])
               render-recipe))
