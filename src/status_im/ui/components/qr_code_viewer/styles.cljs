(ns status-im.ui.components.qr-code-viewer.styles
  (:require [status-im.ui.components.colors :as colors])
  (:require-macros [status-im.utils.styles :refer [defstyle]]))

(def qr-code-hint
  {:color          colors/gray
   :padding-bottom 16
   :text-align     :center})

(def qr-code-padding 40)

(defn qr-code-container [width]
  {:width              width
   :align-items        :center
   :justify-content    :center
   :padding-horizontal qr-code-padding
   :padding-bottom     qr-code-padding})

(defstyle name-container
  {:flex           0.6
   :flex-direction :column
   :ios            {:align-items :center}
   :android        {:margin-left 15}})

(defstyle name-text
  {:color     colors/black
   :font-size 17
   :ios       {:letter-spacing -0.2}})

(def address-text
  {:color     colors/white
   :font-size 12})

(def wallet-qr-code
  {:flex-grow      1
   :flex-direction :column})

(def account-toolbar
  {:background-color colors/white})

(def qr-code
  {:flex-grow        1
   :justify-content  :center})

(def footer
  {:flex-direction   :row
   :justify-content  :center})

(def wallet-info
  {:flex-grow      1
   :align-items    :center})

(def hash-value-type
  {:color          colors/black
   :padding-bottom 5})

(def hash-value-text
  {:color              colors/black
   :align-self         :stretch
   :border-color       colors/gray-border
   :border-width       1
   :margin-horizontal  16
   :padding-horizontal 8
   :padding-bottom     6
   :border-radius      8
   :text-align         :left
   :font-size          15
   :letter-spacing     -0.2
   :line-height        20})

(def done-button-text
  {:color colors/white})

(def share-link-button
  {:margin-top    32
   :margin-bottom 16})
