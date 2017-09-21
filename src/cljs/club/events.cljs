(ns club.events
  (:require
    [clojure.string :as str]
    [clojure.set :refer [difference]]
    [clojure.walk :refer [keywordize-keys]]
    [re-frame.core :as rf]
    [re-frame.db :refer [app-db]]
    [goog.object :refer [getValueByKeys]]
    [club.db]
    [club.db :refer [base-user-record
                     new-series
                     logout-db-fragment
                     set-auth-data!
                     fetch-teachers-list!
                     delete-series!]]
    [club.utils :refer [error
                        get-prop
                        data-from-js-obj
                        parse-url
                        get-url-all!
                        get-url-root!]]
    [cljs.spec     :as s]
    [goog.crypt.base64 :refer [decodeString]]))


;; Interceptors

(defn check-and-throw
  "Throw an exception if db doesn't match the spec"
  [a-spec db]
  (let [valid (s/valid? a-spec db)]
  (when-not valid
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {})))
  valid))

(def check-spec-interceptor (rf/after (partial check-and-throw :club.db/db)))


;; Event Handlers

(rf/reg-event-fx
  :login
  (fn []
    {:login nil}))

(def webauth
  (let [auth0 (getValueByKeys js/window "deps" "auth0")
        opts (clj->js {:domain "clubexpr.eu.auth0.com"
                       :clientID "QKq48jNZqQ84VQALSZEABkuUnM74uHUa"
                       :responseType "token id_token"
                       :redirectUri (get-url-root!)
                       })]
    (new auth0.WebAuth opts)))

(rf/reg-fx
  :login
  (fn [_]
    (.authorize webauth)))

(rf/reg-event-fx
  :logout
  (fn [{:keys [db]} _]
    {:db (merge db logout-db-fragment)
     :clean-url nil}))

(rf/reg-fx
  :clean-url
  (fn []
    (set! (-> js/window .-location .-hash) "")))

(rf/reg-event-fx
  :profile-cancel
  (fn []
    {:profile-cancel nil}))

(rf/reg-fx
  :profile-cancel
  (fn [_]
    (club.db/fetch-profile-data!)
    ; TODO useless use of set-auth-data! : these 4 already set
    ;(swap! app-db assoc-in [:authenticated] true)
    ;; from new-user-data
    ;(swap! app-db assoc-in [:auth-data :auth0-id] auth0-id)
    ;(swap! app-db assoc-in [:auth-data :access-token] access-token)
    ;(swap! app-db assoc-in [:auth-data :expires-at] expires-at)
    ))

(rf/reg-event-fx
  :profile-save
  (fn []
    {:profile-save nil}))

(rf/reg-fx
  :profile-save
  (fn [_]
    (club.db/save-profile-data!)))

(rf/reg-event-db
  :profile-save-ok
  [check-spec-interceptor]
  (fn [db [_ _]]
    ; TODO: set a flag in the state to display «new profile saved»
    db
    ))

(rf/reg-event-fx
  :groups-cancel
  (fn []
    {:groups-cancel nil}))

(rf/reg-fx
  :groups-cancel
  (fn [_]
    (club.db/fetch-groups-data!)))

(rf/reg-event-fx
  :groups-save
  (fn []
    {:groups-save nil}))

(rf/reg-fx
  :groups-save
  (fn [_]
    (club.db/save-groups-data!)))

(rf/reg-event-db
  :groups-save-ok
  [check-spec-interceptor]
  (fn [db [_ _]]
    ; TODO: set a flag in the state to display «new groups saved»
    db
    ))

(rf/reg-event-db
  :user-code-club-src-change
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc db :attempt-code new-value)))

(rf/reg-event-db
  :profile-quality
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:profile-page :quality] new-value)))

(rf/reg-event-fx
  :profile-school
  [check-spec-interceptor]
  (fn [{:keys [db]} [_ new-value]]
    {:db (assoc-in db [:profile-page :school] new-value)
     :profile-load-teachers-list new-value}))

(rf/reg-fx
  :profile-load-teachers-list
  (fn [school-id]
    (fetch-teachers-list! school-id)))

(rf/reg-event-db
  :profile-teacher
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:profile-page :teacher] new-value)))

(rf/reg-event-db
  :profile-lastname
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:profile-page :lastname] new-value)))

