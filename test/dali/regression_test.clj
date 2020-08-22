(ns dali.regression-test
  (:require [clojure.java.io :as jio]
            [clojure.test :refer :all]
            [dali.examples :as examples]
            [dali.io :as io]
            [hiccup.core :refer [html]]
            [dali.layout]
            [dali.layout.align]
            [dali.layout.stack]
            [dali.layout.stack]
            [dali.layout.distribute]
            [dali.layout.surround]
            [dali.layout.connect]
            [dali.layout.matrix]
            [dali.layout.place]))

(def out-dir "examples/output/")
(def fixtures-dir "examples/fixtures/")

(defn- example-name->svg-filename [s]
  (-> s :filename (str ".svg")))

(deftest fixtures-rendered
  (testing "all the fixtures are rendered"
   (let [rendered-fixtures (->> fixtures-dir jio/file .listFiles seq (map #(.getName %)) set)]
     (is (= (->> examples/examples (map example-name->svg-filename) set)
            rendered-fixtures)))))

(defn- render-example-comparison-chart []
  (spit "examples/output/comparison-chart.html"
        (html
         [:html
          [:head
           [:style
            (str "table {"
                 "  border-collapse: collapse;"
                 "}"
                 "table, th, td {"
                 "  border: 1px solid lightgrey;"
                 "}")]]
          [:body
           [:h1 "dali regression testing comparsion chart"]
           [:table
            [:tr
             [:td [:h2 "Name"]]
             [:td [:h2 "Actual"]]
             [:td [:h2 "Expected"]]]
            (for [{:keys [filename]} examples/examples]
              [:tr
               [:td filename]
               [:td [:img {:src (str filename ".svg")}]]
               [:td [:img {:src (str "../fixtures/" filename ".svg")}]]])]]])))

(deftest compare-examples
  (examples/render-examples "examples/output/" examples/examples)
  (render-example-comparison-chart)
  (doseq [f (->> examples/examples (map example-name->svg-filename))]
    (let [example-filename (str out-dir f)
          fixture-filename (str fixtures-dir f)]
      (testing (str "example " f " is identical to fixture")
        (is (= (io/load-enlive-svg fixture-filename)
               (io/load-enlive-svg example-filename)))))))

;; To refresh fixtures:

;;(examples/render-examples "examples/fixtures/" examples/examples)
