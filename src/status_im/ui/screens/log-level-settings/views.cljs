(ns status-im.ui.screens.log-level-settings.views
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.ui.components.icons.vector-icons :as vector-icons]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.status-bar.view :as status-bar]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.ui.screens.log-level-settings.styles :as styles]
            [status-im.utils.platform :as platform])
  (:require-macros [status-im.utils.views :as views]))

(defn- log-level-icon [current?]
  [react/view (if platform/desktop?
                {:style (styles/log-level-icon-container connected?)}
                (styles/log-level-icon-container connected?))
   [vector-icons/icon :icons/log-level
    (if platform/desktop? {:style (styles/log-level-icon connected?)}
        (styles/log-level-icon connected?))]])

(defn change-log-level [log-level]
  (re-frame/dispatch [:change-log-level log-level]))

(defn render-row [current-log-level]
  (fn [log-level]
    (let [current? (= log-level current-log-level)]
      [react/touchable-highlight
       {:on-press (change-log-level log-level)
        :accessibility-label :log-level-item}
       [react/view styles/log-level-item
        [log-level-icon current?]
        [react/view styles/log-level-item-inner
         [react/text {:style styles/log-level-item-name-text}
          log-level]]]])))

(def log-levels
  ["DEBUG" "INFO" "ERROR"])

(views/defview log-level-settings []
  (views/letsubs [current-log-level [:settings/current-log-level]]
    [react/view {:flex 1}
     [status-bar/status-bar]
     [toolbar/toolbar {}
      toolbar/default-nav-back
      [toolbar/content-title (i18n/label :t/offline-messaging-settings)]]
     [react/view styles/wrapper
      [list/flat-list {:data               log-levels
                       :default-separator? false
                       :key-fn             :id
                       :render-fn          (render-row current-log-level-id)}]]]))
