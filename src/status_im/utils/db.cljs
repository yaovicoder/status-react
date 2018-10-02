(ns status-im.utils.db
  (:require [clojure.string :as string]
            [cljs.spec.alpha :as spec]
            [cljs.spec.gen.alpha :as gen]
            [clojure.test.check.generators :as tgen]
            [status-im.js-dependencies :as dependencies]
            [status-im.utils.ethereum.core :as ethereum]))

(defn valid-public-key? [s]
  (boolean (re-matches #"0x04[0-9a-f]{128}" s)))

(spec/def :global/not-empty-string (spec/and string? not-empty))
(spec/def :global/public-key (spec/and :global/not-empty-string valid-public-key?))
(spec/def :global/address (spec/with-gen ethereum/address?
                            #(gen/fmap string/join
                                       (gen/vector (gen/fmap (fn [n] (-> n (.toString 16) (.padStart 2 0)))
                                                             (spec/gen (spec/int-in 0 256)))
                                                   20))))
