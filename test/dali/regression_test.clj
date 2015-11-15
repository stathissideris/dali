(ns dali.regression-test
  (:require [clojure.java.io :as jio]
            [clojure.test :refer :all]
            [dali.examples :as examples]
            [dali.io :as io]
            [dev]))

(def out-dir "examples/output/")
(def fixtures-dir "examples/fixtures/")

(deftest fixtures-rendered
  (testing "all the fixtures are rendered"
   (let [rendered-fixtures (->> fixtures-dir jio/file .listFiles seq (map #(.getName %)) set)]
     (is (= (->> examples/examples (map :filename) set)
            rendered-fixtures)))))

(deftest compare-examples
  (dev/render-examples)
  (doseq [f (->> examples/examples (map :filename))]
    (let [example-filename (str out-dir f)
          fixture-filename (str fixtures-dir f)]
      (testing (str "example " f " is identical to fixture")
        (is (= (io/load-enlive-svg fixture-filename)
               (io/load-enlive-svg example-filename)))))))
