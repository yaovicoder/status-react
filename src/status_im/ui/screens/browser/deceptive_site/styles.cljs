(ns status-im.ui.screens.browser.deceptive-site.styles
  (:require-macros [status-im.utils.styles :refer [defstyle defnstyle]])
  (:require [status-im.ui.components.colors :as colors]))

(def container
  {:justify-content  :center
   :flex             1
   :background-color colors/blue-dark})

(def container-root-view
  {:flex            1
   :margin          16
   :justify-content :center})

(defstyle title-text
  {:color         colors/white
   :font-size     26
   :margin-bottom 12
   :ios           {:letter-spacing -0.4}})

(defstyle description-text
  {:color     "white"
   :font-size 16
   :ios       {:letter-spacing -0.2}})

(def buttons-container
  {:flex-direction :row
   :margin-top     20})

(def button-container
  {:background-color colors/white-light-transparent})

(def button-text
  {:padding-horizontal 0})

(def more-details-container
  {:margin-top     32
   :flex-direction :column})