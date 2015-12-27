(ns dali.layout.surround
  (:require [dali.layout :as layout]
            [dali.syntax :as d]))

(defmethod layout/layout-nodes :dali/surround
  [_ {{:keys [attrs padding rounded]} :attrs} elements bounds-fn]
  (let [padding (or padding 20)
        bounds  (map bounds-fn elements)
        min-x   (apply min (map (fn [[_ [x y] [w h]]] x) bounds))
        min-y   (apply min (map (fn [[_ [x y] [w h]]] y) bounds))
        max-x   (apply max (map (fn [[_ [x y] [w h]]] (+ x w)) bounds))
        max-y   (apply max (map (fn [[_ [x y] [w h]]] (+ y h)) bounds))
        x       min-x
        y       min-y
        w       (- max-x min-x)
        h       (- max-y min-y)]
    (if rounded
      [(d/dali->ixml
        [:rect
         attrs
         [(- x padding) (- y padding)]
         [(+ w (* 2 padding)) (+ h (* 2 padding))]
         rounded])]
      [(d/dali->ixml
        [:rect
         attrs
         [(- x padding) (- y padding)]
         [(+ w (* 2 padding)) (+ h (* 2 padding))]])])))
