(ns dali.examples
  (:require [dali.syntax :as s]
            [dali.layout :as layout]))

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
                    [:text {:font-family "Georgia" :font-size 20} s]
                    [:rect :_ [20 20]]
                    [:circle :_ 15]
                    [:polyline [0 0] [20 0] [10 20] [20 20]]))]
      [:page {:width 350 :height 500 :stroke {:paint :black :width 2} :fill :none}
       [:stack {:position [20 20] :direction :right} (shapes "right")]
       [:stack {:position [130 70] :gap 5 :direction :left} (shapes "left")]
       [:stack {:position [40 150] :gap 5 :direction :down} (shapes "down")]
       [:stack {:position [90 250] :gap 18 :direction :up} (shapes "up")]])}])

(defn render-examples [documents]
  (doseq [{:keys [document filename]} documents]
    (-> document
        (layout/resolve-layout)
        (s/dali->hiccup)
        (s/spit-svg (str "examples/output/" filename)))))


(render-examples examples)


