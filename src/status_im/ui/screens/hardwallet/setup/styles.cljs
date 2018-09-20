(ns status-im.ui.screens.hardwallet.setup.styles
  (:require [status-im.ui.components.colors :as colors]))

(def container
  {:flex             1
   :background-color colors/white})

(def inner-container
  {:flex-direction  :column
   :flex            1
   :align-items     :center
   :justify-content :space-between})

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

(def hardwallet-card-image-container
  {:margin-top -50})

(def hardwallet-card-image
  {:width  255
   :height 160})

(def card-is-empty-text-container
  {:margin-top 0})

(def card-is-empty-text
  {:font-size  15
   :color      colors/gray
   :text-align :center})

(def bottom-action-container
  {:background-color colors/gray-lighter
   :align-items      :center
   :justify-content  :center
   :flex-direction   :row
   :width            160
   :height           44
   :border-radius    10
   :margin-bottom    20})

(def begin-set-up-text
  {:font-size      14
   :color          colors/blue
   :line-height    20
   :text-transform :uppercase})
