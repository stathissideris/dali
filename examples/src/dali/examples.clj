(ns dali.examples
  (:require [dali.syntax :as s]))

(def examples
  [{:filename "hello-world.svg"
    :document
    [:page {:width 60 :height 60}
     [:circle
      {:stroke :indigo :stroke-width 4 :fill :darkorange}
      [30 30] 20]]}

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
     [:line {:stroke-dasharray [5 10]} [10 20] [110 20]]]}])

(defn render-examples [documents]
  (doseq [{:keys [document filename]} documents]
    (-> document (s/dali->hiccup) (s/spit-svg (str "examples/output/" filename)))))

(comment
  (render-examples examples)
  )

