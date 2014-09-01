(ns dali.examples
  (:require [dali.syntax :as s]))

(def examples
  [{:filename "hello-world.svg"
    :document
    [:page {:width 60 :height 60}
     [:circle
      {:stroke :indigo :stroke-width 4 :fill :darkorange}
      [30 30] 20]]}

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

