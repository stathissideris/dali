(ns dali.batik-test
  (:require [dali.batik :refer :all]
            [clojure.test :refer :all]
            [dali.syntax :as syntax]))

(deftest test-outline
  (testing "outline"
    (let [path [:page {:height 500 :width 500}
                [:path
                 {:id :path}
                 :M [110 80]
                 :C [140 10] [165 10] [195 80]
                 :C [225 150] [250 150] [280 80]]]]
      (is (= path
             (-> path
                 syntax/dali->hiccup
                 syntax/hiccup->svg-document-string
                 parse-svg-string
                 context
                 (gvt-node-by-id "path")
                 (outline)))))))
