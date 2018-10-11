(ns status-im.extensions.core
  (:require [clojure.string :as string]
            [status-im.utils.fx :as fx]))

(defn url->storage-details [s]
  (when s
    (let [[_ type id] (string/split s #".*[:/]([a-z]*)@(.*)")]
      [(keyword type) id])))

(fx/defn set-extension-url-from-qr
  [{:keys [db]} url]
  {:db       (assoc db :extension-url url)
   :dispatch [:navigate-back]})
