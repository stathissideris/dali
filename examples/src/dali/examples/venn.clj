(ns dali.examples.venn
  (:require [dali.syntax :as dali]
            [dali.prefab :refer [stripe-pattern]]
            [dali.batik :as btk]))

(defn png-venn-diagram [filename]
  (->
   (let [r 130
         y 200
         x1 200
         x2 370
         outline 3]
     [:page {:width 570 :height 400}
      [:defs
       (stripe-pattern :stripes, :angle 0 :width 2 :width2 12 :fill :lightgray :fill2 :blue)
       (stripe-pattern :stripes2, :angle 90 :width 2 :width2 12 :fill :lightgray :fill2 :red)]
      [:circle {:stroke :none :fill :white} [x1 y] r]
      [:circle {:stroke :none :fill :white} [x2 y] r]
      [:circle {:stroke :none :fill "url(#stripes)" :opacity 0.2} [x1 y] r]
      [:circle {:stroke :none :fill "url(#stripes2)" :opacity 0.2} [x2 y] r]
      [:circle {:stroke {:paint :gray :width 3} :fill :none} [x1 y] r]
      [:circle {:stroke {:paint :gray :width 3} :fill :none} [x2 y] r]])
   dali/dali->hiccup
   ;;(spit-svg "/tmp/venn2.svg")
   dali/hiccup->svg-document-string
   btk/parse-svg-string
   (btk/render-document-to-png filename)))

(comment
  (png-venn-diagram "/tmp/venn2.png"))
