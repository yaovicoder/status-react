(ns status-im.ui.screens.wallet.onboarding.setup.styles
  (:require [status-im.ui.components.colors :as colors]))

(def setup-image-container
  {:align-items :center
   :margin      41})

(def setup-image
  {:width  151
   :height 77})

(def signing-phrase
  {:background-color colors/white
   :border-radius    8
   :margin-left      34
   :margin-right     34
   :flex-direction   :row})

(def signing-emoji-container
  {:flex            1
   :height          68
   :align-items     :center
   :justify-content :center})

(def signing-emoji-container-left-border
  {:border-left-color colors/gray-border
   :border-left-width 1})

(def signing-emoji
  {:font-size      24
   :letter-spacing -0.2})

(def super-safe-transactions
  {:margin-top 40
   :font-size 22
   :text-align :center
   :color colors/white
   :font-weight :bold})

(def description
  {:font-size      14
   :font-style :normal
   :letter-spacing -0.17
   :line-height 21
   :opacity 0.6
   :color          colors/white
   :margin-left    24
   :margin-right   24
   :margin-top     16
   :text-align     :center})

(def warning
  {:border-color colors/white
   :border-width 1
   :border-radius 8
   :font-size      14
   :font-style :normal
   :letter-spacing -0.17
   :line-height 21
   :opacity 0.6
   :color          colors/white
   :margin-left    57
   :margin-right   57
   :margin-top     28
   :text-align     :center})

(def bottom-buttons
  {:background-color colors/blue
   :padding-vertical 8})

(def got-it-button-text
  {:padding-horizontal 0})

(def modal
  {:flex             1
   :background-color colors/blue})
