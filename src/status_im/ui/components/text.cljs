(ns status-im.ui.components.text
  (:require [status-im.ui.components.react :as react]
            [status-im.utils.platform :as platform]))

(defn- selectable-text [{:keys [font value style]}]
  (if platform/ios?
    [react/text-input {:font      font
                       :editable  false
                       :multiline true
                       :style     style
                       :value     value}]
    [react/text {:style               style
                 :font                font
                 :accessibility-label :address-text
                 :selectable          true}
     value]))
