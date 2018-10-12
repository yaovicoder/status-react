(ns status-im.ui.screens.extensions.db
  (:require-macros [status-im.utils.db :refer [allowed-keys]])
  (:require
   [clojure.string :as string]
   [cljs.spec.alpha :as spec]))

(spec/def ::not-blank-string (complement string/blank?))

(spec/def :extension/url ::not-blank-string)
(spec/def :extension/id ::not-blank-string)
(spec/def :extension/extension (allowed-keys :req-un [:extension/id
                                                      :extension/url]))

(spec/def :extensions/extensions (spec/nilable (spec/map-of :extension/id :extension/extension)))
