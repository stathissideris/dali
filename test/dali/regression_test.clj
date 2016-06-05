(ns dali.regression-test
  (:require [clojure.java.io :as jio]
            [clojure.test :refer :all]
            [dali.examples :as examples]
            [dali.io :as io]
            [dali.layout
             [stack
              distribute
              align
              surround
              connect
              matrix
              place]]))

(def out-dir "examples/output/")
(def fixtures-dir "examples/fixtures/")

(defn- example-name->svg-filename [s]
  (-> s :filename (str ".svg")))

(deftest fixtures-rendered
  (testing "all the fixtures are rendered"
   (let [rendered-fixtures (->> fixtures-dir jio/file .listFiles seq (map #(.getName %)) set)]
     (is (= (->> examples/examples (map example-name->svg-filename) set)
            rendered-fixtures)))))

(deftest compare-examples
  (examples/render-examples "examples/output/" examples/examples)
  (doseq [f (->> examples/examples (map example-name->svg-filename))]
    (let [example-filename (str out-dir f)
          fixture-filename (str fixtures-dir f)]
      (testing (str "example " f " is identical to fixture")
        (is (= (io/load-enlive-svg fixture-filename)
               (io/load-enlive-svg example-filename)))))))
