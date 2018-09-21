(ns status-im.test.browser.events
  (:require [cljs.test :refer-macros [deftest is testing]]
            [day8.re-frame.test :refer-macros [run-test-sync]]
            [status-im.init.core :as init]
            status-im.ui.screens.db
            status-im.ui.screens.subs
            [re-frame.core :as re-frame]
            [status-im.browser.core :as browser]
            [status-im.utils.types :as types]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.handlers-macro :as handlers-macro]))

(defn test-fixtures []

  (re-frame/reg-fx :init/init-store #())

  (re-frame/reg-fx :browser/show-browser-selection #())
  (re-frame/reg-fx :data-store/tx #())

  (re-frame/reg-cofx
   :data-store/all-browsers
   (fn [coeffects _]
     (assoc coeffects :all-stored-browsers [])))

  (re-frame/reg-cofx
   :data-store/all-dapp-permissions
   (fn [coeffects _]
     (assoc coeffects :all-dapp-permissions [])))

  (re-frame/reg-fx :browser/send-to-bridge #())

  (re-frame/reg-fx
   :show-dapp-permission-confirmation-fx
   (fn [[permission {:keys [dapp-name permissions-data index] :as params}]]
     (if (and (= dapp-name "test.com") (#{0 1} index))
       (re-frame/dispatch [:next-dapp-permission params permission permissions-data])
       (re-frame/dispatch [:next-dapp-permission params]))))

  (handlers/register-handler-fx
   [(re-frame/inject-cofx :data-store/all-browsers)
    (re-frame/inject-cofx :data-store/all-dapp-permissions)]
   :initialize-test
   (fn [cofx [_]]
     (handlers-macro/merge-fx cofx
                              (init/initialize-app-db)
                              (browser/initialize-browsers)
                              (browser/initialize-dapp-permissions)))))

(deftest browser-events

  (run-test-sync

   (test-fixtures)

   (re-frame/dispatch [:initialize-test])

   (let [browsers  (re-frame/subscribe [:browsers])
         dapp1-url "cryptokitties.co"
         dapp2-url "http://test2.com"]

     (testing "open and remove dapps"
       (is (zero? (count @browsers)))

       (re-frame/dispatch [:browser.ui/dapp-url-submitted dapp1-url])

       (is (= 1 (count @browsers)))

       (re-frame/dispatch [:browser.ui/dapp-url-submitted dapp2-url])

       (is (= 2 (count @browsers)))

       (let [browser1 (first (vals @browsers))
             browser2 (second (vals @browsers))]
         (is (and (:dapp? browser1)
                  (not (:dapp? browser2))))
         (is (and (zero? (:history-index browser1))
                  (zero? (:history-index browser2))))
         (is (and (= [(str "http://" dapp1-url) (:history browser1)])
                  (= [dapp2-url] (:history browser2)))))

       (re-frame/dispatch [:browser.ui/remove-browser-pressed dapp1-url])

       (is (= 1 (count @browsers))))

     (testing "navigate dapp"

       (re-frame/dispatch [:browser.ui/browser-item-selected (first (vals @browsers))])

       (let [browser    (re-frame/subscribe [:get-current-browser])
             dapp2-url2 (str dapp2-url "/nav2")
             dapp2-url3 (str dapp2-url "/nav3")]

         (is (zero? (:history-index @browser)))
         (is (= [dapp2-url] (:history @browser)))

         (is (and (not (browser/can-go-back? @browser))
                  (not (browser/can-go-forward? @browser))))

         (re-frame/dispatch [:browser.ui/previous-page-button-pressed])
         (re-frame/dispatch [:browser.ui/next-page-button-pressed])

         (re-frame/dispatch [:browser/navigation-state-changed
                             (clj->js {"url" dapp2-url2
                                       "loading" false})
                             @browser
                             false])

         (is (= 1 (:history-index @browser)))
         (is (= [dapp2-url dapp2-url2] (:history @browser)))

         (is (and (browser/can-go-back? @browser)
                  (not (browser/can-go-forward? @browser))))

         (re-frame/dispatch [:browser.ui/previous-page-button-pressed @browser])

         (is (zero? (:history-index @browser)))
         (is (= [dapp2-url dapp2-url2] (:history @browser)))

         (is (and (not (browser/can-go-back? @browser))
                  (browser/can-go-forward? @browser)))

         (re-frame/dispatch [:browser/navigation-state-changed
                             (clj->js {"url" dapp2-url3
                                       "loading" false})
                             @browser
                             false])

         (is (= 1 (:history-index @browser)))
         (is (= [dapp2-url dapp2-url3] (:history @browser)))

         (re-frame/dispatch [:browser.ui/previous-page-button-pressed @browser])

         (is (zero? (:history-index @browser)))
         (is (= [dapp2-url dapp2-url3] (:history @browser)))

         (re-frame/dispatch [:browser.ui/next-page-button-pressed @browser])

         (is (= 1 (:history-index @browser)))
         (is (= [dapp2-url dapp2-url3] (:history @browser))))))

   (let [dapps-permissions (re-frame/subscribe [:get :dapps/permissions])
         dapp-name         "test.com"
         dapp-name2        "test2.org"]

     (testing "dapps permissions"

       (is (zero? (count @dapps-permissions)))

       (re-frame/dispatch [:browser/bridge-message-received (types/clj->json {:type        "status-api-request"
                                                                              :host        dapp-name
                                                                              :permissions ["FAKE_PERMISSION"]})
                           nil nil])

       (re-frame/dispatch [:next-dapp-permission
                           {:dapp-name             dapp-name
                            :index                 0
                            :requested-permissions ["FAKE_PERMISSION"]
                            :permissions-data "Data"}])

       (is (= {:dapp        dapp-name
               :permissions []}
              (get @dapps-permissions dapp-name)))

       (re-frame/dispatch [:browser/bridge-message-received (types/clj->json {:type        "status-api-request"
                                                                              :host        dapp-name
                                                                              :permissions ["CONTACT_CODE"]})
                           nil nil])

       (re-frame/dispatch [:next-dapp-permission
                           {:dapp-name             dapp-name
                            :index                 0
                            :requested-permissions ["CONTACT_CODE"]
                            :permissions-data {"CONTACT_CODE" "Data"}}
                           "CONTACT_CODE"
                           {"CONTACT_CODE" "Data"}])

       (is (= 1 (count @dapps-permissions)))

       (is (= {:dapp        dapp-name
               :permissions ["CONTACT_CODE"]}
              (get @dapps-permissions dapp-name)))

       (re-frame/dispatch [:browser/bridge-message-received (types/clj->json {:type        "status-api-request"
                                                                              :host        dapp-name
                                                                              :permissions ["CONTACT_CODE" "FAKE_PERMISSION"]})
                           nil nil])

       (is (= 1 (count @dapps-permissions)))

       (is (= {:dapp        dapp-name
               :permissions ["CONTACT_CODE"]}
              (get @dapps-permissions dapp-name)))

       (re-frame/dispatch [:browser/bridge-message-received (types/clj->json {:type        "status-api-request"
                                                                              :host        dapp-name
                                                                              :permissions ["FAKE_PERMISSION"]})
                           nil nil])

       (is (= 1 (count @dapps-permissions)))

       (is (= {:dapp        dapp-name
               :permissions ["CONTACT_CODE"]}
              (get @dapps-permissions dapp-name)))

       (re-frame/dispatch [:browser/bridge-message-received (types/clj->json {:type        "status-api-request"
                                                                              :host        dapp-name2
                                                                              :permissions ["CONTACT_CODE"]})
                           nil nil])

       (re-frame/dispatch [:next-dapp-permission
                           {:dapp-name             dapp-name2
                            :index                 0
                            :requested-permissions ["CONTACT_CODE" "FAKE_PERMISSION"]
                            :permissions-data "Data"}])

       (is (= 2 (count @dapps-permissions)))

       (is (= {:dapp        dapp-name2
               :permissions []}
              (get @dapps-permissions dapp-name2)))))))
