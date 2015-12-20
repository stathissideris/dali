(ns dali.layout.connect
  (:require [dali
             [geom :as geom]
             [utils :as utils]]
            [dali.layout :as layout]
            [dali.layout.utils :refer [bounds->anchor-point]]
            [net.cgrand.enlive-html :as en]))

(defn- straight-anchors [element1 element2 bounds-fn]
  (let [bounds1 (bounds-fn element1)
        bounds2 (bounds-fn element2)]
    (->>
     (for [[anchor1 anchor2] [[:left :right]
                              [:right :left]
                              [:top :bottom]
                              [:bottom :top]]]
       [[anchor1 anchor2]
        (geom/distance-squared
         (bounds->anchor-point anchor1 bounds1)
         (bounds->anchor-point anchor2 bounds2))])
     (sort-by second)
     first    ;;get the shortest distance
     first))) ;;return the anchor pair

(defn- corner-anchor [bounds intersection]
  (->>
   (for [a [:top :bottom :left :right]]
     [a (geom/distance-squared
         intersection
         (bounds->anchor-point a bounds))])
   (sort-by second)
   first   ;;get the shortest distance
   first)) ;;return the anchor

(defn- connector [attrs points]
  {:tag   :polyline
   :attrs (merge
           {:class :connector
            :dali/content points}
           attrs)})

;;TODO refactor so that you pass bounds, not elements?
(defn- straight-connector [start-element end-element attrs bounds-fn]
  (let [[a1 a2] (straight-anchors start-element end-element bounds-fn)
        p1      (bounds->anchor-point a1 (bounds-fn start-element))
        p2      (bounds->anchor-point a2 (bounds-fn end-element))]
    (connector attrs [p1 p2])))

;;TODO refactor so that you pass bounds, not elements?
(defn- corner-connector [start-element end-element attrs connection-type bounds-fn]
  (let [bounds1      (bounds-fn start-element)
        bounds2      (bounds-fn end-element)
        [cx1 cy1]    (bounds->anchor-point :center bounds1)
        [cx2 cy2]    (bounds->anchor-point :center bounds2)
        intersection (if (= :|- connection-type)
                       [cx1 cy2]
                       [cx2 cy1])
        p1           (-> (corner-anchor bounds1 intersection)
                         (bounds->anchor-point bounds1))
        p2           (-> (corner-anchor bounds2 intersection)
                         (bounds->anchor-point bounds2))]
    (connector attrs [p1 intersection p2])))

(defmethod layout/layout-nodes :dali/connect
  [document tag elements bounds-fn]
  (let [make-selector   (fn [x] (if (keyword? x) [(->> x name (str "#") keyword)] x))

        connection-type (or (-> tag :attrs :type) :--)
        start-selector  (make-selector (-> tag :attrs :from))
        end-selector    (make-selector (-> tag :attrs :to))

        start-element   (first (en/select document start-selector))
        end-element     (first (en/select document end-selector))

        check-element   (fn [x name]
                          (when-not x
                            (throw (utils/exception
                                    (format
                                     "Cannot find %s element using selector '%s' referred to by connector '%s'"
                                     name
                                     (pr-str x)
                                     (pr-str tag))))))
        attrs           (dissoc (:attrs tag) :from :to :type :dali/path)]
    (check-element start-element "start")
    (check-element end-element "end")
    (if (= :-- connection-type)
      [(straight-connector start-element end-element attrs bounds-fn)]
      [(corner-connector start-element end-element attrs connection-type bounds-fn)])))
