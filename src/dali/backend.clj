(ns dali.backend
  (:use dali.core))

(defprotocol Backend
  (draw-point [this shape])
  (draw-line [this shape])
  (draw-rectangle [this shape])
  (draw-ellipse [this shape])
  (draw-arc [this shape])
  (draw-circle [this shape])
  (draw-curve [this shape])
  (draw-quad-curve [this shape])
  (draw-polyline [this shape])
  (draw-polygon [this shape])
  (draw-path [this shape])

  (fill-rectangle [this shape])
  (fill-ellipse [this shape])
  (fill-arc [this shape])
  (fill-circle [this shape])
  (fill-polygon [this shape])
  (fill-path [this shape])

  (set-paint [this paint])
  
  (render-text [this shape])
  (render-point [this shape])
  (render-line [this shape])
  (render-rectangle [this shape])
  (render-ellipse [this shape])
  (render-arc [this shape])
  (render-circle [this shape])
  (render-curve [this shape])
  (render-quad-curve [this shape])
  (render-polyline [this shape])
  (render-polygon [this shape])
  (render-path [this shape])
  (render-image [this shape])

  (render-group [this shape]))


(defmacro delegate-op-to-backend [op & shape-types]
  `(do ~@(map
          (fn [t]
            `(defmethod ~op ~t
               [~'backend ~'shape]
               (~(symbol (str op "-" (name t))) ~'backend ~'shape)))
          shape-types)))

(defmulti draw (fn [context shape] (shape-type shape)))
(delegate-op-to-backend
 draw :point :line :rectangle :ellipse :circle :curve :polyline :polygon :path)

(defmulti fill (fn [context shape] (shape-type shape)))
(delegate-op-to-backend
 fill :rectangle :ellipse :circle :polygon :path)

(defmulti render (fn [context shape] (shape-type shape)))
(delegate-op-to-backend
 render :point :line :rectangle :ellipse :circle :curve :polyline :polygon :path :image :group)
