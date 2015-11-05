(ns dali.schema-test
  (:require [dali.schema :refer :all]
            [clojure.test :refer :all]))

(deftest test-validate
  (is (validate
       [:page {:width 60 :height 60}
        [:circle
         {:stroke :indigo :stroke-width 4 :fill :darkorange}
         [30 30] 20]]))
  (is (validate
       [:page {:width 220 :height 130 :stroke-width 2 :stroke :black :fill :none}
        [:polyline (map #(vector %1 %2) (range 10 210 20) (cycle [10 30]))]
        [:polyline (map #(vector %1 %2) (range 10 210 5) (cycle [60 80]))]
        [:polyline (map #(vector %1 %2) (range 10 210 10) (cycle [100 100 120 120]))]])))
