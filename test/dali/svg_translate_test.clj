(ns dali.svg-translate-test
  (:require [dali.svg-translate :refer :all]
            [clojure.test :refer :all]))

(deftest line
  (is (= [:line {:y2 200, :x2 100, :y1 20, :x1 10}]
         (to-svg [:line [10 20] [100 200]])))
  (is (= [:line {:y2 200, :x2 100, :y1 20, :x1 10, :stroke "black"}]
         (to-svg [:line {:stroke "black"} [10 20] [100 200]]))))

(deftest circle
  (is (= [:circle {:cx 60, :cy 60, :r 50}]
         (to-svg [:circle [60 60] 50])))
  (is (= [:circle {:cx 60, :cy 60, :r 50, :stroke "black"}]
         (to-svg [:circle {:stroke "black"} [60 60] 50]))))

(deftest ellipse
  (is (= [:ellipse {:ry 25, :rx 50, :cy 60, :cx 60}]
         (to-svg [:ellipse [60 60] 50 25]))))

(deftest rectangle
  (is (= [:rect {:x 10, :y 20, :width 50, :height 60}]
         (to-svg [:rect [10 20] [50 60]])))
  (is (= [:rect {:x 10, :y 20, :width 50, :height 60, :rx 5, :ry 5}]
         (to-svg [:rect [10 20] [50 60] 5])))
  (is (= [:rect {:x 10, :y 20, :width 50, :height 60, :rx 5, :ry 10}]
         (to-svg [:rect [10 20] [50 60] [5 10]]))))

(deftest polyline
  (is (= [:polyline {:points "10,20 30,30 50,70 100,120"}]
         (to-svg [:polyline [10 20] [30 30] [50 70] [100 120]])))
  (is (= [:polyline {:points "10,110 20,120 30,110 40,120 50,110 60,120 70,110 80,120 90,110 100,120 110,110 120,120 130,110 140,120"}]
         (to-svg [:polyline
                  (map #(vector %1 %2) (range 10 150 10) (cycle [110 120]))]))))

(deftest polygon
  (is (= [:polygon {:points "10,20 30,30 50,70 100,120"}]
         (to-svg [:polygon [10 20] [30 30] [50 70] [100 120]])))
  (is (= [:polygon {:points "10,110 20,120 30,110 40,120 50,110 60,120 70,110 80,120 90,110 100,120 110,110 120,120 130,110 140,120"}]
         (to-svg [:polygon
                  (map #(vector %1 %2) (range 10 150 10) (cycle [110 120]))]))))

(deftest path
  (is (= [:path {:d "M 10 20"}]
         (to-svg [:path :M [10 20]])))
  (is (= [:path {:d "M 10 20 l 40 30"}]
         (to-svg [:path :M [10 20] :l [40 30]])))
  (is (= [:path {:d "M 10 20 l 40 30 L 10 10 Z"}]
         (to-svg [:path :M [10 20] :l [40 30] :L [10 10] :Z])))
  (is (= [:path {:d "M 45 10 l 10 10 l -10 10 l -10 -10 z"}]
         (to-svg [:path :M [45 10] :l [10 10] :l [-10 10] :l [-10 -10] :z])))
  (is (= [:path {:d "M 110 80 C 140 10, 165 10, 195 80 S 250 150, 280 80"}]
         (to-svg [:path :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]])))

  ;;long names
  (is (= [:path {:d "M 10 20"}]
         (to-svg [:path :move-to [10 20]])))
  (is (= [:path {:d "M 10 20 l 40 30"}]
         (to-svg [:path :move-to [10 20] :line-by [40 30]])))
  (is (= [:path {:d "M 10 20 l 40 30 L 10 10 Z"}]
         (to-svg [:path :move-to [10 20] :line-by [40 30] :line-to [10 10] :close])))
  (is (= [:path {:d "M 110 80 C 140 10, 165 10, 195 80 S 250 150, 280 80"}]
         (to-svg [:path :move-to [110 80] :cubic-to [140 10] [165 10] [195 80] :symmetrical-to [250 150] [280 80]]))))

(deftest group
  (is (= [:g {}
          [:circle {:cx 60, :cy 60, :r 50}]
          [:circle {:cx 60, :cy 60, :r 50}]]
         (to-svg [:g
                  [:circle [60 60] 50]
                  [:circle [60 60] 50]])))
  (is (= [:g {}
          [:circle {:cx 60, :cy 60, :r 50}]
          [:circle {:cx 60, :cy 60, :r 50}]]
         (to-svg [:g
                  (repeat
                   2
                   [:circle [60 60] 50])]))))
