(ns status-im.chat.subs
  (:require [clojure.string :as string]
            [re-frame.core :refer [reg-sub subscribe]]
            [status-im.chat.commands.core :as commands]
            [status-im.chat.commands.input :as commands.input]
            [status-im.chat.constants :as chat.constants]
            [status-im.chat.db :as chat.db]
            [status-im.models.transactions :as transactions]
            [status-im.utils.platform :as platform]))

(reg-sub :get-chats :chats)

(reg-sub :get-current-chat-id :current-chat-id)

(reg-sub :chat-ui-props :chat-ui-props)

(reg-sub :get-id->command :id->command)

(reg-sub :get-access-scope->command-id :access-scope->command-id)

(reg-sub
 :get-current-chat-ui-props
 :<- [:chat-ui-props]
 :<- [:get-current-chat-id]
 (fn [[chat-ui-props id]]
   (get chat-ui-props id)))

(reg-sub
 :get-current-chat-name
 :<- [:get-current-chat-contact]
 :<- [:get-current-chat]
 (fn [[contact chat]]
   (chat.db/chat-name chat contact)))

(reg-sub
 :get-chat-name
 :<- [:get-contacts]
 :<- [:get-chats]
 (fn [[contacts chats] [_ chat-id]]
   (chat.db/chat-name (get chats chat-id) (get contacts chat-id))))

(reg-sub
 :get-current-chat-ui-prop
 :<- [:get-current-chat-ui-props]
 (fn [ui-props [_ prop]]
   (get ui-props prop)))

(reg-sub
 :validation-messages
 :<- [:get-current-chat-ui-props]
 (fn [ui-props]
   (some-> ui-props :validation-messages)))

(reg-sub
 :chat-input-margin
 :<- [:get :keyboard-height]
 (fn [kb-height]
   (cond
     (and platform/iphone-x? (> kb-height 0)) (- kb-height 34)
     platform/ios? kb-height
     :default 0)))

(reg-sub
 :get-active-chats
 :<- [:get-contacts]
 :<- [:get-chats]
 :<- [:account/account]
 (fn [[contacts chats account]]
   (chat.db/active-chats contacts chats account)))

(reg-sub
 :get-chat
 :<- [:get-active-chats]
 (fn [chats [_ chat-id]]
   (get chats chat-id)))

(reg-sub
 :get-current-chat
 :<- [:get-active-chats]
 :<- [:get-current-chat-id]
 (fn [[chats current-chat-id]]
   (get chats current-chat-id)))

(reg-sub
 :get-current-chat-message
 :<- [:get-current-chat]
 (fn [{:keys [messages]} [_ message-id]]
   (get messages message-id)))

(reg-sub
 :get-current-chat-messages
 :<- [:get-current-chat]
 (fn [{:keys [messages]}]
   (or messages {})))

(reg-sub
 :get-current-chat-message-groups
 :<- [:get-current-chat]
 (fn [{:keys [message-groups]}]
   (or message-groups {})))

(reg-sub
 :get-current-chat-message-statuses
 :<- [:get-current-chat]
 (fn [{:keys [message-statuses]}]
   (or message-statuses {})))

(reg-sub
 :get-current-chat-referenced-messages
 :<- [:get-current-chat]
 (fn [{:keys [referenced-messages]}]
   (or referenced-messages {})))

(reg-sub
 :get-current-chat-messages-stream
 :<- [:get-current-chat-messages]
 :<- [:get-current-chat-message-groups]
 :<- [:get-current-chat-message-statuses]
 :<- [:get-current-chat-referenced-messages]
 (fn [[messages message-groups message-statuses referenced-messages]]
   (-> (chat.db/sort-message-groups message-groups messages)
       (chat.db/messages-with-datemarks-and-statuses messages message-statuses referenced-messages)
       chat.db/messages-stream)))

(reg-sub
 :get-commands-for-chat
 :<- [:get-id->command]
 :<- [:get-access-scope->command-id]
 :<- [:get-current-chat]
 (fn [[id->command access-scope->command-id chat]]
   (commands/chat-commands id->command access-scope->command-id chat)))

(reg-sub
 :get-available-commands
 :<- [:get-commands-for-chat]
 :<- [:get-current-chat]
 (fn [[commands chat]]
   (chat.db/available-commands commands chat)))

