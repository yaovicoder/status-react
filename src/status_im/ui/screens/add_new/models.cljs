(ns status-im.ui.screens.add-new.models
  (:require [cljs.spec.alpha :as spec]
            [re-frame.core :as re-frame]
            [status-im.i18n :as i18n]
            [status-im.utils.universal-links.core :as universal-links]))

(defn handle-qr-code [data cofx]
  (if (spec/valid? :global/public-key data)
    (universal-links/handle-view-profile data cofx)
    (or (universal-links/handle-url data cofx)
        {:utils/show-popup [(i18n/label :t/unable-to-read-this-code)
                            (i18n/label :t/use-valid-contact-code)
                            #(re-frame/dispatch [:navigate-to-clean :home])]})))
