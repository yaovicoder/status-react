(ns status-im.ui.screens.browser.deceptive-site.views
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [status-im.ui.components.colors :as colors]
            [status-im.ui.components.react :as react]
            [status-im.ui.components.button.view :as button]
            [status-im.ui.screens.browser.deceptive-site.styles :as styles]
            [status-im.i18n :as i18n]))

(defn view [{:keys [can-go-back? site]}]
  (let [more-details-open? (reagent/atom false)]
    (fn []
      [react/scroll-view {:keyboard-should-persist-taps :always
                          :bounces                      false
                          :content-container-style      styles/container}
       [react/view styles/container-root-view
        [react/text {:style styles/title-text}
         (i18n/label :t/browsing-deceptive-site-title)]
        [react/text {:style styles/description-text}
         (i18n/label :t/browsing-deceptive-site-description)]
        [react/view styles/buttons-container
         [button/button {:on-press            (fn []
                                                (if can-go-back?
                                                  (re-frame/dispatch [:browser-nav-back])
                                                  (re-frame/dispatch [:navigate-back])))
                         :text-style          styles/button-text
                         :style               styles/button-container
                         :accessibility-label :back-to-safety-button}
          (i18n/label :t/browsing-back-to-safety)
          nil]
         [button/button {:on-press            (fn []
                                                (swap! more-details-open? not))
                         :text-style          styles/button-text
                         :accessibility-label :more-details-button}
          (if @more-details-open?
            (i18n/label :t/browsing-less-details)
            (i18n/label :t/browsing-more-details))
          nil]]
        (when @more-details-open?
          [react/view styles/more-details-container
           [react/text {:style styles/description-text}
            (i18n/label :t/browsing-phishing-explanation {:site site})]
           [react/view styles/buttons-container
            [button/button {:on-press            #(re-frame/dispatch [:update-browser-options
                                                                      {:ignoring-unsafe? true}])
                            :text-style          styles/button-text
                            :style               styles/button-container
                            :accessibility-label :more-details-button}
             (i18n/label :t/browsing-visit-unsafe-site)
             nil]]])]])))
