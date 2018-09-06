(ns status-im.ui.screens.offline-messaging-settings.events
  (:require [re-frame.core :as re-frame]

            [status-im.utils.handlers :as handlers]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.ui.screens.accounts.models :as accounts.models]
            [status-im.i18n :as i18n]
            status-im.ui.screens.offline-messaging-settings.edit-mailserver.events))
