(ns status-im.ui.screens.browser.events
  (:require [re-frame.core :as re-frame]
            [status-im.constants :as constants]
            [status-im.data-store.browser :as browser-store]
            [status-im.browser.core :as browser]
            [status-im.native-module.core :as status]
            [status-im.ui.components.list-selection :as list-selection]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.handlers-macro :as handlers-macro]
            [status-im.utils.http :as http]
            [status-im.utils.platform :as platform]
            [status-im.utils.random :as random]
            [status-im.utils.types :as types]
            [status-im.utils.universal-links.core :as utils.universal-links]
            [taoensso.timbre :as log]
            [status-im.utils.ethereum.resolver :as resolver]
            [status-im.utils.ethereum.core :as ethereum]))

(re-frame/reg-fx
 :browse
 (fn [link]
   (if (utils.universal-links/universal-link? link)
     (utils.universal-links/open! link)
     (list-selection/browse link))))

(re-frame/reg-fx
 :call-rpc
 (fn [[payload callback]]
   (status/call-rpc
    (types/clj->json payload)
    (fn [response]
      (if (= "" response)
        (do
          (log/warn :web3-response-error)
          (callback "web3-response-error" nil))
        (callback nil (.parse js/JSON response)))))))

(re-frame/reg-fx
 :send-to-bridge-fx
 (fn [[message webview]]
   (.sendToBridge webview (types/clj->json message))))

(re-frame/reg-fx
 :resolve-ens-multihash
 (fn [{:keys [web3 registry ens-name cb]}]
   (resolver/content web3 registry ens-name cb)))

(handlers/register-handler-fx
 :browse-link-from-message
 (fn [_ [_ link]]
   {:browse link}))

(handlers/register-handler-fx
 :ens-multihash-resolved
 (fn [{:keys [db] :as cofx} [_ hash]]
   (let [options (:browser/options db)
         browsers (:browser/browsers db)
         browser (get browsers (:browser-id options))
         history-index (:history-index browser)]
     (handlers-macro/merge-fx
      cofx
      {:db (assoc-in db [:browser/options :resolving?] false)}
      (browser/update-browser-fx
       (assoc-in browser [:history history-index] (str "https://ipfs.infura.io/ipfs/" hash)))))))

(handlers/register-handler-fx
 :open-url-in-browser
 (fn [cofx [_ url]]
   (browser/open-url-in-browser url cofx)))

(handlers/register-handler-fx
 :send-to-bridge
 (fn [cofx [_ message]]
   {:send-to-bridge-fx [message (get-in cofx [:db :webview-bridge])]}))

(handlers/register-handler-fx
 :open-browser
 (fn [cofx [_ browser]]
   (browser/update-browser-and-navigate browser cofx)))

(handlers/register-handler-fx
 :update-browser-on-nav-change
 (fn [cofx [_ browser url loading error?]]
   (let [host (http/url-host url)]
     (handlers-macro/merge-fx
      cofx
      (browser/resolve-multihash-fx host loading error?)
      (browser/update-browser-history-fx browser url loading)))))

(handlers/register-handler-fx
 :update-browser-options
 (fn [{:keys [db]} [_ options]]
   {:db (update db :browser/options merge options)}))

(handlers/register-handler-fx
 :remove-browser
 (fn [{:keys [db]} [_ browser-id]]
   {:db            (update-in db [:browser/browsers] dissoc browser-id)
    :data-store/tx [(browser-store/remove-browser-tx browser-id)]}))

(defn nav-update-browser [cofx browser history-index]
  (browser/update-browser-fx (assoc browser :history-index history-index) cofx))

(handlers/register-handler-fx
 :browser-nav-back
 (fn [cofx [_ {:keys [history-index] :as browser}]]
   (when (pos? history-index)
     (nav-update-browser cofx browser (dec history-index)))))

(handlers/register-handler-fx
 :browser-nav-forward
 (fn [cofx [_ {:keys [history-index] :as browser}]]
   (when (< history-index (dec (count (:history browser))))
     (nav-update-browser cofx browser (inc history-index)))))

(handlers/register-handler-fx
 :browser/bridge-message-received
 (fn [cofx [_ message]]
   (browser/process-bridge-message message cofx)))

(handlers/register-handler-fx
 :check-permissions-queue
 (fn [cofx _]
   (browser/check-permissions-queue cofx)))

(handlers/register-handler-fx
 :next-dapp-permission
 (fn [cofx [_ params permission permissions-data]]
   (browser/next-permission {:params           params
                             :permission       permission
                             :permissions-data permissions-data}
                            cofx)))
