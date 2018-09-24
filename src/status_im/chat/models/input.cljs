(ns status-im.chat.models.input
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [goog.object :as object]
            [status-im.chat.constants :as constants]
            [status-im.chat.models :as chat]
            [status-im.chat.commands.input :as commands.input]
            [status-im.utils.datetime :as datetime]
            [status-im.js-dependencies :as dependencies]
            [status-im.utils.fx :as fx]
            [taoensso.timbre :as log]))

(defn text->emoji
  "Replaces emojis in a specified `text`"
  [text]
  (when text
    (string/replace text
                    #":([a-z_\-+0-9]*):"
                    (fn [[original emoji-id]]
                      (if-let [emoji-map (object/get (object/get dependencies/emojis "lib") emoji-id)]
                        (object/get emoji-map "char")
                        original)))))

(fx/defn set-chat-input-text
  "Set input text for current-chat. Takes db and input text and cofx
  as arguments and returns new fx. Always clear all validation messages."
  [{{:keys [current-chat-id] :as db} :db} new-input]
  {:db (-> (chat/set-chat-ui-props db {:validation-messages nil})
           (assoc-in [:chats current-chat-id :input-text] (text->emoji new-input)))})

(fx/defn set-chat-input-metadata
  "Sets user invisible chat input metadata for current-chat"
  [{:keys [db] :as cofx} metadata]
  (let [current-chat-id (:current-chat-id db)]
    {:db (assoc-in db [:chats current-chat-id :input-metadata] metadata)}))

(defn- start-cooldown [{:keys [db]} cooldowns]
  {:dispatch-later        [{:dispatch [:chat/disable-cooldown]
                            :ms       (constants/cooldown-periods-ms cooldowns
                                                                     constants/default-cooldown-period-ms)}]
   :show-cooldown-warning nil
   :db                    (assoc db
                                 :chat/cooldowns (if (= constants/cooldown-reset-threshold cooldowns)
                                                   0
                                                   cooldowns)
                                 :chat/spam-messages-frequency 0
                                 :chat/cooldown-enabled? true)})

(fx/defn process-cooldown
  "Process cooldown to protect against message spammers"
  [{{:keys [chat/last-outgoing-message-sent-at
            chat/cooldowns
            chat/spam-messages-frequency
            current-chat-id] :as db} :db :as cofx}]
  (when (chat/public-chat? current-chat-id cofx)
    (let [spamming-fast? (< (- (datetime/timestamp) last-outgoing-message-sent-at)
                            (+ constants/spam-interval-ms (* 1000 cooldowns)))
          spamming-frequently? (= constants/spam-message-frequency-threshold spam-messages-frequency)]
      (cond-> {:db (assoc db
                          :chat/last-outgoing-message-sent-at (datetime/timestamp)
                          :chat/spam-messages-frequency (if spamming-fast?
                                                          (inc spam-messages-frequency)
                                                          0))}

        (and spamming-fast? spamming-frequently?)
        (start-cooldown (inc cooldowns))))))

(fx/defn chat-input-focus
  "Returns fx for focusing on active chat input reference"
  [{{:keys [current-chat-id chat-ui-props]} :db} ref]
  (when-let [cmp-ref (get-in chat-ui-props [current-chat-id ref])]
    {::focus-rn-component cmp-ref}))

(fx/defn select-chat-input-command
  "Sets chat command in current chat input"
  [{:keys [db] :as cofx} command params metadata]
  (fx/merge cofx
            (set-chat-input-metadata metadata)
            (commands.input/select-chat-input-command command params)
            (chat-input-focus :input-ref)))

;; effects

(re-frame/reg-fx
 ::focus-rn-component
 (fn [ref]
   (try
     (.focus ref)
     (catch :default e
       (log/debug "Cannot focus the reference")))))