(rf/reg-event-db
  :profile-firstname
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:profile-page :firstname] new-value)))

(rf/reg-event-db
  :initialize-db
  [check-spec-interceptor]
  (fn  [_ _]
    club.db/default-db))

(rf/reg-event-fx
  :nav
  (fn [{:keys [db]} _]
    (let [parsed-url (parse-url (get-url-all!))
          page (:page parsed-url)
          query-params (:query-params parsed-url)
          ; 'empty?' prevents wrecking the state at loading time
          new-db (if (empty? db) db (assoc db :current-page page))
          cofx (if (empty? query-params)
                 {:db new-db}
                 {:db new-db :auth query-params :clean-url nil})]
       cofx)))

(defn process-user-check!
  [result new-user-data]
  (let [new-auth0-id (:auth0-id new-user-data)
        user-with-same-auth0-id (->> result
                                     data-from-js-obj
                                     (filter #(= new-auth0-id (:auth0-id %)))
                                     first)]
    (if (nil? user-with-same-auth0-id)
      (.. club.db/k-users
          (createRecord (clj->js (base-user-record new-auth0-id)))
          (then #(set-auth-data! (merge new-user-data (data-from-js-obj %))))
          (catch (error "events/process-user-check!")))
      (set-auth-data! (merge new-user-data user-with-same-auth0-id)))))

(rf/reg-fx
  :auth
  (fn [{:keys [access_token expires_in id_token]}]  ; we left: token_type state
    (let [expires-in-num (js/parseInt expires_in)
          expires-at (str (+ (* expires-in-num 1000) (.getTime (new js/Date))))
          decoded-json (-> id_token
                          (str/split ".")
                          second
                          decodeString)
          decoded-js (try (.parse js/JSON decoded-json)
                          (catch js/Object e (println e)
                                             (println id_token)
                                             (println decoded-json)
                                             js/Object)) ; default: empty obj
          auth0-id (getValueByKeys decoded-js "sub")
          new-user-data {:auth0-id auth0-id
                         :access-token access_token
                         :expires-at expires-at}]
      (if (not (nil? auth0-id))
        (.. club.db/k-users
            (listRecords)
            (then #(process-user-check! % new-user-data))
            (catch (error "events/:auth"))))
    )))

(rf/reg-event-db
  :write-teachers-list
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:profile-page :teachers-list] new-value)))

; :init-groups is a merge of old and new data, like :write-groups,
; but existing scholar ids remain untouched (merge new old)
(rf/reg-event-db
  :init-groups
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [old-groups (:groups-page @app-db)
          new-groups (merge new-value old-groups)]
      (assoc-in db [:groups-page] new-groups))))

; :write-groups is a merge of old and new data, like :init-groups,
; but existing scholar ids are replaced (merge old new)
(rf/reg-event-db
  :write-groups
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [old-groups (:groups-page @app-db)
          new-groups (merge old-groups new-value)]
      (assoc-in db [:groups-page] new-groups))))

(rf/reg-event-db
  :groups-change
  [check-spec-interceptor]
  (fn [db [_ [scholar-id value]]]
    (let [clj-val (-> value js->clj keywordize-keys)
          groups (set (map :value clj-val))]
      (assoc-in db [:groups-page scholar-id :groups] groups))))

(rf/reg-event-db
  :current-series-id
  [check-spec-interceptor]
  (fn [db [_ new-series-id]]
    (let [current-series (->> db :series-page
                                 (filter #(= new-series-id (:id %)))
                                 first
                                 :series)]
      (-> db (assoc-in [:current-series-id] new-series-id)
             (assoc-in [:current-series] current-series)))))

(rf/reg-event-db
  :write-series
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:series-page] new-value)))

(rf/reg-event-db
  :series-filtering-nature
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [nature (-> new-value js->clj keywordize-keys :value)
          new-db (assoc-in db [:series-filtering :nature] nature)
          new-filter (fn [expr] (= (get-prop expr "nature") nature))]
      (if (= "All" nature)
        (update-in new-db [:series-filtering :filters] dissoc :nature)
        (assoc-in new-db [:series-filtering :filters :nature] new-filter)))))

(rf/reg-event-db
  :series-filtering-depth
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [depth-range (js->clj new-value)
          [m M] depth-range
          new-db (assoc-in db [:series-filtering :depth] depth-range)
          new-filter (fn [expr] (<= m (get-prop expr "depth") M))]
      (assoc-in new-db [:series-filtering :filters :depth] new-filter))))

