(ns dali.test
  (:use [dali.core]
        [dali.style]
        [dali.backend]
        [dali.backend.java-2d])
  (:require [clarity.dev :as dev]))

#_(dev/watch-image #(test-dali))
(def image (ref (buffered-image [500 500])))

(defn test-dali []
  (let [triangle (polygon [50 150] [75 90] [100 150])
        my-line (line [110 100] [170 110])
        backend (image-backend @image)]
    ;(.setPaint (.graphics backend) java.awt.Color/WHITE)
    (.setRenderingHint (.graphics backend)
                       java.awt.RenderingHints/KEY_ANTIALIASING
                       java.awt.RenderingHints/VALUE_ANTIALIAS_ON)
    (doto backend
      (set-paint (color 0 0 0))
      (fill (rectangle [0 0] [(.getWidth @image) (.getHeight @image)]))
      (set-paint (color 0 220 0))
      (draw (arrow [200 50] [300 100] 20 40 30))
     
      (draw my-line)
      (draw (parallel my-line 20 :right))
      
      (draw (circle (interpolate [110 100] [170 110] 0.5) 5))
      
      (draw (line [0 0] [50 50]))
      (draw (circle [0 0] 50))
      (draw (point 120 120))
      (draw (circle [0 0] 55))
      (draw (rectangle [200 200] [100 50]))
      (fill (ellipse [200 200] [100 50]))
      (draw (rotate-around (rectangle [160 100] [60 60])
                           60
                           (center (rectangle [160 100] [60 60]))))

      (fill (circle [70 300] 30))
      (fill (rotate-around triangle 15 (center triangle)))

      (draw (polyline [50 50] [70 30] [90 50] [110 30]))
      (draw (curve [100 200] [100 0] [100 40] [50 40]))
      (draw (rounded-rect [155 305] [140 90] 20))
      (draw (path :move-to [20 200] :line-to [50 200]))
      (set-paint (color 0 100 0))
      (draw (path :move-to [175 399]
                  :quad-by [0 -20] [20 -20]
                  :line-by [100 0]
                  :quad-by [20 0] [20 20]
                  :line-by [0 50]
                  :quad-by [0 20] [-20 20]
                  :line-by [-100 0]
                  :quad-by [-20 0] [-20 -20]
                  :close))))
  @image)
