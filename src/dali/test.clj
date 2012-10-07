(ns dali.test
  (:use [dali.core]
        [dali.style]
        [dali.backend]
        [dali.backend.java-2d])
  (:require [clarity.dev :as dev]))

#_(dev/watch-image #(test-dali))
(def image (ref (buffered-image [500 500])))

(defn mark-point [backend point]
  (draw backend (circle point 5)))

(defn test-dali []
  (let [triangle (polygon [50 150] [75 90] [100 150])
        my-line (line [110 100] [170 110])

        line1 (line [10 70] [90 90])
        line2 (line [10 120] [90 60])
        line3 (line [20 120] [20 60])
        
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

      (draw line1)
      (draw line2)
      (draw line3)
      (mark-point (line-intersection line1 line2))
      (mark-point (line-intersection line1 line3))
      (mark-point (line-intersection line2 line3))
      
      (draw my-line)
      (draw (parallel my-line 20 :right))
      
      ;(mark-point (interpolate [110 100] [170 110] 0.5))
      
      (draw (line [0 0] [50 50]))
      (draw (circle [0 0] 50))
      (draw (point 120 120))
      (draw (circle [0 0] 55))
      (draw (rectangle [200 200] [100 50]))

      (draw (rotate-around (rectangle [160 100] [60 60])
                           60
                           (center (rectangle [160 100] [60 60]))))

      (fill (circle [70 300] 30))
      (fill (rotate-around triangle 15 (center triangle)))

      (draw (polyline [50 50] [70 30] [90 50] [110 30]))
      (draw (curve [100 200] [100 0] [100 40] [50 40]))

      (render
       (group {:stroke {:color (color 255 255 255)
                        :width 3}
               :fill   {:color (color 0 100 0)}}
        (rounded-rect
         {:fill {:color (color 128 0 0)}}
         [315 295] [40 90] 10)
        (rounded-rect
         {:stroke {:dash [10 15]}}
         [155 305] [140 90] 20)))

      ;(draw (path :move-to [20 200] :line-to [50 200]))
      ;(set-paint (color 0 100 0))
      (draw (path :move-to [175 420]
                  :quad-by [0 -20] [20 -20]
                  :line-by [100 0]
                  :quad-by [20 0] [20 20]
                  :line-by [0 50]
                  :quad-by [0 20] [-20 20]
                  :line-by [-100 0]
                  :quad-by [-20 0] [-20 -20]
                  :close)))
    
    (let [r (rectangle [200 200] [100 50])
          l1 (line [220 180] [280 270])
          l2 (line [180 210] [320 240])
          intersections1 (line-rectangle-intersection l1 r)
          intersections2 (line-rectangle-intersection l2 r)]
      (doto backend
        (set-paint (color 0 220 0))
        (draw r)
        (draw l1)
        (draw l2))
      (doseq [i (concat intersections1 intersections2)]
        (mark-point backend i))))
  @image)
