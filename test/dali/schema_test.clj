(ns dali.schema-test
  (:require [dali.schema :refer :all]
            [clojure.test :refer :all]))

(deftest test-validate
  (is (validate
       [:dali/page {:width 60 :height 60}
        [:circle
         {:stroke :indigo :stroke-width 4 :fill :darkorange}
         [30 30] 20]]))
  (is (validate
       [:dali/page {:width 220 :height 130 :stroke-width 2 :stroke :black :fill :none}
        [:polyline (map #(vector %1 %2) (range 10 210 20) (cycle [10 30]))]
        [:polyline (map #(vector %1 %2) (range 10 210 5) (cycle [60 80]))]
        [:polyline (map #(vector %1 %2) (range 10 210 10) (cycle [100 100 120 120]))]]))
  (is (validate
       [:dali/page {:width 90 :height 50 :stroke :black :stroke-width 2 :fill :none}
        [:rect {:transform [:rotate [30 30 20]]}
         [20 10] [20 20]]
        [:circle {:stroke :none :fill :red} [30 20] 2]
        [:rect {:transform [:rotate [10 60 20] :skew-x [30]]}
         [50 10] [20 20]]]))
  (is (validate
       [:dali/page
        {:height 500 :width 500, :stroke {:paint :black :width 2} :fill :none}

        [:rect {:stroke :blue} [110.0 10.0] [170.0 140.0]] ;;geometry bounds
        [:rect {:stroke :red} [100.80886 17.5] [188.38226 125.0]] ;;sensitive bounds
        [:path {:id :thick :stroke-width 20} :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]]
        [:path {:stroke :white} :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]] ;;sensitive bounds

        [:path :M [45 10] :l [10 10] :l [-10 10] :l [-10 -10] :z]
        [:line [10 20] [100 100]]
        [:line [10 100] [100 20]]
        [:polyline
         (map #(vector %1 %2) (range 10 150 10) (cycle [110 120]))]
        [:rect {:id :rounded} [10 130] [100 60] 15]
        [:g {:stroke {:paint :green :width 4} :fill :white}
         (map
          #(vector :circle [% 160] 15)
          (range 35 85 15))]
        [:rect {:stroke-dasharray [5 10 5]} [10 200] [100 60] 15]

        [:rect {:stroke :green} [10.28125 271.26172] [357.16016 11.265625]] ;;bounds of the text
        [:text {:id :the-text :x 10 :y 280 :stroke :none :fill :black}
         "The only difference between me and a madman is that I'm not mad."]]))
  (is (thrown?
       clojure.lang.ExceptionInfo
       (validate
        [:dali/page {:width 60 :height 60}
         [:circle
          {:stroke :indigo :stroke-width 4 :fill :darkorange}
          [30 30]]]))) ;;circle needs a radius
  (is (thrown?
       clojure.lang.ExceptionInfo
       (validate
        [:dali/page {:width 220 :height 130 :stroke-width 2 :stroke :black 1 :none} ;;keys can't be numeric
         [:polyline (map #(vector %1 %2) (range 10 210 20) (cycle [10 30]))]
         [:polyline (map #(vector %1 %2) (range 10 210 5) (cycle [60 80]))]
         [:polyline (map #(vector %1 %2) (range 10 210 10) (cycle [100 100 120 120]))]])))
  (is (thrown?
       clojure.lang.ExceptionInfo
       (validate
        [:dali/page {:width 90 :height 50 :stroke :black :stroke-width 2 :fill :none}
         [:rect {:transform [:rotate [30 30 20]]}
          [20 10] [20 20]]
         [:circle {:stroke :none :fill :red} [30 20] 2]
         [:rect {:transform [:WRONG [10 60 20] :skew-x [30]]}
          [50 10] [20 20]]])))
  (is (thrown?
       clojure.lang.ExceptionInfo
       (validate
        [:dali/page {:width 90 :height 50 :stroke :black :stroke-width 2 :fill :none}
         [:rect {:transform [:rotate [30 30]]} ;;3 params needed
          [20 10] [20 20]]
         [:circle {:stroke :none :fill :red} [30 20] 2]
         [:rect {:transform [:rotate [10 60 20] :skew-x [30]]}
          [50 10] [20 20]]]))))
