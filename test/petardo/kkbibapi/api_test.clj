(ns petardo.kkbibapi.api-test
  (:require [clojure.test :refer :all]
            [petardo.kkbibapi.api :refer [parse-loan-status-page]]
            [clojure.edn]
            [clojure.java.io]))

(deftest parsing
  (is (= [{:expire-date "2021-10-25"
           :title       "Fuld af Fupz"
           :authors     ["Kim Fupz Aakeson" "Siri Melchior"]
           :item-id     "870970-basis:54189338"}]
         (let [input (->> "./test-input-2.edn"
                          clojure.java.io/resource
                          slurp
                          clojure.edn/read-string)]
           (->> input
                parse-loan-status-page
                (map (fn [loan] (update loan
                                        :expire-date
                                        #(.toString %)))))))))
