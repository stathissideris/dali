(ns dali.geom-test
  (:require [dali.geom :refer :all]
            [clojure.test :refer :all]))

(deftest test-slope
  (is (= 1    (slope [0 0] [30 30])))
  (is (= 1    (slope [30 30] [0 0])))
  (is (= -1.0 (float (slope [0 30] [30 0]))))
  (is (= -1.0 (float (slope [30 0] [0 30]))))
  (is (= 3/2  (slope [5 5] [15 20]))))

(deftest test-angle
  (is (=  -45.0 (angle [5 5] [10 10])))
  (is (=   45.0 (angle [5 10] [10 5])))
  (is (=  -90.0 (angle [0 0] [0 5])))
  (is (=    0.0 (angle [0 0] [5 0])))
  (is (= -180.0 (angle [0 0] [-10 0])))
  (is (=   90.0 (angle [0 0] [0 -10]))))
