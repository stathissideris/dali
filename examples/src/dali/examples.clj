(ns dali.examples
  (:require [dali.syntax :as s]))

(def hello-world
  {:filename "hello-world.svg"
   :document
   [:page {:width 60 :height 60}
    [:circle
     {:stroke :blue :stroke-width 4 :fill :yellow}
     [30 30] 20]]})

(def examples [hello-world])

(defn render-examples [documents]
  (doseq [{:keys [document filename]} documents]
    (-> document (s/dali->hiccup) (s/spit-svg (str "examples/output/" filename)))))

(comment
  (render-examples examples)
  )

