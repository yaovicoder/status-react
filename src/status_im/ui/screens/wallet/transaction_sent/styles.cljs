(ns status-im.ui.screens.wallet.transaction-sent.styles
  (:require [status-im.ui.components.colors :as colors]))

(def transaction-sent
  {:color         colors/white
   :font-weight   :bold
   :line-height   27
   :font-size     22
   :margin-bottom 8})

(def transaction-sent-description
  {:color              "rgba(255,255,255,0.6)"
   :font-size          15
   :line-height        22
   :text-align         :center
   :padding-horizontal 30
   :margin-bottom      35})
