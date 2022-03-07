(ns petardo.kkbibapi.api
  (:require [hickory.core :as h]
            [hickory.select :as s]
            [clj-http.client :as client]
            [clojure.string :as string]
            [clj-http.core]
            [clj-http.cookies]
            [clojure.edn]))

(def months-danish
  "List of months names in Danish for date parsing."
  '("januar" "februar" "marts" "april" "maj" "juni" "july" "august"
             "september" "oktober" "november" "december"))

(defn parse-danish-date
  "Convert a tuple of day, month and year to a proper date.

  Example input: `[\"02\" \"oktober\" \"2011\"]`."
  [[day month year]]
  (java.time.LocalDate/of
   (Integer. year)
   (inc (.indexOf months-danish month))
   (Integer. day)))

(defn login
  [cookie-store {:keys [cpr pin]}]
  (binding [clj-http.core/*cookie-store* cookie-store]
    ;; issue a first request to get a cookie
    (client/get "https://bibliotek.kk.dk/adgangsplatformen/login?destination=ding_frontpage%3Fmessage%3Dlogout")
    ;; get a token we can use for authentication
    (let [token
          (->> (client/get "https://bibliotek.kk.dk/adgangsplatformen/login?destination=ding_frontpage")
               :body
               (re-find #"/login/identityProviderCallback/borchk/([a-f0-9]+)")
               (second))
          borchk-url (format "https://login.bib.dk/login/identityProviderCallback/borchk/%s" token)]
      ;; Do the actual login. We need `:redirect-strategy :lax` as the POST
      ;; request results in a 302 redirect.
      (client/post borchk-url {;; :save-request? true :debug-body true
                               :form-params       {:agency                710100
                                                   :libraryName           "København"
                                                   :autocomplete-username nil
                                                   :userId                cpr
                                                   :pincode               pin}
                               :max-redirects     5
                               :redirect-strategy :lax}))))

(defn get-loan-page
  [cookie-store]
  (binding [clj-http.core/*cookie-store* cookie-store]
    ;; Retrive loans page and parse it with hickory.
    (->> (client/get "https://bibliotek.kk.dk/user/me/status-loans")
         :body
         h/parse
         h/as-hickory)))

(defn parse-loan-status-page
  [input]
  (some->> input
           (s/select
            (s/descendant
             (s/class :pane-loans)
             (s/class :pane-content)
             (s/class :material-item)))

           (map (fn [item]
                  {:item-id (->> item
                                 (s/select
                                  (s/descendant
                                   (s/class :ting-cover-processed)))
                                 first
                                 :attrs
                                 :data-ting-cover-object-id)
                   :expire-date (-> item
                                    (->> (s/select
                                          (s/descendant
                                           (s/class :expire-date)
                                           (s/class :item-information-data))))
                                    first
                                    :content
                                    first
                                    (clojure.string/split #"\.| ")
                                    (->> (remove empty?)
                                         parse-danish-date))
                   :title       (-> item
                                    (->> (s/select
                                          (s/descendant
                                           (s/class :item-title)
                                           (s/tag :a))))
                                    first
                                    :content
                                    first)
                   :authors     (some-> item
                                        (->> (s/select
                                              (s/descendant
                                               (s/class :item-creators))))

                                        first
                                        :content
                                        first
                                        (clojure.string/split #", "))}))))

(defn write-loan-metrics
  [{:keys [user loans] :as _data}]
  (let [current-date (java.time.LocalDate/now)
        epoch-milli  (.toEpochMilli (.toInstant (.atZone (java.time.LocalDateTime/now) (java.time.ZoneId/systemDefault))))
        days-left-fn (fn [expire-date]
                       (.until current-date
                               expire-date
                               java.time.temporal.ChronoUnit/DAYS))]
    (clojure.string/join
     ""
     (->> loans
          (map (fn [{:keys [item-id expire-date title _authors]}]
                 ;; http_requests_total{method="post",code="200"} 1027 1395066363000
                 ;; http_requests_total{method="post",code="400"}    3 1395066363000
                 (format "petardo_kkbibapi_loans_days_left{user=\"%s\", item_id=\"%s\", item_title=\"%s\"} %d %d\n"
                         user
                         item-id
                         title
                         (days-left-fn expire-date)
                         epoch-milli)))))))

(defn get-loans
  [{:keys [user] :as config}]
  (let [c (clj-http.cookies/cookie-store)]
    (login c config)
    {:user  user
     :loans (->> c
                 get-loan-page
                 parse-loan-status-page)}))

(comment
  (let [c (clj-http.cookies/cookie-store)]
    (login c {:cpr "000000000"
              :pin "0000"})
    (->> c
         get-loan-page
         parse-loan-status-page)))
