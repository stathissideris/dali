(ns dali.backend.java-2d
  (:require [clojure.java.io :as io])
  (:use [dali.core]
        [dali.backend]
        [dali.utils]
        [dali.math]
        [dali.defaults]
        [dali.style])
  (:import [java.awt.geom CubicCurve2D$Double Path2D$Double AffineTransform
            Point2D$Float Point2D$Double Rectangle2D$Double]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]
           [java.awt Color BasicStroke LinearGradientPaint RadialGradientPaint TexturePaint Font]))

(defn get-laf-property
  [key]
  (javax.swing.UIManager/get key))

(def *DEFAULT-FONT* (get-laf-property "TextField.font"))

(defn point-float [x y] (Point2D$Float. x y))
(defn point-double [x y] (Point2D$Double. x y))

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
  "Sets the stroke."
  [backend stroke]
  (if-let [color (:color stroke)]
    (set-paint backend color))
  (if-let [java-stroke (stroke->java-stroke stroke)]
    (.setStroke (.graphics backend) java-stroke)))

(defn set-fill
  "Sets the fill from the optional style info attached to the
  object."
  [backend fill]
  (set-paint backend fill)) ;;TODO support textures etc

(let [cycle-method-map
      {:no-cycle java.awt.MultipleGradientPaint$CycleMethod/NO_CYCLE
       :repeat java.awt.MultipleGradientPaint$CycleMethod/REPEAT
       :reflect java.awt.MultipleGradientPaint$CycleMethod/REFLECT}]

  (defn linear-gradient->java-gradient [gr]
    (let [stops (partition 2 (:stops gr))]
      (LinearGradientPaint.
       (apply point-float (:start gr))
       (apply point-float (:end gr))
       (into-array Float/TYPE (map first stops))
       (into-array java.awt.Color (map (comp color->java-color second) stops))
       (cycle-method-map (:cycle-method gr)))))

  (defn radial-gradient->java-gradient [gr]
    (let [stops (partition 2 (:stops gr))]
      (if (:focus-point gr)
        (RadialGradientPaint. ;;with off-center focus
         (apply point-float (:center gr))
         (float (:radius gr))
         (apply point-float (:focus-point gr))
         (into-array Float/TYPE (map first stops))
         (into-array java.awt.Color (map (comp color->java-color second) stops))
         (cycle-method-map (:cycle-method gr)))
        (RadialGradientPaint. ;;no off-center focus
         (apply point-float (:center gr))
         (float (:radius gr))
         (into-array Float/TYPE (map first stops))
         (into-array java.awt.Color (map (comp color->java-color second) stops))
         (cycle-method-map (:cycle-method gr)))))))

(defn gradient->java-gradient [gr]
  (condp = (:type gr)
    :linear-gradient (linear-gradient->java-gradient gr)
    :radial-gradient (radial-gradient->java-gradient gr)))

(defn rectangle->java-rectangle
  [{{[x y] :position [w h] :dimensions} :geometry}]
  (Rectangle2D$Double. x y w h))

(defn image-texture->java-paint [txt]
  (TexturePaint.
   (get-in txt [:image :data])
   (rectangle->java-rectangle (:anchor txt))))

(defn transform->java-transform [tr]
  (let [set-transform (fn [tr a1 a2 a3 a4 a5 a6]
                        (.setTransform tr a1 a2 a3 a4 a5 a6))
        res (AffineTransform.)]
    (doall
     (map
      (fn [[dir v]]
        [(condp = dir
           :scale (if (number? v)
                    (doto res (.scale v v))                   
                    (doto res (.scale (first v) (second v))))
           :skew (doto res (.shear (first v) (second v)))
           :translate (doto res (.translate (first v) (second v)))
           :rotate (if (number? v)
                     (doto res (.rotate (degrees->radians v)))
                     (let [[a [x y :as p]] v]
                       (doto res (.rotate (degrees->radians a) x y))))
           :matrix (do (apply set-transform res v) res))
         rest])
      (reverse (partition 2 tr))))
    res))

