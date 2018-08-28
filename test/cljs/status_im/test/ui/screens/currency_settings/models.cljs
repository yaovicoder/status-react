(ns status-im.test.ui.screens.currency-settings.models
  (:require [cljs.test :refer-macros [deftest is testing]]
            [status-im.ui.screens.currency-settings.models :as models]))

(deftest get-currency
  (is (= :usd (models/get-currency {:account/account {:settings {:wallet {:currency :usd}}}})))
  (is (= :usd (models/get-currency {:account/account {:settings {:wallet {:currency nil}}}})))
  (is (= :usd (models/get-currency {:account/account {:settings {:wallet {}}}})))
  (is (= :aud (models/get-currency {:account/account {:settings {:wallet {:currency :aud}}}}))))

(deftest set-currency
  (let [cofx (models/set-currency :usd {:db {:account/account {:settings {:wallet {}}}}})]
    (is (= [:db :get-balance :get-tokens-balance :get-prices :data-store/base-tx] (keys cofx)))
    (is (= :usd (get-in cofx [:db :account/account :settings :wallet :currency]))))
  (is (= :jpy (get-in (models/set-currency :jpy {:db {:account/account {:settings {:wallet {}}}}})
                      [:db :account/account :settings :wallet :currency]))))
