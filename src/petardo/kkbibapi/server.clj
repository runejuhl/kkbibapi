(ns petardo.kkbibapi.server
  (:require [ring.adapter.jetty :require [run-jetty]]
            [petardo.kkbibapi.api :as api]))

(defn create-handler [configs]
  (fn [request]
    (println "got request from" (:remote-addr request) "for" (:uri request))
    (let [uri (:uri request)]
      (cond
        (= uri "/metrics")
        {:status  200
         :headers {"Content-Type" "text/ascii"}
         :body    (str
                   "# HELP petardo_kkbibapi_loans_days_left Number of loans at the public libraries in the City of Copenhagen\n"
                   "# TYPE petardo_kkbibapi_loans_days_left gauge\n"
                   (apply str (->> configs
                                   (map api/get-loans)
                                   (map api/write-loan-metrics))))}
        :else
        {:status  404
         :headers {"Content-Type" "text/html"}
         :body    "Nothing to see here, please move along."}))))
