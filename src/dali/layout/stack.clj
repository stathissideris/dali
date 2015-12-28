(ns dali.layout.stack
  (:require [dali.layout :as layout]
            [dali.layout.utils :refer [bounds->anchor-point place-by-anchor]]
            [retrograde :as retro]))

(def direction->default-anchor
  {:down :top
   :up :bottom
   :right :left
   :left :right})

(defmethod layout/layout-nodes :dali/stack
  [_ {{:keys [direction anchor gap]} :attrs} nodes bounds-fn]
  (let [gap         (or gap 0)
        direction   (or direction :down)
        anchor      (or anchor (direction->default-anchor direction))
        nodes       (if (seq? (first nodes)) (first nodes) nodes) ;;so that you map over nodes etc
        [x y]       (bounds->anchor-point anchor (bounds-fn (first nodes)))
        vertical?   (or (= direction :down) (= direction :up))
        advance-pos (if (or (= direction :down) (= direction :right)) + -)
        get-size    (if vertical?
                      (fn get-size [[_ _ [_ h]]] h)
                      (fn get-size [[_ _ [w _]]] w))
        get-pos     (if vertical?
                      (fn get-pos [[_ [_ y] _]] y)
                      (fn get-pos [[_ [x _] _]] x))
        place-point (if vertical?
                      (fn place-point [x y pos] [x pos])
                      (fn place-point [x y pos] [pos y]))
        initial-pos (if vertical? y x)]
    (retro/transform
     [this-gap 0 gap
      bounds nil (bounds-fn element)
      size 0 (get-size bounds)
      pos 0 (get-pos bounds)
      this-pos initial-pos (advance-pos this-pos' size' this-gap')
      element (place-by-anchor element anchor (place-point x y this-pos) bounds)]
     nodes)))
