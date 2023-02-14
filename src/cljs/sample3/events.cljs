(ns sample3.events
  (:require
    [re-frame.core :as rf]
    [ajax.core :as ajax]
    [reitit.frontend.easy :as rfe]
    [reitit.frontend.controllers :as rfc]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]))

;;dispatchers

(rf/reg-event-db
  :initialize-db
  (fn-traced [db _]
    (-> db
        (assoc :equations [])
        (assoc :operations [["+" "plus"] ["-" "minus"] ["*" "mult"] ["/" "div"]])
        (assoc :form {})
        )))

(rf/reg-event-db
  :update-form
  (fn-traced [db [_ key val]]
    (assoc-in db [:form key] val)))
(rf/reg-event-db
  :common/navigate
  (fn-traced [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-event-db
  :process-response
  (fn-traced [db [_ symbol response]]
    (assoc-in db [:equations] (conj (:equations db) (conj (:form db) response {:operator symbol})))))

(rf/reg-event-fx
  :bad-response
  (fn-traced [_ _]
    {:invalid-operands nil}))
(rf/reg-event-fx
  :request
  (fn-traced [{:keys [db]} [_ url symbol]]
    {:http-xhrio {:method          :get
                  :uri             url
                  :params          {:x (get-in db [:form :x]) :y (get-in db [:form :y])}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:process-response symbol]
                  :on-failure      [:bad-response]}}
    ))

(rf/reg-fx
  :invalid-operands
  (fn [_]
    (js/alert "invalid operands")))
(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
  :set-docs
  (fn-traced [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success       [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn-traced [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-event-fx
  :page/init-home
  (fn-traced [_ _]
    {:dispatch [:fetch-docs]}))

;;subscriptions

(rf/reg-sub
  :equations
  (fn [db _]
    (:equations db)))
(rf/reg-sub
  :form
  (fn [db _]
    (:form db)))
(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :operations
  (fn [db _]
    (:operations db)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))
