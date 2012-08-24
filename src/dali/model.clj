(ns dali.model
  (:require [clarity.graphics :as gfx]
            [clarity.dev :as dev]))

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

(defn shape-type [shape] (if (vector? shape) ::point (::type shape)))

(defmulti translate (fn [shape delta] (shape-type shape)))

(defmethod translate ::point
  [shape delta]
  (vec (map + shape delta)))

(defmulti stroke (fn [context shape] (shape-type shape)))

(defmethod stroke ::point
  [context point]
  (let [[x y] point]
    (.drawLine context x y x y)))

(defmethod stroke ::line
  [context line]
  (let [{{[x1 y1] ::start [x2 y2] ::end} ::geometry} line]
    (.drawLine context x1 y1 x2 y2)))

(defmethod stroke ::rectangle
  [context rectangle]
  (let [{{[px py] ::position [w h] ::dimensions} ::geometry} rectangle]
    (.drawRect context px py w h)))

(defmethod stroke ::ellipse
  [context ellipse]
  (let [{{[px py] ::position [w h] ::dimensions} ::geometry} ellipse]
    (.drawOval context px py w h)))

(defmethod stroke ::circle
  [context circle]
  (let [{{c ::center r ::radius} ::geometry} circle]
    (stroke (ellipse (translate c [(- r) (- r)])
                     [(* 2 r) (* 2 r)]))))

(defmethod stroke ::polyline
  [context poly]
  (let [{{points ::points} ::geometry} poly]
    (doall
     (map (fn [p1 p2] stroke (line p1 p2))
          (partition 2 1 points)))))

(defmethod stroke ::polygon
  [context poly]
  (let [{{points ::points} ::geometry} poly]
    (doall
     (map (fn [p1 p2] stroke (line p1 p2))
          (take (inc (count points))
                (partition 2 1 (cycle points)))))))

(defn test-dali []
  (let [img (gfx/buffered-image [500 500])]
    (doto (gfx/graphics img)
      (stroke (line [100 100] [200 150])))
    (dev/watch-image (atom img))))

#_(test-dali)
