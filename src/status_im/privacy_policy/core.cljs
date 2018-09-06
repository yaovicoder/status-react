(ns status-im.privacy-policy.core
  (:require [status-im.ui.components.react :as react]))

(def ^:const privacy-policy-link "https://www.iubenda.com/privacy-policy/45710059")

(re-frame/reg-fx
 :privacy-policy/open-privacy-policy-link
 (fn [] (.openURL react/linking privacy-policy-link)))
