(ns dali.core
  (:require [clarity.graphics :as gfx]
            [clarity.dev :as dev])
  (:use [dali.math])
  (:import [java.awt.geom CubicCurve2D$Double Path2D$Double]))

(defn point [x y] [x y])

(defn dimensions [w h] [w h])

(defmacro defshape [shape & geometry-components]
  `(defn ~shape [~@geometry-components]
     {::type ~(keyword (str *ns*) (str shape))
      ::geometry ~(zipmap (map (fn [x] (keyword (str *ns*) (str x)))
                               geometry-components)
                          geometry-components)}))

(defshape line start end)
(defshape rectangle position dimensions)
(defshape ellipse position dimensions)
(defshape arc position dimensions angles)
(defshape circle center radius)
(defshape curve start control1 control2 end)
(defshape quad-curve start control end)

(defn text [position text]
  {::type ::text,
   ::geometry {::position position},
   ::content text})

(defn polyline [& points]
  {::type ::polyline,
   ::geometry {::points points}})

(defn polygon [& points]
  {::type ::polygon,
   ::geometry {::points points}})

(defn polygon->params
  "Converts the geometry of the polygon to parameters appropriate for
  passing to Graphics2D.fillPolygon and related methods."
  [{{points ::points} ::geometry}]
  [(into-array Integer/TYPE (map first points))
   (into-array Integer/TYPE (map second points))
   (count points)])

(defn rectangle->polygon
  [{{[px py] ::position [w h] ::dimensions} ::geometry :as shape}]
  (-> shape
      (assoc ::type ::polygon)
      (assoc ::geometry
        {::points [[px py] [(+ px w) py] [(+ px w) (+ py h)] [px (+ py h)]]})))

(defn path [& path-spec]
  {::type ::path
   ::geometry {::path-spec path-spec}})

;;; example:
;; (path :move-to [10 10]
;;       :line-to [20 20]
;;       :curve-to [[40 20] [40 100] [20 100]]
;;       :quad-to [[20 10] [0 0]]
;;       :close)

(defn shape-type [shape] (if (vector? shape) ::point (::type shape)))

;;;;;;;; Translate ;;;;;;;;

(defmulti translate (fn [shape delta] (shape-type shape)))

(defmethod translate ::point
  [shape delta]
  (vec (map + shape delta)))

(defmethod translate ::line
  [{{start ::start end ::end} ::geometry :as shape} delta]
  (-> shape
      (assoc-in [::geometry ::start] (translate start delta))
      (assoc-in [::geometry ::end] (translate end delta))))

(defmethod translate ::rectangle
  [{{position ::position} ::geometry :as shape} delta]
  (assoc-in shape [::geometry ::position] (translate position delta)))

(defmethod translate ::ellipse
  [{{position ::position} ::geometry :as shape} delta]
  (assoc-in shape [::geometry ::position] (translate position delta)))

(defmethod translate ::circle
  [{{c ::center} ::geometry :as shape} delta]
  (assoc-in shape [::geometry ::center] (translate c delta)))

(defmethod translate ::polygon
  [{{points ::points} ::geometry :as shape} delta]
  (assoc-in shape [::geometry ::points] (map #(translate % delta) points)))

(defmethod translate ::polyline
  [{{points ::points} ::geometry :as shape} delta]
  (assoc-in shape [::geometry ::points] (map #(translate % delta) points)))

(defmethod translate ::curve
  [{geometry ::geometry :as shape} delta]
  (assoc shape ::geometry
         (zipmap (keys geometry) (map #(translate % delta) (vals geometry)))))

(defmethod translate ::quad-curve
  [{geometry ::geometry :as shape} delta]
  (assoc shape ::geometry
         (zipmap (keys geometry) (map #(translate % delta) (vals geometry)))))

;;;;;;;; Advanced shapes ;;;;;;;;

(defn path->java-path [{{spec ::path-spec} ::geometry}]
  (let [p (Path2D$Double.)
        get-last-point (fn [x] (if (number? (last x)) x (last x)))]
    (loop [[type v & the-rest] spec
           previous-point [0 0]]
      (if-not
          type p
          (do
            (condp = type
              :move-to (let [[x y] v] (.moveTo p, x y))
              :move-by (let [[x y] (translate previous-point v)] (.moveTo p, x y))
              :line-to (let [[x y] v] (.lineTo p, x y))
              :line-by (let [[x y] (translate previous-point v)] (.lineTo p, x y))
              :quad-to (let [[[cx cy] [x2 y2]] v]
                         (.quadTo p, cx cy, x2 y2))
              :quad-by (let [[[cx cy] [x2 y2]]
                             (map (partial translate previous-point) v)]
                         (.quadTo p, cx cy, x2 y2))
              :curve-to (let [[[c1x c1y] [c2x c2y] [x2 y2]] v]
                          (.curveTo p, c1x c1y, c2x c2y, x2 y2))
              :curve-by (let [[[c1x c1y] [c2x c2y] [x2 y2]]
                              (map (partial translate previous-point) v)]
                          (.curveTo p, c1x c1y, c2x c2y, x2 y2))
              :close (.closePath p))
            (recur the-rest (translate previous-point
                                       (get-last-point v))))))))

(defn rounded-rect [[px py] [w h] r]
  (let [internal-w (- w (* 2 r))
        internal-h (- h (* 2 r))]
   (path :move-to [(+ px r) py]
         :quad-by [[0 (- r)] [r (- r)]]
         :line-by [internal-w 0]
         :quad-by [[r 0] [r r]]
         :line-by [0 internal-h]
         :quad-by [[0 r] [(- r) r]]
         :line-by [(- internal-w) 0]
         :quad-by [[(- r) 0] [(- r) (- r)]]
         :close)))

;;;;;;;; Rotate ;;;;;;;;

(defmulti rotate (fn [shape angle] (shape-type shape)))

(defmethod rotate ::point
  [[x y] a]
  [(- (* x (cos a)) (* y (sin a)))
   (+ (* x (sin a)) (* y (cos a)))])

(defmethod rotate ::line
  [{{start ::start end ::end} ::geometry :as shape} a]
  (-> shape
      (assoc-in [::geometry ::start] (rotate start a))
      (assoc-in [::geometry ::end] (rotate end a))))

(defmethod rotate ::circle
  [{{c ::center} ::geometry :as shape} a]
  (assoc-in shape [::geometry ::center] (rotate c a)))

;; TODO ellipse????

(defmethod rotate ::polygon
  [shape angle]
  (let [{{points ::points} ::geometry} shape]
   (assoc-in shape [::geometry ::points] (map #(rotate % angle) points))))

(defmethod rotate ::rectangle
  [shape angle]
  (rotate (rectangle->polygon shape) angle))

(defmethod rotate ::polyline
  [shape angle]
  (let [{{points ::points} ::geometry} shape]
   (assoc-in shape [::geometry ::points] (map #(rotate % angle) points))))

(defmethod rotate ::curve
  [{geometry ::geometry :as shape} angle]
  (assoc shape ::geometry
         (zipmap (keys geometry) (map #(rotate % angle) (vals geometry)))))

(defmethod rotate ::quad-curve
  [{geometry ::geometry :as shape} angle]
  (assoc shape ::geometry
         (zipmap (keys geometry) (map #(rotate % angle) (vals geometry)))))

(defn rotate-around
  [shape angle rotation-center]
  (-> shape
      (translate (minus rotation-center))
      (rotate angle)
      (translate rotation-center)))

;;;;;;;; Center ;;;;;;;;

(defmulti center shape-type)

(defmethod center ::point
  [shape] shape)

(defmethod center ::line
  [{{[x1 y1] ::start [x2 y2] ::end} ::geometry}]
  [(+ (min x1 x2) (/ (abs (- x1 x2)) 2))
   (+ (min y1 y2) (/ (abs (- y1 y2)) 2))])

(defmethod center ::rectangle
  [{{[px py] ::position [w h] ::dimensions} ::geometry}]
  [(+ px (/ w 2)) (+ py (/ h 2))])

(defmethod center ::ellipse
  [{{[px py] ::position [w h] ::dimensions} ::geometry}]
  [(+ px (/ w 2)) (+ py (/ h 2))])

(defmethod center ::circle
  [shape]
  (get-in ::geometry ::center))

(defmethod center ::polygon
  [{{points ::points} ::geometry}]
  (let [c (count points)]
   [(/ (apply + (map first points)) c)
    (/ (apply + (map second points)) c)]))

(defmethod center ::polyline
  [{{points ::points} ::geometry}]
  (let [c (count points)]
   [(/ (apply + (map first points)) c)
    (/ (apply + (map second points)) c)]))

;;;;;;;; Draw ;;;;;;;;

(defmulti draw (fn [context shape] (shape-type shape)))

(defn circle->ellipse [shape]
  (let [{{c ::center r ::radius} ::geometry} shape]
    (ellipse (translate c [(- r) (- r)])
             [(* 2 r) (* 2 r)])))

(defmethod draw ::point
  [context shape]
  (let [[x y] shape]
    (.drawLine context x y x y)))

(defmethod draw ::line
  [context shape]
  (let [{{[x1 y1] ::start [x2 y2] ::end} ::geometry} shape]
    (.drawLine context x1 y1 x2 y2)))

(defmethod draw ::rectangle
  [context shape]
  (let [{{[px py] ::position [w h] ::dimensions} ::geometry} shape]
    (.drawRect context px py w h)))

(defmethod draw ::ellipse
  [context shape]
  (let [{{[px py] ::position [w h] ::dimensions} ::geometry} shape]
    (.drawOval context px py w h)))

(defmethod draw ::circle
  [context shape]
  (draw context (circle->ellipse shape)))

(defmethod draw ::polyline
  [context shape]
  (let [[xs ys c] (polygon->params shape)]
    (.drawPolyline context xs ys c)))

(defmethod draw ::polygon
  [context shape]
  (let [[xs ys c] (polygon->params shape)]
    (.drawPolygon context xs ys c)))

(defmethod draw ::curve
  [context
   {{[x1 y1] ::start [c1x c1y] ::control1
     [c2x c2y] ::control2 [x2 y2] ::end} ::geometry}]
  (.draw context (CubicCurve2D$Double. x1 y1, c1x c1y, c2x c2y, x2 y2)))

(defmethod draw ::path
  [context shape]
  (.draw context (path->java-path shape)))

;;;;;;;; Fill ;;;;;;;;

(defmulti fill (fn [context shape] (shape-type shape)))

(defmethod fill ::rectangle
  [context shape]
  (let [{{[px py] ::position [w h] ::dimensions} ::geometry} shape]
    (.fillRect context px py w h)))

(defmethod fill ::ellipse
  [context shape]
  (let [{{[px py] ::position [w h] ::dimensions} ::geometry} shape]
    (.fillOval context px py w h)))

(defmethod fill ::circle
  [context shape]
  (fill context (circle->ellipse shape)))

(defmethod fill ::polygon
  [context shape]
  (let [[xs ys c] (polygon->params shape)]
    (.fillPolygon context xs ys c)))

(defmethod fill ::path
  [context shape]
  (.fill context (path->java-path shape)))

#_(dev/watch-image #(test-dali))
(defonce img (ref (gfx/buffered-image [500 500])))

(defn test-dali []
  (let [triangle (polygon [50 150] [75 90] [100 150])]
   (doto (gfx/graphics @img)
     (.setRenderingHint java.awt.RenderingHints/KEY_ANTIALIASING
                        java.awt.RenderingHints/VALUE_ANTIALIAS_ON)
     (.setPaint java.awt.Color/BLACK)
     (fill (rectangle [0 0] [(.getWidth @img) (.getHeight @img)]))
     (.setPaint java.awt.Color/WHITE)
     (draw (line [0 100] [200 50]))
     (draw (line [0 0] [50 50]))
     (draw (circle [0 0] 50))
     (draw (point 120 120))
     (draw (circle [0 0] 55))
     (draw (rotate-around (rectangle [160 100] [60 60])
                          30
                          (center (rectangle [160 100] [60 60]))))
     (fill (rectangle [160 160] [60 60]))
     (fill (circle [70 300] 60))
     (draw (polyline [50 50] [70 30] [90 50] [110 30]))
     (draw (curve [0 0] [100 0] [100 40] [0 40]))
     (fill (rotate-around triangle 10 (center triangle)))
     (draw (rounded-rect [155 305] [140 90] 20))
     (draw (path :move-to [170 300]
                 :quad-by [[0 -20] [20 -20]]
                 :line-by [100 0]
                 :quad-by [[20 0] [20 20]]
                 :line-by [0 50]
                 :quad-by [[0 20] [-20 20]]
                 :line-by [-100 0]
                 :quad-by [[-20 0] [-20 -20]]
                 :close))))
  @img)
