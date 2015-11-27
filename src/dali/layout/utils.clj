(ns dali.layout.utils
  (:require [dali
             [geom :as geom :refer [v-]]
             [syntax :as syntax]]))

(defn bounds->anchor-point
  [anchor [_ [x y] [w h]]]
  (let [res
        (condp = anchor
          :top-left     [x y]
          :top          [(+ x (/ w 2)) y]
          :top-right    [(+ x w) y]
          :left         [x (+ y (/ h 2))]
          :right        [(+ x w) (+ y (/ h 2))]
          :bottom-left  [x (+ y h)]
          :bottom       [(+ x (/ w 2)) (+ y h)]
          :bottom-right [(+ x w) (+ y h)]
          :center       [(+ x (/ w 2)) (+ y (/ h 2))])]
    (-> res
        (update 0 float)
        (update 1 float))))

(defn place-top-left
  "Adds a translation transform to an element so that its top-left
  corner is at the passed position."
  [element top-left bounds]
  (let [type (first element)
        [_ current-pos [w h]] bounds]
    (let [tr (v- top-left current-pos)]
      (if (every? zero? tr)
        element
        (syntax/add-transform element [:translate tr])))))

(defn place-by-anchor
  [element anchor position bounds]
  (let [[_ original-position] bounds
        anchor-point (bounds->anchor-point anchor bounds)]
    (place-top-left
     element
     (v- position (v- anchor-point original-position))
     bounds)))
