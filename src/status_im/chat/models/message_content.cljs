(ns status-im.chat.models.message-content
  (:require [status-im.constants :as constants]))

(def ^:private actions {:link    constants/regx-url
                        :tag     constants/regx-tag
                        :mention constants/regx-mention})

(def ^:private stylings {:bold   constants/regx-bold
                         :italic constants/regx-italic})

(def ^:private type->regex (merge actions stylings))

(defn- right-to-left-text? [text]
  (and (seq text)
       (re-matches constants/regx-rtl-characters (first text))))

(defn- query-regex [regex content]
  (loop [input   content
         matches []
         offset  0]
    (if-let [match (.exec regex input)]
      (let [match-value    (aget match 0)
            relative-index (.-index match)
            start-index    (+ offset relative-index)
            end-index      (+ start-index (count match-value))]
        (recur (apply str (drop end-index input))
               (conj matches [start-index end-index])
               end-index))
      (seq matches))))

(defn enrich-content
  "Enriches message content with `:metadata` and `:rtl?` information.
  Metadata map keys can by any of the `:link`, `:tag`, `:mention` actions
  or `:bold` and `:italic` stylings.
  Value for each key is sequence of tuples representing ranges in original
  `:text` content. "
  [{:keys [text] :as content}]
  (let [metadata (reduce-kv (fn [metadata type regex]
                              (if-let [matches (query-regex regex text)]
                                (assoc metadata type matches)
                                metadata))
                            {}
                            type->regex)]
    (cond-> content
      (seq metadata) (assoc :metadata metadata)
      (right-to-left-text? text) (assoc :rtl? true))))

(defn- can-embedd? [outer-type inner-type]
  ;; anything can be embedded inside text
  (or (= :text outer-type)
      ;; for now, we can only embedd actions inside stylings and not vice versa
      (and (contains? stylings outer-type)
           (contains? actions inner-type))
      ;; Styling inside styling (eq bold inside italic and vice versa) is supported
      (and (contains? stylings outer-type)
           (contains? stylings inner-type))))

(defn- update-indexes [[indexes node] offset]
  [(mapv #(- % offset) indexes) node])

(defn build-render-tree
  "Builds render tree from message text and metadata, can be used by render code
  by simply walking the tree and calling `subs` on provided ranges/message text."
  [{:keys [text metadata]}]
  (let [sorted-ranges (->> metadata
                           (reduce-kv (fn [acc type ranges]
                                        (reduce #(assoc %1 %2 {:type type}) acc ranges))
                                      {})
                           (sort-by ffirst))]
    (reduce (fn [acc [[start-idx end-idx] node :as record]]
              (let [[[last-start-idx last-end-idx] last-node] (last acc)]
                (cond
                  (> start-idx last-end-idx)
                  (conj acc record) ;; next record is not embedded, simply append

                  (and (< end-idx last-end-idx)
                       (can-embedd? (:type last-node) (:type node))) ;; valid embedded record, append to childern with updated indexes
                  (update-in acc [(dec (count acc)) 1 :children] (fnil conj []) (update-indexes record last-start-idx))

                  :else ;; any other case (can't be embedded, overlapping indexes), just drop the record
                  acc)))
            [[[0 (count text)] {:type :text}]]
            sorted-ranges)))

(defn emoji-only-content?
  "Determines if text is just an emoji"
  [{:keys [text]}]
  (and (string? text) (re-matches constants/regx-emoji text)))
