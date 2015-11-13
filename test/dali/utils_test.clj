(ns dali.utils-test
  (:require [dali.utils :refer :all]
            [clojure.zip :as zip]
            [clojure.test :refer :all :exclude [function?]]))

(deftest test-transform-zipper-backwards
  (is (= [[5 6 7] [8 9 10 [11 12]]]
         (-> [[4 5 6] [7 8 9 [10 11]]]
             generic-zipper
             zipper-last
             (transform-zipper-backwards
              (fn [z]
                (let [node (zip/node z)]
                  (if (number? node) (inc node) node))))))))
