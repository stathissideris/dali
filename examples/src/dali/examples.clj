(ns dali.examples
  (:require [dali.syntax :as s]
            [dali.layout :as layout]
            [dali.stock :as stock]))

(def examples
  [{:filename "hello-world.svg"
    :document
    [:page {:width 60 :height 60}
     [:circle
      {:stroke :indigo :stroke-width 4 :fill :darkorange}
      [30 30] 20]]}

   ;;transform syntax demonstrated
   {:filename "transform.svg"
    :document
    [:page {:width 90 :height 50 :stroke :black :stroke-width 2 :fill :none}
     
     [:rect {:transform [[:rotate [30 30 20]]]} ;;rotate around center marked by circle
      [20 10] [20 20]]
     [:circle {:stroke :none :fill :red} [30 20] 2]

     [:rect {:transform [[:rotate [10 60 20]] [:skew-x [30]]]}
      [50 10] [20 20]]]}

   {:filename "dasharray.svg"
    :document
    [:page {:width 120 :height 30 :stroke :black :stroke-width 2}
     [:line {:stroke-dasharray [10 5]} [10 10] [110 10]]
     [:line {:stroke-dasharray [5 10]} [10 20] [110 20]]]}

   ;;basic stacking
   {:filename "stack1.svg"
    :document
    [:page {:width 200 :height 40 :stroke :none}
     [:stack
      {:position [10 20] :anchor :left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 20]]
      [:rect {:fill :green} :_ [40 20]]
      [:rect {:fill :orange} :_ [20 20]]]]}

   ;;stacking with anchors
   {:filename "stack2.svg"
    :document
    [:page {:width 200 :height 80 :stroke :none}
     [:stack
      {:position [10 40] :anchor :left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [10 40] 4]]}

   ;;stacking with other anchors
   {:filename "stack3.svg"
    :document
    [:page {:width 310 :height 80 :stroke :none}
     [:stack
      {:position [10 70] :anchor :bottom-left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [10 70] 4]
     
     [:stack
      {:position [170 10] :anchor :top-left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [170 10] 4]]}

   ;;stacking with different directions and gaps
   {:filename "stack4.svg"
    :document
    (let [shapes (fn [s]
                   (list
                    [:text {:font-family "Georgia" :font-size 20
                            :stroke :none :fill :black} s]
                    [:rect :_ [20 20]]
                    [:circle :_ 15]
                    [:polyline [0 0] [20 0] [10 20] [20 20]]))]
      [:page {:width 150 :height 260 :stroke {:paint :black :width 2} :fill :none}
       [:stack {:position [20 20] :direction :right} (shapes "right")]
       [:stack {:position [130 70] :gap 5 :direction :left} (shapes "left")]
       [:stack {:position [40 150] :gap 5 :direction :down} (shapes "down")]
       [:stack {:position [110 250] :gap 18 :direction :up} (shapes "up")]])}

   {:filename "distribute1.svg"
    :document
    [:page {:width 200 :height 60 :stroke :none}
     [:distribute
      {:position [10 20] :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 20]]
      [:rect {:fill :green} :_ [40 20]]
      [:rect {:fill :orange} :_ [20 20]]]

     ;;show centers
     [:g (map #(vector
                :line
                {:stroke {:paint :red :width 2}} [% 40] [% 50])
              (range 35 200 50))]]}

   {:filename "markers1.svg"
    :document
    [:page {:width 220 :height 90 :stroke {:width 2 :paint :black}}
     [:defs
      (stock/sharp-arrow-end :sharp)
      (stock/triangle-arrow-end :triangle)
      (stock/curvy-arrow-end :curvy)
      (stock/dot-end :dot)
      (stock/sharp-arrow-end :very-sharp :height 32)]
     [:polyline
      {:fill :none :marker-end "url(#sharp)"}
      [50 80] [90 30]]
     [:polyline
      {:fill :none :marker-end "url(#triangle)"}
      [80 80] [120 30]]
     [:polyline
      {:fill :none :marker-end "url(#curvy)"}
      [110 80] [150 30]]
     [:polyline
      {:fill :none :marker-end "url(#dot)"}
      [140 80] [180 30]]
     [:polyline
      {:fill :none :marker-end "url(#very-sharp)"}
      [170 80] [210 30]]]}

   {:filename "graph1.svg"
    :document
    [:page {:width 260 :height 140}
     [:stack
      {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [h] [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]])
           [10 30 22 56 90 59 23 12 44 50])]]}

   {:filename "graph2.svg"
    :document
    [:page {:width 270 :height 140}
     [:stack
      {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [h]
             [:stack
              {:direction :up :gap 6}
              [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]]
              [:text {:text-family "Verdana" :font-size 12} (str h)]])
           [10 30 22 56 90 59 23 12 44 50])]]}

   {:filename "graph3.svg"
    :document
    [:page {:width 270 :height 150}
     [:stack
      {:position [10 140], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [[a b c]]
             [:stack
              {:direction :up}
              [:rect {:stroke :none, :fill "#D46A6A"} :_ [20 a]]
              [:rect {:stroke :none, :fill "#D49A6A"} :_ [20 b]]
              [:rect {:stroke :none, :fill "#407F7F"} :_ [20 c]]])
           [[10 10 15]
            [30 10 20]
            [22 10 25]
            [56 10 10]
            [90 10 30]
            [59 22 25]
            [23 10 13]
            [12 6 8]
            [44 22 18]
            [50 20 10]])]]}
   ])

(defn render-examples [documents]
  (doseq [{:keys [document filename]} documents]
    (-> document
        (layout/resolve-layout)
        (s/dali->hiccup)
        (s/spit-svg (str "examples/output/" filename)))))

(comment
 (render-examples examples)
 )


