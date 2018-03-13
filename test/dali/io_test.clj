(ns dali.io-test
  (:require [dali.io :refer :all]
            [clojure.test :refer :all]))
            
(deftest test-render-svg-string
  (testing "Test render-svg-string"
    (are [doc expected] (= expected (render-svg-string doc))
      [:dali/page
       [:circle [0 0] 20]]
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\"30.0\" height=\"30.0\"><circle cx=\"0\" cy=\"0\" r=\"20\"></circle></svg>"

      [:dali/page {:stroke :none}
       [:g {:transform [:translate [-10 -10]]}
        [:dali/stack
         {:direction :right}
         [:rect {:fill :mediumslateblue} [10 10] [50 20]]
         [:rect {:fill :sandybrown} :_ [30 20]]
         [:rect {:fill :green} :_ [40 20]]
         [:rect {:fill :orange} :_ [20 20]]]]]
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.2\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" stroke=\"none\" width=\"150.0\" height=\"30.0\"><g transform=\"translate(-10 -10)\"><g data-dali-layout-tag=\"stack\"><rect x=\"10\" y=\"10\" width=\"50\" height=\"20\" fill=\"mediumslateblue\"></rect><rect x=\"0\" y=\"0\" width=\"30\" height=\"20\" fill=\"sandybrown\" transform=\"translate(60.0 10.0)\"></rect><rect x=\"0\" y=\"0\" width=\"40\" height=\"20\" fill=\"green\" transform=\"translate(90.0 10.0)\"></rect><rect x=\"0\" y=\"0\" width=\"20\" height=\"20\" fill=\"orange\" transform=\"translate(130.0 10.0)\"></rect></g></g></svg>")))
