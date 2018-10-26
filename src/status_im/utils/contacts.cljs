(ns status-im.utils.contacts
  (:require [status-im.js-dependencies :as js-dependencies]
            [status-im.utils.identicon :as identicon]
            [status-im.utils.gfycat.core :as gfycat]))

(defn public-key->new-contact [public-key]
  {:name       (gfycat/generate-gfy public-key)
   :photo-path (identicon/identicon public-key)
   :public-key public-key})

(defn public-key->address [public-key]
  (let [length (count public-key)
        normalized-key (case length
                         132 (subs public-key 4)
                         130 (subs public-key 2)
                         128 public-key
                         nil)]
    (when normalized-key
      (subs (.sha3 js-dependencies/Web3.prototype normalized-key #js {:encoding "hex"}) 26))))
