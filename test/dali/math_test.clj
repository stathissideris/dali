(ns dali.math-test
  (:require [dali.math :refer :all]
            [clojure.test :refer :all]))

(defmacro fix [x]
  `(is (= ~(eval x) ~x)))

(deftest test-polar-angle
  (is (= 45.0 (polar-angle [5 5])))
  (is (= -45.0 (polar-angle [5 -5])))
  (is (= -90.0 (polar-angle [0 -5])))
  (is (= 180.0 (polar-angle [-5 0])))
  (is (= -135.0 (polar-angle [-5 -5])))
  (is (= 135.0 (polar-angle [-5 5]))))

(defn near [p1 p2]
  (every? #(> 0.001 (abs %)) (map - p1 p2)))

(deftest test-polar-to-cartesian
  (is (near [20 20] (-> [20 20] cartesian->polar polar->cartesian)))
  (is (near [10 20] (-> [10 20] cartesian->polar polar->cartesian)))
  (is (near [-2 25] (-> [-2 25] cartesian->polar polar->cartesian)))
  (is (near [100 2] (-> [100 2] cartesian->polar polar->cartesian)))
  (is (near [0 0]   (-> [0 0] cartesian->polar polar->cartesian)))
  (is (near [-20 0] (-> [-20 0] cartesian->polar polar->cartesian)))
  (is (near [20 0]  (-> [20 0] cartesian->polar polar->cartesian))))
