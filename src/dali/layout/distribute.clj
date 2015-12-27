(ns dali.layout.distribute
  (:require [dali.layout :as layout]
            [dali.layout.utils :refer [bounds->anchor-point place-by-anchor]]))

(defmethod layout/layout-nodes :dali/distribute
  [_ {{:keys [direction anchor gap]} :attrs} elements bounds-fn]
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

          [x y] (bounds->anchor-point anchor (first bounds))
          
          step (+ gap (if vertical?
                        (apply max (map (fn [[_ _ [_ h]]] h) bounds))
                        (apply max (map (fn [[_ _ [w _]]] w) bounds))))

          element-pos (if vertical?
                          (fn element-pos [pos orig-pos] [(first orig-pos) pos])
                          (fn element-pos [pos orig-pos] [pos (second orig-pos)]))

          positions (condp = direction
                      :down  (range y Integer/MAX_VALUE step)
                      :up    (range y Integer/MIN_VALUE (- step))
                      :right (range x Integer/MAX_VALUE step)
                      :left  (range x Integer/MIN_VALUE (- step)))]
      (map (fn [e pos b]
             (place-by-anchor
              e anchor (element-pos pos (bounds->anchor-point anchor b)) b))
           elements positions bounds))))
