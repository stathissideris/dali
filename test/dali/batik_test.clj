(ns dali.batik-test
  (:require [clojure.test :refer :all]
            [dali.batik :as batik]
            [dali.batik :refer :all]
            [dali.io :as io]
            [dali.layout :as layout]
            [dali.syntax :as syntax]))

(comment
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
                  syntax/ixml->xml
                  io/xml->svg-document-string
                  parse-svg-string
                  context
                  (gvt-node-by-id "path")
                  (outline))))))))

(deftest test-get-bounds
  (is (= [:rect [55.0 45.0] [10.0 10.0]]
         (let [ctx
               (batik/context
                (@#'layout/index-tree
                 (syntax/dali->ixml
                  [:page
                   [:g {:transform [:translate [20 10]]}
                    [:g {:transform [:translate [13 13]]}
                     [:rect {:transform [:translate [11 11]]} [0 0] [10 10]]]]])))]
           (get-bounds ctx {:attrs {:dali/path [0 0 0 0]}})))))
