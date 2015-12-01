(ns dali.layout.distribute
  (:require [dali.layout :as layout]
            [dali.layout.utils :refer [place-by-anchor]]))

(defmethod layout/layout-nodes :distribute
  [_ {{:keys [position direction anchor gap]} :attrs} elements bounds-fn]
  (let [direction (or direction :right)
        anchor (or anchor :center)
        vertical? (or (= direction :down) (= direction :up))]
    (if vertical?
      (when (not (#{:center :left :right} anchor))
        (throw (Exception. (str "distribute layout supports only :center :left :right anchors for direction " direction "\n elements: " elements))))
      (when (not (#{:center :top :bottom} anchor))
        (throw (Exception. (str "distribute layout supports only :center :top :bottom anchors for direction " direction "\n elements: " elements)))))
    (let [gap (or gap 0)
          elements (if (seq? (first elements)) (first elements) elements) ;;so that you map over elements etc
          bounds (map bounds-fn elements)

          position (or position (-> bounds first second))
          [x y] position
          
          step (+ gap (if vertical?
                        (apply max (map (fn [[_ _ [_ h]]] h) bounds))
                        (apply max (map (fn [[_ _ [w _]]] w) bounds))))
          place-point (if vertical?
                        (fn place-point [x y pos orig-pos] [(first orig-pos) pos])
                        (fn place-point [x y pos orig-pos] [pos (second orig-pos)]))

          first-offset (+ gap (/ step 2))
          positions (condp = direction
                      :down  (range (+ y first-offset) Integer/MAX_VALUE step)
                      :up    (range (- y first-offset) Integer/MIN_VALUE (- step))
                      :right (range (+ x first-offset) Integer/MAX_VALUE step)
                      :left  (range (- x first-offset) Integer/MIN_VALUE (- step)))]
      (map (fn [e pos b] (place-by-anchor e anchor (place-point x y pos (second b)) b))
           elements positions bounds))))
