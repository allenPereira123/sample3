(ns sample3.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [sample3.ajax :as ajax]
    [sample3.events]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))

; Comment block on

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])



(defn input-field [key form-data]
  [:div.field
   [:label.label "Operand"]
   [:div.control
    [:input.input.is-primary
     {:style {:margin-bottom "15px"}
      :type "number"
      :value (key @form-data)
      :on-change #(rf/dispatch [:update-form key (js/parseInt (-> % .-target .-value))])}]]])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "sample3"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]
               [:div#nav-menu.navbar-menu
                {:class (when @expanded? :is-active)}
                [:div.navbar-start
                 [nav-link "#/" "Home" :home]
                 [nav-link "#/about" "About" :about]]]]))



(defn button [[symbol url]]
  [:button.button.is-primary {:on-click (fn [e]
                                          (let [{:keys [x y]} @(rf/subscribe [:form])]
                                            (if (or (js/isNaN x) (js/isNaN y))
                                              (js/alert "Invalid Operand/s")
                                              (rf/dispatch [:request (str "http://localhost:3000/api/math/" url) symbol]))))}
   symbol]
  )
(defn operations []
  [:div
   [:label.label "Operators"]
   [:div.buttons
    (for [operator @(rf/subscribe [:operations])]
      ^{:key operator}[button operator])]])

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])


(def color-map {:red "#FF0000" :green "#90EE90" :blue "#ADD8E6" :orange "#FFA07A"})
(defn setColor [total]
  (if (< total 0 ) (:red color-map)
                   (if (<= total 19) (:green color-map) (if (<= total 49) (:blue color-map) (:orange color-map)))))
(defn list-item [{:keys [x y operator total]}]
  [:li {:style {:list-style-type "circle" :color (setColor total)}} x  " " operator " " y " = " (str total)])
(defn equation []
  [:div {:style {:margin-top "10px"}}
   [:label.label "Equations"]
   [:ul {:style {:margin-left "15px"}}
    (for [item @(rf/subscribe [:equations])]
      [list-item item])]])

(defn home-page []
    (let [form (rf/subscribe [:form])]
      (fn []
        [:div {:style {:margin "50px" :width "350px"}}
        [:div.box
        [input-field :x form]
        [input-field :y form]
        [operations]
        [equation]]])))

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app"))
  )

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch [:initialize-db])
  (mount-components)
  )
