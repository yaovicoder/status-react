(ns status-im.ui.screens.hardwallet.setup.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [re-frame.core :as re-frame]
            [status-im.react-native.resources :as resources]
            [status-im.ui.screens.hardwallet.setup.styles :as styles]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.styles :as components.styles]
            [status-im.i18n :as i18n]
            [status-im.ui.components.colors :as colors]))

(defview hardwallet-setup []
  [react/view styles/container
   [react/view components.styles/flex
    [react/view styles/inner-container
     [react/view styles/maintain-card-container
      [vector-icons/icon :icons/hardwallet {:color colors/blue}]
      [react/text {:style styles/maintain-card-text}
       (i18n/label :t/maintain-card-to-phone-contact)]]
     [react/view styles/hardwallet-card-image-container
      [react/image {:source (:hardwallet-card resources/ui)
                    :style  styles/hardwallet-card-image}]
      [react/view styles/card-is-empty-text-container
       [react/text {:style styles/card-is-empty-text}
        (i18n/label :t/card-is-empty)]]]
     [react/view styles/bottom-action-container
      [react/text {:style      styles/begin-set-up-text
                   :font       :medium
                   :uppercase? true}
       (i18n/label :t/begin-set-up)]]]]])