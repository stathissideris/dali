(ns dali.syntax-test
  (:require [dali.syntax :refer :all]
            [clojure.test :refer :all]))

(def process-attr-map @#'dali.syntax/process-attr-map)

(deftest test-process-attr-map
  (is (= {:cx 30, :cy 30, :r 20, :stroke "indigo", :stroke-width 4, :fill "darkorange"}
         (process-attr-map
          {:cx 30, :cy 30, :r 20,
           :stroke :indigo,
           :stroke-width 4,
           :fill :darkorange,
           :dali/content-attr [[30 30] 20]}))))

(deftest test-node->xml
  (is (= {:tag :polyline}
         (dali-node->ixml-node [:polyline])))
  (is (= {:tag :polyline
          :content [[:foo]]}
         (dali-node->ixml-node [:polyline [:foo]])))
  (is (= {:tag :polyline
          :content [[:foo] [:bar]]}
         (dali-node->ixml-node [:polyline [:foo] [:bar]])))
  (is (= {:tag :polyline
          :attrs {:foo :bar}
          :content [[:foo]]}
         (dali-node->ixml-node [:polyline {:foo :bar} [:foo]])))
  (is (= {:tag :polyline
          :attrs {:foo :bar}
          :content [[:foo] [:bar]]}
         (dali-node->ixml-node [:polyline {:foo :bar} [:foo] [:bar]])))
  (is (= {:tag :rect
          :attrs {:transform [[:rotate [10 60 20]] [:skew-x [30]]]
                  :dali/content-attr [[50 10] [20 20]]}}
         (dali-node->ixml-node [:rect
                     {:transform [:rotate [10 60 20] :skew-x [30]]}
                     [50 10] [20 20]])))
  (is (= {:tag :b, :content ["test"]}
         (dali-node->ixml-node [:b "test"])))
  (is (= {:tag :g
          :attrs {}
          :content
          [[:rect {:x 0 :y 0}]]}
       (dali-node->ixml-node
        [:g {}
         [:rect {:x 0 :y 0}]])))
  (is (= {:tag :path
          :attrs
          {:id :thick
           :stroke-width 20
           :dali/content-attr
           [[:M [[110 80]]]
            [:C [[140 10] [165 10] [195 80]]]
            [:S [[250 150] [280 80]]]]}}
         (dali-node->ixml-node
          [:path
           {:id :thick :stroke-width 20}
           :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]]))))

(deftest test-dali->xml
  (is (= {:tag :path
          :attrs {:d "M 110 80 C 140 10, 165 10, 195 80 S 250 150, 280 80"
                  :id "thick"
                  :stroke-width 20
                  :stroke "red"}}
         (dali->xml
          [:path
           {:id :thick :stroke-width 20 :stroke :red}
           :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]]))))

(deftest test-dali->ixml
  (is (= {:tag :polyline, :content [{:tag :foo}]}
         (dali->ixml [:polyline [:foo]])))
  (is (= {:tag :polyline, :content [{:tag :foo} {:tag :bar}]}
         (dali->ixml [:polyline [:foo] [:bar]])))
  (is (= {:tag :polyline, :attrs {:foo :bar}, :content [{:tag :foo}]}
         (dali->ixml [:polyline {:foo :bar} [:foo]])))
  (is (= {:tag :polyline, :attrs {:dali/content-attr [[1 2] [3 4]]}}
         (dali->ixml [:polyline [1 2] [3 4]])))
  (is (= {:tag :polyline, :attrs {:foo :bar, :dali/content-attr [[1 2] [3 4]]}}
         (dali->ixml [:polyline {:foo :bar} [1 2] [3 4]]))))

(deftest test-dali->ixml
  (is (= {:tag :page,
          :attrs {:width 60, :height 60},
          :content
          [{:tag :circle
            :attrs
            {:stroke :indigo
             :stroke-width 4
             :fill :darkorange
             :dali/content-attr [[30 30] 20]}}]}
         (dali->ixml [:page {:width 60 :height 60}
                      [:circle
                       {:stroke :indigo :stroke-width 4 :fill :darkorange}
                       [30 30] 20]])))
  (is (= {:tag :page,
          :attrs {:width 60, :height 60},
          :content
          [{:tag :circle,
            :attrs
            {:stroke :indigo
             :stroke-width 4,
             :fill :darkorange
             :dali/content-attr [[0 0] 20]}}]}
         (dali->ixml [:page {:width 60 :height 60}
                      [:circle
                       {:stroke :indigo :stroke-width 4 :fill :darkorange}
                       :_ 20]]))))

(deftest test-ixml-node->xml-node
  (is (= {:tag :circle
          :attrs {:stroke "indigo"
                  :stroke-width 4
                  :fill "darkorange"
                  :cx 30 :cy 30 :r 20}}
         (ixml-node->xml-node
          {:tag :circle
           :attrs {:stroke :indigo
                   :stroke-width 4
                   :fill :darkorange
                   :dali/content-attr [[30 30] 20]}})))
  (is (= {:tag :svg
          :attrs
          {:xmlns "http://www.w3.org/2000/svg"
           :version "1.2"
           :xmlns:xlink "http://www.w3.org/1999/xlink"
           :width 60
           :height 60}
          :content
          [{:tag :circle
            :attrs
            {:stroke :indigo
             :stroke-width 4
             :fill :darkorange
             :dali/content-attr [[30 30] 20]}}]}
         (ixml-node->xml-node
          {:tag :page,
           :attrs {:width 60 :height 60}
           :content
           [{:tag :circle
             :attrs
             {:stroke :indigo
              :stroke-width 4
              :fill :darkorange
              :dali/content-attr [[30 30] 20]}}]})))
  (is (= {:tag :rect
          :attrs
          {:x 0 :y 0
           :width 50 :height 20
           :fill "mediumslateblue"
           :transform "translate(10.0 10.0)"}}
         (ixml-node->xml-node
          {:tag :rect
           :attrs
           {:fill :mediumslateblue
            :dali/content-attr [[0 0] [50 20]]
            :transform [[:translate [10.0 10.0]]]}}))))

(deftest test-ixml->xml
  (is (= {:tag :svg
          :attrs
          {:xmlns "http://www.w3.org/2000/svg"
           :version "1.2"
           :xmlns:xlink "http://www.w3.org/1999/xlink"
           :width 60
           :height 60}
          :content
          [{:tag :circle
            :attrs
            {:cx 30
             :cy 30
             :r 20
             :stroke "indigo"
             :stroke-width 4
             :fill "darkorange"}}]}
         (ixml->xml
          {:tag :page
           :attrs {:width 60 :height 60}
           :content
           [{:tag :circle
             :attrs
             {:stroke :indigo
              :stroke-width 4
              :fill :darkorange
              :dali/content-attr [[30 30] 20]}}]})))
  (is (= {:tag :text
          :attrs {:font-family "Georgia"
                  :font-size 20
                  :stroke "none"
                  :fill "black"}
          :content ["up"]}
         (ixml->xml
          {:tag :text,
           :attrs
           {:font-family "Georgia",
            :font-size 20,
            :stroke :none,
            :fill :black},
           :content ["up"]}))))
