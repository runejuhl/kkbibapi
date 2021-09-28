(ns petardo.kkbibapi.core
  (:gen-class)
  (:require [petardo.kkbibapi.server :as server]
            [clojure.edn]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn -main
  "I don't do a whole lot ... yet."
  [config-file]
  (let [{:keys [users port]} (clojure.edn/read-string (slurp config-file))]
    (run-jetty (server/create-handler users)
               {:port port
                :join? false}))
  ;; (let [configs (clojure.edn/read-string (slurp config-file))
  ;;       loans   (->> configs
  ;;                    (map get-loans))]
  ;;   (api/write-loan-metrics loans))
  )