(reg-sub
 :get-all-available-commands
 :<- [:get-commands-for-chat]
 (fn [commands]
   (chat.db/map->sorted-seq commands)))

(reg-sub
 :selected-chat-command
 :<- [:get-current-chat]
 :<- [:get-current-chat-ui-prop :selection]
 :<- [:get-commands-for-chat]
 (fn [[{:keys [input-text]} selection commands]]
   (commands.input/selected-chat-command input-text selection commands)))

(reg-sub
 :chat-input-placeholder
 :<- [:get-current-chat]
 :<- [:selected-chat-command]
 (fn [[{:keys [input-text]} {:keys [params current-param-position]}]]
   (when (string/ends-with? (or input-text "") chat.constants/spacing-char)
     (get-in params [current-param-position :placeholder]))))

(reg-sub
 :chat-parameter-box
 :<- [:get-current-chat]
 :<- [:selected-chat-command]
 (fn [[_ {:keys [current-param-position params]}]]
   (when (and params current-param-position)
     (get-in params [current-param-position :suggestions]))))

(reg-sub
 :show-parameter-box?
 :<- [:chat-parameter-box]
 :<- [:show-suggestions?]
 :<- [:validation-messages]
 :<- [:selected-chat-command]
 (fn [[chat-parameter-box show-suggestions? validation-messages {:keys [command-completion]}]]
   (and chat-parameter-box
        (not validation-messages)
        (not show-suggestions?)
        (not (= :complete command-completion)))))

(reg-sub
 :show-suggestions-view?
 :<- [:get-current-chat-ui-prop :show-suggestions?]
 :<- [:get-current-chat]
 :<- [:get-all-available-commands]
 (fn [[show-suggestions? {:keys [input-text]} commands]]
   (and (or show-suggestions?
            (commands.input/starts-as-command? (string/trim (or input-text ""))))
        (seq commands))))

(reg-sub
 :show-suggestions?
 :<- [:show-suggestions-view?]
 :<- [:selected-chat-command]
 (fn [[show-suggestions-box? selected-command]]
   (and show-suggestions-box? (not selected-command))))

(reg-sub
 :unviewed-messages-count
 (fn [[_ chat-id]]
   (subscribe [:get-chat chat-id]))
 (fn [{:keys [unviewed-messages]}]
   (count unviewed-messages)))

(reg-sub
 :get-photo-path
 :<- [:get-contacts]
 :<- [:account/account]
 (fn [[contacts account] [_ id]]
   (or (:photo-path (contacts id))
       (when (= id (:public-key account))
         (:photo-path account)))))

(reg-sub
 :get-last-message
 (fn [[_ chat-id]]
   (subscribe [:get-chat chat-id]))
 (fn [{:keys [messages message-groups]}]
   (->> (chat.db/sort-message-groups message-groups messages)
        first
        second
        last
        :message-id
        (get messages))))

(reg-sub
 :chat-animations
 (fn [db [_ key type]]
   (let [chat-id (subscribe [:get-current-chat-id])]
     (get-in db [:animations :chats @chat-id key type]))))

(reg-sub
 :get-chats-unread-messages-number
 :<- [:get-active-chats]
 (fn [chats _]
   (apply + (map (comp count :unviewed-messages) (vals chats)))))

(reg-sub
 :transaction-confirmed?
 (fn [db [_ tx-hash]]
   (-> (get-in db [:wallet :transactions tx-hash :confirmations] "0")
       (js/parseInt)
       (>= transactions/confirmations-count-threshold))))

(reg-sub
 :wallet-transaction-exists?
 (fn [db [_ tx-hash]]
   (not (nil? (get-in db [:wallet :transactions tx-hash])))))

(reg-sub
 :chat/cooldown-enabled?
 (fn [db]
   (:chat/cooldown-enabled? db)))

(reg-sub
 :chat-cooldown-enabled?
 :<- [:get-current-chat]
 :<- [:chat/cooldown-enabled?]
 (fn [[{:keys [public?]} cooldown-enabled?]]
   (and public?
        cooldown-enabled?)))

(reg-sub
 :get-reply-message
 :<- [:get-current-chat]
 (fn [{:keys [metadata messages]}]
   (get messages (:responding-to-message metadata))))
