(ns dali.backend.java-2d
  (:use [dali.core]
        [dali.backend]
        [dali.utils])
  (:import [java.awt.geom CubicCurve2D$Double Path2D$Double]
           [java.awt.image BufferedImage]
           [java.awt Color BasicStroke]))

(defn polygon->params
  "Converts the geometry of the polygon to parameters appropriate for
  passing to Graphics2D.fillPolygon and related methods."
  [{{points :points} :geometry}]
  [(into-array Integer/TYPE (map first points))
   (into-array Integer/TYPE (map second points))
   (count points)])

(defn split-params-by-keyword [params]
  (->> params
       (partition-by keyword?)
       (partition-all 2)
       (map (fn [[k v]] [(first k) v]))))

(defn color->java-color [color]
  (condp = (:colorspace color)
    :rgb  (let [{:keys [r g b]} color]
            (Color. r g b))
    :rgba (let [{:keys [r g b a]} color]
            (Color. r g b a))))

(defn path->java-path [{{spec :path-spec} :geometry}]
  (let [p (Path2D$Double.)
        get-last-point (fn [x] (if (number? (last x)) x (last x)))]
    (loop [[[type v] & the-rest] (split-params-by-keyword spec)
           previous-point [0 0]]
      (if-not
          type p
          (do
            (condp = type
              :move-to (let [[[x y] ] v] (.moveTo p, x y))
              :move-by (let [[x y] (translate previous-point (first v))] (.moveTo p, x y))
              :line-to (let [[[x y]] v] (.lineTo p, x y))
              :line-by (let [[x y] (translate previous-point (first v))] (.lineTo p, x y))
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


(def cap-map {:butt java.awt.BasicStroke/CAP_BUTT
              :round java.awt.BasicStroke/CAP_ROUND
              :square java.awt.BasicStroke/CAP_SQUARE})
(def join-map {:miter java.awt.BasicStroke/JOIN_MITER
               :round java.awt.BasicStroke/JOIN_ROUND
               :bevel java.awt.BasicStroke/JOIN_BEVEL})

(defn stroke->java-stroke [stroke] ;;TODO docstring
  (let [stroke (dissoc stroke :color)]
    (when-not (empty? stroke)
      (let [{:keys [width cap join miter-limit dash dash-phase]
             :or {width 1
                  cap :round
                  join :round
                  miter-limit 10.0
                  dash nil
                  dash-phase 0}} stroke
            cap (when cap (get cap-map cap))
            join (when join (get join-map join))
            dash (when dash (into-array Float/TYPE dash))]
        (BasicStroke. width cap join miter-limit dash dash-phase)))))

(defn set-stroke
  "Sets the stroke from the optional style info attached to the
  object."
  [backend shape]
  (if-let [color (get-in shape [:style :stroke :color])]
    (set-paint backend color))
  (if-let [java-stroke (stroke->java-stroke
                        (get-in shape [:style :stroke]))]
    (.setStroke (.graphics backend) java-stroke)))

(defn set-fill
  "Sets the fill from the optional style info attached to the
  object."
  [backend shape]
  (if-let [color (get-in shape [:style :fill :color])]
    (set-paint backend color))) ;;TODO support textures etc

(defmacro isolate-style
  "Isolates the side-effects of the body to the backend, and executes
  the body in an implicit do."
  [backend & body]
  `(let [paint# (.getPaint (.graphics ~backend))
         stroke# (.getStroke (.graphics ~backend))]
     (do ~@body)
     (.setPaint (.graphics ~backend) paint#)
     (.setStroke (.graphics ~backend) stroke#)))

(defn stroke-maybe [backend shape]
  (when (has-stroke? shape)
    (isolate-style backend
      (set-stroke backend shape)
      (draw backend shape))))

(defn fill-and-stroke [backend shape]
  (when (has-fill? shape)
    (isolate-style backend
      (set-fill backend shape)
      (fill backend shape)))
  (stroke-maybe backend shape))

(deftype Java2DBackend [graphics]
  Backend
  (draw-point [this [x y]]
    (.drawLine (.graphics this) x y x y))
  (draw-line [this {{[x1 y1] :start [x2 y2] :end} :geometry}]
    (.drawLine (.graphics this) x1 y1 x2 y2))
  (draw-rectangle [this {{[px py] :position [w h] :dimensions} :geometry}]
    (.drawRect (.graphics this) px py w h))
  (draw-ellipse [this {{[px py] :position [w h] :dimensions} :geometry}]
    (.drawOval (.graphics this) px py w h))
  (draw-arc [this shape]
    )
  (draw-circle [this shape]
    (draw-ellipse this (circle->ellipse shape)))
  (draw-curve [this {{[x1 y1] :start [c1x c1y] :control1
                      [c2x c2y] :control2 [x2 y2] :end} :geometry}]
    (.draw (.graphics this) (CubicCurve2D$Double. x1 y1, c1x c1y, c2x c2y, x2 y2)))
  (draw-quad-curve [this shape])
  (draw-polyline [this shape]
    (let [[xs ys c] (polygon->params shape)]
      (.drawPolyline (.graphics this) xs ys c)))
  (draw-polygon [this shape]
    (let [[xs ys c] (polygon->params shape)]
      (.drawPolygon (.graphics this) xs ys c)))
  (draw-path [this shape]
    (.draw (.graphics this) (path->java-path shape)))

  (fill-rectangle [this {{[px py] :position [w h] :dimensions} :geometry}]
    (.fillRect (.graphics this) px py w h))
  (fill-ellipse [this {{[px py] :position [w h] :dimensions} :geometry}]
    (.fillOval (.graphics this) px py w h))
  (fill-arc [this shape])
  (fill-circle [this shape]
    (fill-ellipse this (circle->ellipse shape)))
  (fill-polygon [this shape]
    (let [[xs ys c] (polygon->params shape)]
      (.fillPolygon (.graphics this) xs ys c)))
  (fill-path [this shape]
    (.fill (.graphics this) (path->java-path shape)))

  (set-paint [this paint]
    (condp = (:type paint)
      :color (.setPaint (.graphics this) (color->java-color paint))))
  
  (render-text [this shape]) ;;TODO
  (render-point [this shape]
    (stroke-maybe this shape))
  (render-line [this shape]
    (stroke-maybe this shape))
  (render-rectangle [this shape]
    (fill-and-stroke this shape))
  (render-ellipse [this shape]
    (fill-and-stroke this shape))
  (render-arc [this shape]) ;;TODO
  (render-circle [this shape]
    (let [ell (circle->ellipse shape)]
      (fill-and-stroke this shape)))
  (render-curve [this shape]
    (stroke-maybe this shape))
  (render-quad-curve [this shape]
    (stroke-maybe this shape))
  (render-polyline [this shape]
    (stroke-maybe this shape))
  (render-polygon [this shape]
    (fill-and-stroke this shape))
  (render-path [this shape]
    (fill-and-stroke this shape))
  (render-group [this group]
    (doseq [shape (:content group)]
      (isolate-style this
        (let [merged
              (assoc shape
                :style (deep-merge (:style group)
                                   (:style shape)))] ;;shape takes precedence
          (render this merged))))))

(defn buffered-image-type [type]
  (if (keyword? type)
    (eval `(. java.awt.image.BufferedImage
              ~(symbol
                (str "TYPE_"
                     (-> type
                         (name)
                         (.replace \- \_)
                         (.toUpperCase))))))
    type))

(defn buffered-image
  ([[width height]] (buffered-image [width height] :int-rgb)) ;;TODO make compatible
  ([[width height] type]
     (BufferedImage. width height (buffered-image-type type))))

(defn image-backend
  ([img]
     (Java2DBackend. (.getGraphics img)))
  ([w h]
     (image-backend (buffered-image [w h]))))
