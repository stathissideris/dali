(ns dali.syntax-test
  (:require [dali.syntax :refer :all]
            [clojure.test :refer :all]))

(deftest test-node->xml
  (is (= {:tag :polyline}
         (node->ixml [:polyline])))
  (is (= {:tag :polyline
          :content [[:foo]]}
         (node->ixml [:polyline [:foo]])))
  (is (= {:tag :polyline
          :content [[:foo] [:bar]]}
         (node->ixml [:polyline [:foo] [:bar]])))
  (is (= {:tag :polyline
          :attrs {:foo :bar}
          :content [[:foo]]}
         (node->ixml [:polyline {:foo :bar} [:foo]])))
  (is (= {:tag :polyline
          :attrs {:foo :bar}
          :content [[:foo] [:bar]]}
         (node->ixml [:polyline {:foo :bar} [:foo] [:bar]])))
  (is (= {:tag :rect
          :attrs {:transform [[:rotate [10 60 20]] [:skew-x [30]]]
                  :dali/content-attr [[50 10] [20 20]]}}
         (node->ixml [:rect
                     {:transform [:rotate [10 60 20] :skew-x [30]]}
                     [50 10] [20 20]])))
  (is (= {:tag :b, :content ["test"]}
         (node->ixml [:b "test"])))
  (is (= {:tag :g
          :attrs {}
          :content
          [[:rect {:x 0 :y 0}]]}
       (node->ixml
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
         (node->ixml
          [:path
           {:id :thick :stroke-width 20}
           :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]]))))

(deftest test-dali->xml
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
