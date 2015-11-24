(ns dali.prefab)

(defn stripe-pattern [id & [{:keys (angle width fill width2 fill2)}]]
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

(defn sharp-arrow-marker
  [id & [{:keys [width height attrs scale]}]]
  (let [w     (or width 8)
        h     (or height 11)
        scale (or scale 1)
        w     (if width w (* scale w))
        h     (if height h (* scale h))
        w2    (float (/ w 2))
        h3    (float (/ h 3))]
    [:symbol (merge
              {:id id :class [:dali-marker :sharp-arrow-end]
               :dali/marker-tip [(* 2 h3) 0] :style "overflow:visible;"} attrs)
     [:path :M [0 0] :L [(- h3) w2] :L [(* 2 h3) 0] :L [(- h3) (- w2)] :z]]))

(defn triangle-arrow-marker
  [id & [{:keys [width height attrs scale]}]]
  (let [w     (or width 8)
        h     (or height 11)
        scale (or scale 1)
        w     (if width w (* scale w))
        h     (if height h (* scale h))
        w2    (/ w 2)]
    [:symbol (merge
              {:id id :class [:dali-marker :triangle-arrow-end]
               :dali/marker-tip [h 0] :style "overflow:visible;"} attrs)
     [:path :M [0 (- w2)] :L [h 0] :L [0 w2] :z]]))

(defn curvy-arrow-marker
  [id & [{:keys [width height attrs scale]}]]
  (let [w     (or width 8)
        h     (or height 11)
        scale (or scale 1)
        w     (if width w (* scale w))
        h     (if height h (* scale h))
        w2    (/ w 2)]
    [:symbol (merge
              {:id id :class [:dali-marker :curvy-arrow-end]
               :dali/marker-tip [h 0] :style "overflow:visible;"} attrs)
     [:path
      :m [(* -0.15 h) (- w2)]
      :l [(* 1.15 h) w2]
      :l [(* -1.15 h) w2]
      :c [(* 0.2 h) (* -0.3 w)] [(* 0.2 h) (* -0.7 w)] [0,(- w)] :z]]))

(defn dot-marker
  [id & [{:keys [radius attrs]}]]
  (let [radius (or radius 4)]
    [:symbol (merge
              {:id id :class [:dali-marker :dot]
               :dali/marker-tip [0 0] :style "overflow:visible;"} attrs)
    [:circle [0 0] radius]]))

(defn drop-shadow-effect
  [id & [{:keys [color offset radius opacity filter-padding]}]]
  (let [offset              (or offset [5 5])
        color               (or color "#000000")
        radius              (or radius 4)
        opacity             (or opacity 0.5)
        filter-padding      (or filter-padding 20)
        percent             #(str % "%")
        filter-padding-pos  (percent (- filter-padding))
        filter-padding-size (percent (+ 100 (* 2 filter-padding)))
        [dx dy]             offset]
    [:filter {:id id
              :x filter-padding-pos
              :y filter-padding-pos
              :width filter-padding-size
              :height filter-padding-size}
     [:feGaussianBlur {:in "SourceAlpha"
                       :stdDeviation (/ radius 2) ;; http://dbaron.org/log/20110225-blur-radius
                       :result :blur-out}]
     [:feOffset {:dx dx, :dy dy, :in :blur-out, :result :the-shadow}]
     [:feColorMatrix {:in :the-shadow, :result :color-out, :type :matrix
                      :values [0 0 0 0 0
                               0 0 0 0 0
                               0 0 0 0 0
                               0 0 0 opacity 0]}]
     [:feBlend {:in "SourceGraphic" :in2 :color-out :mode :normal}]]))
