(ns dali.backend)

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
  (render-shape [this shape]))
