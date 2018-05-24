(ns status-im.utils.ethereum.tokens
  (:require-macros [status-im.utils.ethereum.macros :refer [resolve-icons]])
  (:require [clojure.string :as string]))

(defn- asset-border [color]
  {:border-color color :border-width 1 :border-radius 32})

(def ethereum {:name     "Ether"
               :symbol   :ETH
               :decimals 18
               :icon     {:source (js/require "./resources/images/assets/ethereum.png")
                          ;; TODO(goranjovic) find a better place to set UI info
                          ;; like colors. Removed the reference to component.styles to
                          ;; avoid circular dependency between namespaces.
                          :style  (asset-border "#628fe333")}})

(defn ethereum? [k]
  (= k (:symbol ethereum)))

;; symbol are used as global identifier (per network) so they must be unique

(def all
  {:mainnet
   (resolve-icons :mainnet
                  [{:symbol   :EOS
                    :name     "EOS"
                    :address  "0x86fa049857e0209aa7d9e616f7eb3b3b78ecfdb0"
                    :decimals 18}
                   {:symbol   :OMG
                    :name     "OmiseGo"
                    :address  "0xd26114cd6EE289AccF82350c8d8487fedB8A0C07"
                    :decimals 18}
                   {:symbol   :PPT
                    :name     "Populous"
                    :address  "0xd4fa1460f537bb9085d22c7bccb5dd450ef28e3a"
                    :decimals 18}
                   {:symbol   :REP
                    :name     "Augur"
                    :address  "0xe94327d07fc17907b4db788e5adf2ed424addff6"
                    :decimals 18}
                   {:symbol   :POWR
                    :name     "PowerLedger"
                    :address  "0x595832f8fc6bf59c85c527fec3740a1b7a361269"
                    :decimals 18}
                   {:symbol   :PAY
                    :name     "TenXPay"
                    :address  "0xB97048628DB6B661D4C2aA833e95Dbe1A905B280"
                    :decimals 18}
                   {:symbol   :VERI
                    :name     "Veros"
                    :address  "0xedbaf3c5100302dcdda53269322f3730b1f0416d"
                    :decimals 18}
                   {:symbol   :GNT
                    :name     "Golem Network Token"
                    :address  "0xa74476443119A942dE498590Fe1f2454d7D4aC0d"
                    :decimals 18}
                   {:symbol   :BNB
                    :name     "BNB"
                    :address  "0xB8c77482e45F1F44dE1745F52C74426C631bDD52"
                    :decimals 18}
                   {:symbol   :BAT
                    :name     "Basic Attention Token"
                    :address  "0x0d8775f648430679a709e98d2b0cb6250d2887ef"
                    :decimals 18}
                   {:symbol   :KNC
                    :name     "Kyber Network Crystal"
                    :address  "0xdd974d5c2e2928dea5f71b9825b8b646686bd200"
                    :decimals 18}
                   {:symbol   :AE
                    :name     "Aeternity"
                    :address  "0x5ca9a71b1d01849c0a95490cc00559717fcf0d1d"
                    :decimals 18}
                   {:symbol   :RDN
                    :name     "Raiden Token"
                    :address  "0x255aa6df07540cb5d3d297f0d0d4d84cb52bc8e6"
                    :decimals 18}
                   {:symbol   :SNT
                    :name     "Status Network Token"
                    :address  "0x744d70fdbe2ba4cf95131626614a1763df805b9e"
                    :decimals 18}
                   {:symbol   :GNO
                    :name     "Gnosis Token"
                    :address  "0x6810e776880c02933d47db1b9fc05908e5386b96"
                    :decimals 18}
                   {:symbol   :ICN
                    :name     "ICONOMI"
                    :address  "0x888666CA69E0f178DED6D75b5726Cee99A87D698"
                    :decimals 18}
                   {:symbol   :WTC
                    :name     "Walton Token"
                    :address  "0xb7cb1c96db6b22b0d3d9536e0108d062bd488f74"
                    :decimals 18}
                   {:symbol   :ZRX
                    :name     "0x Protocol Token"
                    :address  "0xe41d2489571d322189246dafa5ebde1f4699f498"
                    :decimals 18}
                   {:symbol   :BNT
                    :name     "Bancor Network Token"
                    :address  "0x1f573d6fb3f13d689ff844b4ce37794d79a7ff1c"
                    :decimals 18}
                   {:symbol   :PPP
                    :name     "PayPie"
                    :address  "0xc42209aCcC14029c1012fB5680D95fBd6036E2a0"
                    :decimals 18}
                   {:symbol   :LINK
                    :name     "ChainLink Token"
                    :address  "0x514910771af9ca656af840dff83e8264ecf986ca"
                    :decimals 18}
                   {:symbol   :KIN
                    :name     "Kin"
                    :address  "0x818fc6c2ec5986bc6e2cbf00939d90556ab12ce5"
                    :decimals 18}
                   {:symbol   :ANT
                    :name     "Aragon Network Token"
                    :address  "0x960b236A07cf122663c4303350609A66A7B288C0"
                    :decimals 18}
                   {:symbol   :LRC
                    :name     "LoopringCoin"
                    :address  "0xEF68e7C694F40c8202821eDF525dE3782458639f"
                    :decimals 18}
                   {:symbol   :ZSC
                    :name     "Zeus Shield Token"
                    :address  "0x7A41e0517a5ecA4FdbC7FbebA4D4c47B9fF6DC63"
                    :decimals 18}
                   {:symbol   :DATA
                    :name     "Streamr DATAcoin"
                    :address  "0x0cf0ee63788a0849fe5297f3407f701e122cc023"
                    :decimals 18}
                   {:symbol   :WINGS
                    :name     "WINGS"
                    :address  "0x667088b212ce3d06a1b553a7221E1fD19000d9aF"
                    :decimals 18}
                   {:symbol   :MLN
                    :name     "Melon Token"
                    :address  "0xBEB9eF514a379B997e0798FDcC901Ee474B6D9A1"
                    :decimals 18}
                   {:symbol   :MDA
                    :name     "Moeda Loyalty Points"
                    :address  "0x51db5ad35c671a87207d88fc11d593ac0c8415bd"
                    :decimals 18}
                   {:symbol   :PLR
                    :name     "PILLAR"
                    :address  "0xe3818504c1b32bf1557b16c238b2e01fd3149c17"
                    :decimals 18}
                   {:symbol   :Centra
                    :name     "Centra Token"
                    :address  "0x96A65609a7B84E8842732DEB08f56C3E21aC6f8a"
                    :decimals 18}
                   {:symbol   :SAN
                    :name     "SANtiment network token"
                    :address  "0x7c5a0ce9267ed19b22f8cae653f198e3e8daf098"
                    :decimals 18}
                   {:symbol   :SNM
                    :name     "SONM Token"
                    :address  "0x983f6d60db79ea8ca4eb9968c6aff8cfa04b3c63"
                    :decimals 18}
                   {:symbol   :REQ
                    :name     "Request Token"
                    :address  "0x8f8221afbb33998d8584a2b05749ba73c37a938a"
                    :decimals 18}
                   {:symbol   :MANA
                    :name     "Decentraland MANA"
                    :address  "0x0f5d2fb29fb7d3cfee444a200298f468908cc942"
                    :decimals 18}
                   {:symbol   :1ST
                    :name     "FirstBlood Token"
                    :address  "0xaf30d2a7e90d7dc361c8c4585e9bb7d2f6f15bc7"
                    :decimals 18}
                   {:symbol   :AMB
                    :name     "Amber Token"
                    :address  "0x4dc3643dbc642b72c158e7f3d2ff232df61cb6ce"
                    :decimals 18}
                   {:symbol   :XPA
                    :name     "XPlay Token"
                    :address  "0x90528aeb3a2b736b780fd1b6c478bb7e1d643170"
                    :decimals 18}
                   {:symbol   :OTN
                    :name     "Open Trading Network"
                    :address  "0x881ef48211982d01e2cb7092c915e647cd40d85c"
                    :decimals 18}
                   {:symbol   :DNT
                    :name     "district0x Network Token"
                    :address  "0x0abdace70d3790235af448c88547603b945604ea"
                    :decimals 18}
                   {:symbol   :EDO
                    :name     "Eidoo Token"
                    :address  "0xced4e93198734ddaff8492d525bd258d49eb388e"
                    :decimals 18}
                   {:symbol   :COB
                    :name     "Cobinhood Token"
                    :address  "0xb2f7eb1f2c37645be61d73953035360e768d81e6"
                    :decimals 18}
                   {:symbol   :ENJ
                    :name     "Enjin Coin"
                    :address  "0xf629cbd94d3791c9250152bd8dfbdf380e2a3b9c"
                    :decimals 18}
                   {:symbol   :AVT
                    :name     "AVT"
                    :address  "0x0d88ed6e74bbfd96b831231638b66c05571e824f"
                    :decimals 18}
                   {:symbol   :TIME
                    :name     "TIME"
                    :address  "0x6531f133e6deebe7f2dce5a0441aa7ef330b4e53"
                    :decimals 18}
                   {:symbol   :CND
                    :name     "Cindicator Token"
                    :address  "0xd4c435f5b09f855c3317c8524cb1f586e42795fa"
                    :decimals 18}
                   {:symbol   :STX
                    :name     "STOX"
                    :address  "0x006BeA43Baa3f7A6f765F14f10A1a1b08334EF45"
                    :decimals 18}
                   {:symbol   :VIB
                    :name     "Vibe"
                    :address  "0x2c974b2d0ba1716e644c1fc59982a89ddd2ff724"
                    :decimals 18}
                   {:symbol   :DPY
                    :name     "Delphy"
                    :address  "0x6c2adc2073994fb2ccc5032cc2906fa221e9b391"
                    :decimals 18}
                   {:symbol   :CDT
                    :name     "CoinDash Token"
                    :address  "0x2fe6ab85ebbf7776fee46d191ee4cea322cecf51"
                    :decimals 18}])
   :testnet
   (resolve-icons :testnet
                  [{:name     "Status Test Token"
                    :symbol   :STT
                    :decimals 18
                    :address  "0xc55cf4b03948d7ebc8b9e8bad92643703811d162"}
                   {:name     "Modest Test Token"
                    :symbol   :MDS
                    :decimals 18
                    :address  "0x57cc9b83730e6d22b224e9dc3e370967b44a2de0"}])})

(defn tokens-for [chain]
  (get all chain))

(defn sorted-tokens-for [chain]
  (->> (tokens-for chain)
       (sort #(compare (string/lower-case (:name %1))
                       (string/lower-case (:name %2))))))

(defn symbol->token [chain symbol]
  (some #(when (= symbol (:symbol %)) %) (tokens-for chain)))

(defn address->token [chain address]
  (some #(when (= address (:address %)) %) (tokens-for chain)))

(defn asset-for [chain symbol]
  (if (= (:symbol ethereum) symbol)
    ethereum
    (symbol->token chain symbol)))
