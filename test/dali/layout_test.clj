(ns dali.layout-test
  (:require [dali.layout :refer :all]
            [clojure.test :refer :all]))

(def index-tree @#'dali.layout/index-tree)

(deftest test-index-tree
  (is (= {:tag :svg
          :attrs {:dali/path [0]}
          :content
          [{:tag :g
            :attrs {:dali/path [0 0]}
            :content
            [{:tag :circle :attrs {:cx 1 :cy 30 :r 20 :dali/path [0 0 0]}}
             {:tag :circle :attrs {:cx 2 :cy 30 :r 20 :dali/path [0 0 1]}}
             {:tag :circle :attrs {:cx 3 :cy 30 :r 20 :dali/path [0 0 2]}}
             {:tag :circle :attrs {:cx 4 :cy 30 :r 20 :dali/path [0 0 3]}}]}
           {:tag :g
            :attrs {:dali/path [0 1]}
            :content
            [{:tag :circle :attrs {:cx 5 :cy 30 :r 20 :dali/path [0 1 0]}}
             {:tag :circle :attrs {:cx 6 :cy 30 :r 20 :dali/path [0 1 1]}}
             {:tag :circle :attrs {:cx 7 :cy 30 :r 20 :dali/path [0 1 2]}}
             {:tag :circle :attrs {:cx 8 :cy 30 :r 20 :dali/path [0 1 3]}}]}]}
         (index-tree
          {:tag :svg
           :content
           [{:tag :g
             :content
             [{:tag :circle :attrs {:cx 1 :cy 30 :r 20}}
              {:tag :circle :attrs {:cx 2 :cy 30 :r 20}}
              {:tag :circle :attrs {:cx 3 :cy 30 :r 20}}
              {:tag :circle :attrs {:cx 4 :cy 30 :r 20}}]}
            {:tag :g
             :content
             [{:tag :circle :attrs {:cx 5 :cy 30 :r 20}}
              {:tag :circle :attrs {:cx 6 :cy 30 :r 20}}
              {:tag :circle :attrs {:cx 7 :cy 30 :r 20}}
              {:tag :circle :attrs {:cx 8 :cy 30 :r 20}}]}]}))))