(defmacro with-java-transform [backend tr & body]
  `(let [gfx# (.graphics ~backend)
         old# (.getTransform gfx#)]
     (.setTransform gfx# ~tr)
     ~@body
     (.setTransform gfx# old#)))

(defmacro with-transform [backend tr & body]
  `(let [gfx# (.graphics ~backend)
         old# (.getTransform gfx#)]
     (.setTransform gfx# (transform->java-transform ~tr))
     ~@body
     (.setTransform gfx# old#)))

(defn font->java-font
  [{family :family ;;TODO style and weight
    size :size
    :or {family (.getFamily *DEFAULT-FONT*)
         size (.getSize *DEFAULT-FONT*)}}]
  (Font. family java.awt.Font/PLAIN size))

(defn text-bounds-java-2d
  [backend {content :content {position :position} :geometry :as text}]
  (let [font (font->java-font (get-in text [:style :font]))
        rect (.getStringBounds
              font content
              (.getFontRenderContext (.graphics backend)))
        upper-left [(.getX rect) (.getY rect)]]
    (rectangle (translate position upper-left)
               [(.getWidth rect) (.getHeight rect)])))

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
  (let [st (if (has-stroke? shape) (:stroke shape) DEFAULT-STROKE)]
    (isolate-style backend
     (set-stroke backend (eval-dynamic-style
                          backend
                          shape
                          (get-in shape [:style :stroke])))
     (if (has-transform? shape)       
       (with-transform backend (eval-dynamic-style
                                backend
                                shape
                                (:transform shape))
         (draw backend shape))
       (draw backend shape)))))

(defn fill-and-stroke [backend shape]
  (when (has-fill? shape)
    (isolate-style backend
      (set-fill backend (eval-dynamic-style
                         backend
                         shape
                         (get-in shape [:style :fill])))
      (if (has-transform? shape)
        (with-transform backend (eval-dynamic-style
                                 backend
                                 shape
                                 (:transform shape))          
          (fill backend shape))
        (fill backend shape))))
  (stroke-maybe backend shape)) ;;TODO maybe inline this call

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
      :color (.setPaint (.graphics this) (color->java-color paint))
      :linear-gradient (.setPaint (.graphics this) (gradient->java-gradient paint))
      :radial-gradient (.setPaint (.graphics this) (gradient->java-gradient paint))
      :image-texture (.setPaint (.graphics this) (image-texture->java-paint paint))))

  (render-text [this {{[x y] :position} :geometry, txt :content, style :style :as shape}]
    (let [render-fn
          #(.drawString (.graphics this) (str txt) (float x) (float y))]
      (when (:font style)
        (.setFont (.graphics this) (font->java-font (:font style))))
      (if (has-fill? shape)
        (isolate-style this
          (set-paint this (eval-dynamic-style
                           this shape (get-in shape [:style :fill])))
          (if (has-transform? shape)
            (with-transform this (eval-dynamic-style
                                  this shape (:transform shape))          
              (render-fn))
            (render-fn)))
        (render-fn)))) ;;TODO
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
  (render-image [this {{data :data} :data-map {[x y] :position} :geometry :as shape}]
    (if (has-transform? shape)
      (with-transform this (eval-dynamic-style
                            this shape (:transform shape))
        (.drawImage (.graphics this) data x y nil))
      (.drawImage (.graphics this) data x y nil)))
  (render-group [this group]
    (doseq [shape (:content group)]
      (isolate-style this
        (let [merged
              (assoc shape
                :style (deep-merge (:style group)
                                   (:style shape)))] ;;shape takes precedence
          (render this merged)))))
  (text-bounds [this shape]
    (text-bounds-java-2d this shape)))

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

(defn load-image
  ([info]
     (let [data (ImageIO/read (io/as-file (:filename info))) ;;TODO URL?
           dim [(.getWidth data) (.getHeight data)]]
       (-> info
           (assoc :data data)
           (assoc :dimensions dim)))))

(defn image-backend
  ([img]
     (Java2DBackend. (.getGraphics img)))
  ([w h]
     (image-backend (buffered-image [w h]))))
