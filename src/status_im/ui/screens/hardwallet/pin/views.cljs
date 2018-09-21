(ns status-im.ui.screens.hardwallet.pin.views
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.react-native.resources :as resources]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.styles :as components.styles]
            [status-im.ui.screens.hardwallet.pin.styles :as styles]))

(defn hardwallet-pin []
  [react/view styles/container
   [react/view components.styles/flex
    [react/view styles/inner-container
     [react/view styles/maintain-card-container
      [vector-icons/icon :icons/hardwallet {:color colors/blue}]
      [react/text {:style styles/maintain-card-text}
       (i18n/label :t/maintain-card-to-phone-contact)]]
     [react/view styles/center-container
      [react/text {:style styles/center-title-text
                   :font  :bold}
       (i18n/label :t/create-pin)]
      [react/text {:style           styles/create-pin-text
                   :number-of-lines 2}
       (i18n/label :t/create-pin-description)]]
     [react/view styles/waiting-indicator-container
      [react/activity-indicator {:animating true
                                 :size      :large}]]]]])