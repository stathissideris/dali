(ns dali.test
  (:use [dali.core]
        [dali.style]
        [dali.math]
        [dali.backend]
        [dali.backend.java-2d])
  (:require [dali.dev :as dev])
  (:import [java.awt.geom CubicCurve2D$Double Path2D$Double AffineTransform]))

#_(dev/watch-image #(test-dali))
(def img (ref (buffered-image [500 500])))

(defn mark-point [backend point]
  (draw backend (circle point 5)))

(defn test-dali [& args]
  (let [triangle (polygon [50 150] [75 90] [100 150])
        my-line (line [110 100] [170 80])

        line1 (line [10 70] [90 80])
        line2 (line [10 120] [90 60])
        line3 (line [20 120] [20 60])

        cloud-icon (load-image (image-data :file "data/cloud_icon.png"))
        texture (load-image (image-data :file "data/texture.png"))

        backend (image-backend @img)]
    ;(.setPaint (.graphics backend) java.awt.Color/WHITE)
    (.setRenderingHint (.graphics backend)
                       java.awt.RenderingHints/KEY_ANTIALIASING
                       java.awt.RenderingHints/VALUE_ANTIALIAS_ON)

    #_(println
       (.getStringBounds
        dali.backend.java-2d/*DEFAULT-FONT*
        "Stathis"
        (.getFontRenderContext (.graphics backend))))

    #_(println (text-bounds backend (text [100 100] "Stathis")))
    
    (doto backend
      (set-paint (color 0 0 0))
      (fill (rectangle [0 0] [(.getWidth @img) (.getHeight @img)]))
      (set-paint (color 0 220 0)))

    (doto backend
      (render-text (text {:fill (color 150 0 150)
                          :transform [:translate #(minus (center %))
                                      :rotate 30
                                      :translate #(center %)]}
                         [185 25] "Testing the dali library"))
      (render (arrow
               {:stroke {:width 2
                         :color (color 255 255 255)}
                :fill (color 150 20 20)}
               [200 50] [300 80] 20 40 30))

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
      (render
       (rectangle {:stroke {:width 8
                            :color (color 0 130 0)
                            :join :miter}
                   :transform [:translate #(minus (center %))
                               :skew [0.2 0.2]
                               :scale 0.8
                               :rotate 30
                               :translate center]}
                  [330 170] [100 70]))

      (draw (rotate-around (rectangle [160 100] [60 60])
                           60
                           (center (rectangle [160 100] [60 60]))))

      (draw (circle [70 300] 50))
      (render
       (circle
        {:stroke {:color (color 0 0 0)}
         :fill (radial-gradient [60 290] 35
                                0.2 (color 100 230 100)
                                1.0 (color 0 60 0))}
        [70 300] 30))

      (fill (rotate-around triangle 15 (center triangle)))

      (draw (polyline [400 50] [420 30] [440 50] [460 30]))
      (draw (parallel (polyline [400 50] [420 30] [440 50] [460 30]) 5 :right))
      (draw (parallel (polyline [400 50] [420 30] [440 50] [460 30]) 10 :left))

      (draw (polygon [430 100] [420 150] [460 150]))
      (draw (parallel (polygon [430 100] [420 150] [460 150]) 5 :right))
      (draw (parallel (polygon [430 100] [420 150] [460 150]) 10 :left))
      
      (draw (curve [100 200] [100 0] [100 40] [50 40]))
      (render
       (group {:stroke {:color (color 255 255 255)
                        :width 3}
               :fill   (color 120 120 200)
               }
        (rounded-rect
         {:stroke {:dash [10 15]}
          :fill (linear-gradient
                 [155 305] [170 395]
                 0.2 (color 100 230 100)
                 0.5 (color 0 170 0)
                 0.9 (color 0 60 0))}
         [175 290] [140 90] 20)
        (rounded-rect
         {;:fill (fn [_] (let [r (int (rand 160))] (color r 0 0)))
          }
         [370 290] [40 90] 10)

        (arrow {:stroke {:width 2}}
               (translate
                (center (right-bound (rectangle [175 290] [140 90])))
                [10 0])
               (translate
                (center (left-bound (rectangle [370 290] [40 90])))
                [-10 0])
               12 28 20)))

      (render
       (path {:stroke {:color (color 220 220 220)
                       :width 2}
              :fill (image-texture texture)}
             :move-to [175 415]
             :quad-by [0 -20] [20 -20]
             :line-by [100 0]
             :quad-by [20 0] [20 20]
             :line-by [0 50]
             :quad-by [0 20] [-20 20]
             :line-by [-100 0]
             :quad-by [-20 0] [-20 -20]
             :close))

      (render (image cloud-icon [40 410])))

      ;(draw (path :move-to [20 200] :line-to [50 200]))
      ;(set-paint (color 0 100 0))


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
  @img)
