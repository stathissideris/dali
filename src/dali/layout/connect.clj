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

(defn- connector [attrs points]
  {:tag   :polyline
   :attrs (merge
           {:class :connector
            :dali/content points}
           attrs)})

;;TODO refactor so that you pass bounds, not elements?
(defn- straight-connector [start-element end-element attrs bounds-fn]
  (let [manual-anchor1 (:from-anchor attrs)
        manual-anchor2 (:to-anchor attrs)
        [a1 a2]        (straight-anchors start-element end-element bounds-fn)
        p1             (-> (or manual-anchor1 a1)
                           (bounds->anchor-point (bounds-fn start-element)))
        p2             (-> (or manual-anchor2 a2)
                           (bounds->anchor-point (bounds-fn end-element)))]
    (connector attrs [p1 p2])))

(defn- corner-anchors [element1 element2 type bounds-fn]
  (let [bounds1 (bounds-fn element1)
        bounds2 (bounds-fn element2)
        anchor-pairs (if (= type :-|)
                       [[:left :top]
                        [:left :bottom]
                        [:right :top]
                        [:right :bottom]]
                       [[:top :right]
                        [:top :left]
                        [:bottom :right]
                        [:bottom :left]])]
    (->>
     (for [[anchor1 anchor2] anchor-pairs]
       [[anchor1 anchor2]
        (geom/distance-squared
         (bounds->anchor-point anchor1 bounds1)
         (bounds->anchor-point anchor2 bounds2))])
     (sort-by second)
     first    ;;get the shortest distance
     first)))

;;TODO refactor so that you pass bounds, not elements?
(defn- corner-connector [start-element end-element attrs connection-type bounds-fn]
  (let [[auto-a1 auto-a2] (corner-anchors start-element end-element connection-type bounds-fn)
        a1                (or (:from-anchor attrs) auto-a1)
        a2                (or (:to-anchor attrs) auto-a2)

        [cx1 cy1 :as p1]  (bounds->anchor-point a1 (bounds-fn start-element))
        [cx2 cy2 :as p2]  (bounds->anchor-point a2 (bounds-fn end-element))

        intersection-p    (if (= :|- connection-type)
                            [cx1 cy2]
                            [cx2 cy1])]
    (connector attrs [p1 intersection-p p2])))

(defmethod layout/layout-nodes :dali/connect
  [document tag elements bounds-fn]
  (let [make-selector   (fn [x] (if (keyword? x) [(->> x name (str "#") keyword)] x))

        connection-type (or (-> tag :attrs :type) :--)
        start-selector  (make-selector (-> tag :attrs :from))
        end-selector    (make-selector (-> tag :attrs :to))

        start-element   (first (en/select document start-selector))
        end-element     (first (en/select document end-selector))

        check-element   (fn [x selector]
                          (when-not x
                            (throw (utils/exception
                                    (format
                                     "Cannot find element '%s' referred to by connector '%s'"
                                     selector
                                     (pr-str tag))))))
        attrs           (dissoc (:attrs tag) :from :to :type :dali/path)]
    (check-element start-element (-> tag :attrs :from))
    (check-element end-element (-> tag :attrs :to))
    (if (= :-- connection-type)
      [(straight-connector start-element end-element attrs bounds-fn)]
      [(corner-connector start-element end-element attrs connection-type bounds-fn)])))
