(ns status-im.ui.screens.add-new.events
  (:require [cljs.spec.alpha :as spec]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.universal-links.core :as universal-links]
            [taoensso.timbre :as log]))

(handlers/register-handler-fx
 :handle-qr-code
 (fn [cofx [_ _ data]]
   (log/debug "qr code scanned with data " data)
   (if (spec/valid? :global/public-key data)
     (universal-links/handle-view-profile data cofx)
     (or (universal-links/handle-url data cofx)
         {:utils/show-popup [(i18n/label :t/unable-to-read-this-code)
                             (i18n/label :t/use-valid-contact-code)
                             #(re-frame/dispatch [:navigate-to-clean :home])]}))))
