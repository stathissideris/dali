(ns dali.layout.align
  (:require [clojure.string :as string]
            [dali.layout :as layout]
            [dali.layout.utils :refer [bounds->anchor-point place-by-anchor]]
            [dali.utils :as utils]))

(def ^:private axes
  #{:top :bottom :left :right :v-center :h-center :center})

(defn- guide-from-element [node axis bounds-fn]
  (let [[_ [x y] [w h]] (bounds-fn node)]
    (condp = axis
      :top y
      :bottom (+ y h)
      :left x
      :right (+ x w)
      :v-center (+ y (/ h 2))
      :h-center (+ x (/ w 2)))))

(defn- align-center [{{:keys [relative-to axis]} :attrs} elements bounds-fn]
  (when (number? relative-to)
    (utils/exception ":relative-to cannot be a number when :axis is :center"))
  (let [relative-to (or relative-to :first)
        bounds      (map bounds-fn elements)
        rel-bounds  (if (= :first relative-to) (first bounds) (last bounds))
        pos         (bounds->anchor-point :center rel-bounds)]
   (map (fn [e b]
          (place-by-anchor e :center pos b)) elements bounds)))

(defmethod layout/layout-nodes :dali/align
  [_ {{:keys [relative-to axis]} :attrs :as tag} elements bounds-fn]
  (assert (or (= :first relative-to)
              (= :last relative-to)
              (number? relative-to)
              (nil? relative-to)) ":relative-to can either be a number or :first or :last")
  (assert (axes axis)
          (str ":axis has to be one of " (string/join ", " axes)))
  (if (= :center axis)
    (align-center tag elements bounds-fn)
    (let [guide     (if (number? relative-to)
                      relative-to
                      (guide-from-element (if (= :last relative-to)
                                            (last elements)
                                            (first elements)) axis bounds-fn))
          anchor    (condp = axis
                      :left     :top-left
                      :right    :top-right
                      :top      :top-left
                      :bottom   :bottom-left
                      :v-center :top
                      :h-center :left)
          v-guide?  (#{:right :left :v-center} axis)
          bounds    (map bounds-fn elements)
          positions (if v-guide?
                      (map (fn [[_ [_ y]]] y) bounds)
                      (map (fn [[_ [x _]]] x) bounds))]
      (map (fn [e pos bounds]
             (place-by-anchor e anchor (if v-guide?
                                         [guide pos]
                                         [pos guide]) bounds))
           elements positions bounds))))
