(ns dali.library)

(defn stripe-pattern [id & {:keys (angle width fill width2 fill2)}]
  (let [width (or width 10)
        width2 (or width2 width)
        fill (or fill :black)
        fill2 (or fill2 :white)
        pattern
        [:pattern
         (merge
          {:id id
           :width 10 :height (+ width width2)
           :patternUnits :userSpaceOnUse}
          (when angle
            {:patternTransform (str "rotate(" angle ")")}))
         [:rect {:fill fill :stroke :none} [0 0] [10 width]]]]
    (if-not fill2
      pattern
      (conj pattern
            [:rect {:fill fill2 :stroke :none} [0 width] [10 width2]]))))
