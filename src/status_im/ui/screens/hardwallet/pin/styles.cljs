(ns status-im.ui.screens.hardwallet.pin.styles
  (:require [status-im.ui.components.colors :as colors]))

(def container
  {:flex             1
   :background-color colors/white})

(def inner-container
  {:flex-direction  :column
   :flex            1
   :align-items     :center
   :justify-content :space-between})

(def center-container
  {:flex-direction :column
   :align-items    :center
   :height         200})

(def center-title-text
  {:font-size 22
   :color     colors/black})

(def create-pin-text
  {:font-size   15
   :padding-top 8
   :width       314
   :text-align  :center
   :color       colors/gray})

(def waiting-indicator-container
  {:height 200})

(def maintain-card-container
  {:flex-direction  :row
   :align-items     :center
   :justify-content :center
   :margin-top      81
   :width           369
   :height          60
   :border-radius   10
   :border-width    1
   :border-color    colors/blue
   :border-style    :dashed})

(def maintain-card-text
  {:padding-horizontal 20
   :font-size          12
   :color              colors/blue})

