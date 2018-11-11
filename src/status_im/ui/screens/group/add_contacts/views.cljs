(ns status-im.ui.screens.group.add-contacts.views
  (:require-macros [status-im.utils.views :refer [defview letsubs]])
  (:require [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.ui.components.contact.contact :refer [toogle-contact-view]]
            [status-im.ui.components.list.views :as list]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.status-bar.view :refer [status-bar]]
            [status-im.ui.components.toolbar.view :as toolbar]
            [status-im.ui.screens.group.styles :as styles]))

(defn- on-toggle [checked? public-key]
  (let [action (if checked? :deselect-contact :select-contact)]
    (re-frame/dispatch [action public-key])))

(defn- on-toggle-participant [checked? public-key]
  (let [action (if checked? :deselect-participant :select-participant)]
    (re-frame/dispatch [action public-key])))

(defn- group-toggle-contact [contact]
  [toogle-contact-view contact :is-contact-selected? on-toggle])

(defn- group-toggle-participant [contact]
  [toogle-contact-view contact :is-participant-selected? on-toggle-participant])

(defn- toggle-list-toolbar [{:keys [handler count label]} title]
  [toolbar/toolbar {}
   toolbar/default-nav-back
   [toolbar/content-title title]
   (when (pos? count)
     [toolbar/text-action {:handler             handler
                           :accessibility-label :next-button}
      label])])

(defn toggle-list [contacts render-function]
  [react/view {:flex 1}
   [list/flat-list {:style                     styles/contacts-list
                    :data                      contacts
                    :key-fn                    :address
                    :render-fn                 render-function
                    :keyboardShouldPersistTaps :always}]])

;; Start group chat
(defview contact-toggle-list []
  (letsubs [contacts                [:contacts/added]
            selected-contacts-count [:selected-contacts-count]]
    (when (seq contacts)
      [react/keyboard-avoiding-view {:style styles/group-container}
       [status-bar]
       [toggle-list-toolbar {:handler #(re-frame/dispatch [:navigate-to :new-group])
                             :label   (i18n/label :t/next)
                             :count   (pos? selected-contacts-count)}
        (i18n/label :t/group-chat)]
       [toggle-list contacts group-toggle-contact]])))

;; Add participants to existing group chat
(defview add-participants-toggle-list []
  (letsubs [contacts                [:contacts/not-in-current-chat]
            {:keys [name]}          [:chats/current]
            selected-contacts-count [:selected-participants-count]]
    [react/keyboard-avoiding-view {:style styles/group-container}
     [status-bar]
     [toggle-list-toolbar {:count   selected-contacts-count
                           :handler #(do
                                       (re-frame/dispatch [:group-chats.ui/add-members-pressed])
                                       (re-frame/dispatch [:navigate-back]))
                           :label   (i18n/label :t/add)}
      name]
     (when (seq contacts)
       [toggle-list contacts group-toggle-participant])]))
