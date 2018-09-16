(ns status-im.ui.components.qr-code-viewer.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [status-im.react-native.js-dependencies :as rn-dependencies]
            [status-im.ui.components.button.view :as button]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.ui.components.qr-code-viewer.styles :as styles]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.text :as text]
            [status-im.i18n :as i18n]))

(defn qr-code [props]
  (reagent/create-element
   rn-dependencies/qr-code
   (clj->js (merge {:inverted true} props))))

(defn qr-code-viewer [{:keys [style hint-style footer-style]} value hint legend]
  (if value
    (let [{:keys [width height]} @(re-frame/subscribe [:dimensions/window])]
      [react/view {:style (merge styles/qr-code style)}
       (when width
         (let [size (int (* 0.7 (min width height)))]
           [react/view {:style               styles/qr-code-container
                        :accessibility-label :qr-code-image}
            [qr-code {:value value
                      :size  (- size (* 2 styles/qr-code-padding))}]]))
       [react/text {:style (merge styles/qr-code-hint hint-style)}
        hint]
       [react/view styles/footer
        [react/view styles/wallet-info
         [text/selectable-text {:value legend
                                :style (merge styles/hash-value-text footer-style)}]]]
       [button/button-with-icon {:on-press            #(list-selection/open-share {:message value})
                                 :label               (i18n/label :t/share-link)
                                 :icon                :icons/share
                                 :accessibility-label :share-my-contact-code-button
                                 :style               {:margin-top    32
                                                       :margin-bottom 16}}]])
    [react/view [react/text "no value"]]))