(rf/reg-event-db
  :series-filtering-nb-ops
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [nb-ops-range (js->clj new-value)
          [m M] nb-ops-range
          new-db (assoc-in db [:series-filtering :nb-ops] nb-ops-range)
          new-filter (fn [expr] (<= m (get-prop expr "nbOps") M))]
      (assoc-in new-db [:series-filtering :filters :nb-ops] new-filter))))

(rf/reg-event-db
  :series-filtering-prevented-ops
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [p-ops (js->clj new-value)
          new-db (assoc-in db [:series-filtering :prevented-ops] p-ops)
          new-filter
            (apply every-pred
              (map #(fn [expr] (not (some #{%} (get-prop expr "uniqueOps"))))
                   p-ops))]
      (if (empty? p-ops)
        (update-in new-db [:series-filtering :filters] dissoc :prevented-ops)
        (assoc-in new-db [:series-filtering :filters :prevented-ops] new-filter)))))

(rf/reg-event-db
  :new-series
  [check-spec-interceptor]
  (fn [db]
    (-> db
        (assoc-in [:editing-series] true)
        (assoc-in [:current-series-id] "")
        (assoc-in [:current-series] new-series))))

(rf/reg-event-db
  :series-cancel
  [check-spec-interceptor]
  (fn [db]
    (if (empty? (:current-series-id db))
      ; abort creating a new series
      (-> db
          (assoc-in [:editing-series] false)
          (assoc-in [:current-series-id] "")
          (assoc-in [:current-series] new-series))
      ; abort editing an existing series
      (assoc db :editing-series false))))

(rf/reg-event-fx
  :series-save
  [check-spec-interceptor]
  (fn [{:keys [db]}]
    {:series-save nil}))

(rf/reg-fx
  :series-save
  (fn [_]
    (club.db/save-series-data!)))

(rf/reg-event-db
  :series-edit
  [check-spec-interceptor]
  (fn [db]
    (assoc db :editing-series true)))

(rf/reg-event-fx
  :series-delete
  [check-spec-interceptor]
  (fn [{:keys [db]}]
    {:series-delete nil}))

(rf/reg-fx
  :series-delete
  (fn []
    (delete-series!)))

(rf/reg-event-db
  :series-title
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:current-series :title] new-value)))

(rf/reg-event-db
  :series-desc
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (assoc-in db [:current-series :desc] new-value)))

(defn sorted-expr->db-expr
  [expr-data]
  (let [lisp (-> expr-data (getValueByKeys "content" "props" "src"))
        rank (-> expr-data (getValueByKeys "rank"))]
    {:content lisp :rank rank}))

(rf/reg-event-db
  :series-exprs-sort
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [exprs (vec (map sorted-expr->db-expr new-value))]
      (assoc-in db [:current-series :exprs] exprs))))

(rf/reg-event-db
  :series-exprs-add
  [check-spec-interceptor]
  (fn [db [_ new-value]]
    (let [rank (count (-> db :current-series :exprs))]
      (update-in db [:current-series :exprs] conj {:content new-value :rank rank}))))

(rf/reg-event-db
  :series-exprs-delete
  [check-spec-interceptor]
  (fn [db [_ deleted-rank]]
    (let [remove-elt #(vec (concat (subvec % 0 deleted-rank )
                                   (subvec % (+ deleted-rank 1))))
          fix-ranks #(vec (map (fn [{:keys [content rank]}]
                                 {:content content
                                  :rank (if (> rank deleted-rank)
                                          (- rank 1)
                                          rank)})
                               %))]
      (-> db (update-in [:current-series :exprs] remove-elt)
             (update-in [:current-series :exprs] fix-ranks)))))

(rf/reg-event-db
  :series-save-ok
  [check-spec-interceptor]
  (fn [db [_ record]]
    ; TODO: set a flag in the state to display «new series saved»
    (-> db
        (assoc-in [:current-series-id] (-> record data-from-js-obj :id)))))

(rf/reg-event-db
  :series-delete-ok
  [check-spec-interceptor]
  (fn [db [_ record]]
    ; TODO: set a flag in the state to display «new series saved»
    (-> db
        (assoc-in [:current-series-id] "")
        (assoc-in [:current-series] new-series))))
