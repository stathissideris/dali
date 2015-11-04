(ns dali.prefab)

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

(defn sharp-arrow-end
  ;;based on  Arrow1Lend in Inkscape
  [id & {:keys [width height style]}]
  (let [w (float (/ (or width 8) 2))
        h (float (/ (or height 20) 2))]
   [:marker {:id id :ref-x (- h 2.5) :ref-y 0 :orient :auto :style "overflow:visible;"}
    [:path (merge {:fill :black :stroke :none} style)
     :M [0 0] :L [(- w) w] :L [h 0] :L [(- w) (- w)] :z]]))

(defn triangle-arrow-end
  [id & {:keys [width height style]}]
  (let [w (float (/ (or width 8) 2))
        h (or height 11)]
   [:marker {:id id :ref-x (- h 2.3) :ref-y 0 :orient :auto :style "overflow:visible;"}
    [:path (merge {:fill :black :stroke :none} style)
     :M [0 w] :L [h 0] :L [0 (- w)] :z]]))

(defn curvy-arrow-end
  [id & {:keys [width height style]}]
  (let [w (float (/ (or width 10) 2))
        h (or height 10)]
   [:marker {:id id :ref-x (- h 2.3) :ref-y 0 :orient :auto :style "overflow:visible;"}
    [:path (merge {:fill :black :stroke :none} style)
     :M [-0.78996659 -4.0017078] :L [10.13591 0.01601414] :L [-0.78996719 4.0337352]
     :c [1.74549835,-2.3720609] [1.73544075,-5.6174519] [0,-8.035443] :z]]))

(defn dot-end
  [id & {:keys [radius]}]
  (let [radius (or radius 2.2)]
   [:marker {:id id :ref-x 0 :ref-y 0 :orient :auto :style "overflow:visible;"}
    [:circle [0 0] radius]]))
