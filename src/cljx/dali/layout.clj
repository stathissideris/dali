(ns cljx.dali.layout
  (:require [clojure.walk :as walk]
            [retrograde :as retro]
            [dali.syntax :as s]
            [dali.batik :as batik]
            [dali.geom :as geom :refer [v+ v- v-half]]))

(def anchors #{:top-left :top-middle :top-right :middle-left :middle-right :bottom-left :bottom-middle :bottom-right :center})

(defn bounds->anchor-point
  [anchor [_ [x y] [w h]]]
  (condp = anchor
    :top-left      [x y]
    :top-middle    [(+ x (/ w 2)) y]
    :top-right     [(+ x w) y]
    :middle-left   [x (+ y (/ h 2))]
    :middle-right  [(+ x w) (+ y (/ h 2))]
    :bottom-left   [x (+ y h)]
    :bottom-middle [(+ x (/ w 2)) (+ y h)]
    :bottom-right  [(+ x w) (+ y h)]
    :center        [(+ x (/ w 2)) (+ y (/ h 2))]))

(defn- replace-blanks [element replacement]
  (walk/postwalk (fn [f] (if (= f :_) replacement f)) element))

(defn place-top-left
  "Adds a translation transform to an element so that its top-left
  corner is at the passed position."
  [element top-left bounds]
  (let [type (first element)
        [_ current-pos [w h]] bounds
        transform
        (condp = type
          :circle (v- top-left current-pos)
          :text top-left ;;TODO
          (v- top-left current-pos))]
    (s/add-transform element [:translate transform])))

(defn place-by-anchor
  [element anchor position bounds]
  (let [[_ original-position] bounds
        anchor-point (bounds->anchor-point anchor bounds)]
    (place-top-left
     element
     (v- position (v- anchor-point original-position))
     bounds)))

(defn stack [ctx {:keys [position direction anchor gap] :as params} & elements]
  (let [gap (or gap 2)
        anchor (or anchor :top-middle)
        [x y] position
        elements (map #(replace-blanks % [0 0]) elements)]
    (into [:g]
     (retro/transform
      [this-gap 0 gap
       bounds nil (batik/rehearse-bounds ctx element)
       size 0 (let [[_ _ [_ h]] bounds] h)
       pos 0 (let [[_ [_ y] _] bounds] y)
       this-pos y (+ this-pos' size' this-gap')
       element (place-by-anchor element anchor [x this-pos] bounds)]
      elements))))

(comment
  (distribute
   ctx
   {:position [10 10] :direction :qright}
   [:circle :_ 10]
   [:circle :_ 20]
   [:circle :_ 50]))

(comment
  (distribute
   ctx
   {:position [10 10] :direction :right}
   (take
    20
    (cycle
     [[:circle :_ 10]
      [:rect :_ [10 10]]]))))

(comment
  (distribute
   ctx
   {:position [10 10] :direction :left}
   (interleave
    [:circle :_ 10]
    (repeat [:rect :_ [10 10]]))))

(comment
  (distribute
   ctx
   {:position [10 10] :anchor :bottom-center}
   (map (fn [x] [:rect :_ [10 x]])
        [50 60 34 22 55 10 12 19])))

(comment
  (def ctx (batik/batik-context (batik/parse-svg-uri "file:///s:/temp/svg.svg") :dynamic? true))

  (defn marker [pos] [:circle pos 3])
  
  (defn anchor-box [ctx pos anchor]
    [:g
     (marker pos)
     (let [box [:rect [-100 -100] [25 25]]]
       (place-by-anchor box anchor pos
                        (batik/rehearse-bounds ctx box)))])

  (defn anchor-circle [ctx pos anchor]
    [:g
     (marker pos)
     (let [c [:circle [-100 -100] 12.5]]
       (place-by-anchor c anchor pos
                        (batik/rehearse-bounds ctx c)))])

  (defn make-stack [ctx pos anchor]
    [:g
     (marker pos)
     (stack
      ctx
      {:position pos :gap 5 :anchor anchor}
      [:rect :_ [10 100]]
      [:circle :_ 15]
      [:rect :_ [20 20]]
      [:rect :_ [10 30]]
      [:rect :_ [10 5]]
      [:rect :_ [10 5]])])
  
  (s/spit-svg
   (s/dali->hiccup
    [:page
     {:height 700 :width 500, :stroke {:paint :black :width 1} :fill :none}

     ;;test that top-left works
     [:rect [300 50] [100 100]]
     (place-top-left
      [:circle [-100 -100] 50]
      [300 50]
      (batik/rehearse-bounds ctx [:circle [-100 -100] 50]))

     ;;test that top-left works
     (marker [250 200])
     (place-top-left
      [:rect [-100 -100] [25 25]]
      [250 200]
      (batik/rehearse-bounds ctx [:rect [-100 -100] [50 50]]))

     ;;test all cases of place-by-anchor
     (anchor-box ctx [300 200] :top-left)
     (anchor-box ctx [350 200] :top-middle)
     (anchor-box ctx [400 200] :top-right)
     (anchor-box ctx [300 250] :middle-left)
     (anchor-box ctx [350 250] :center)
     (anchor-box ctx [400 250] :middle-right)
     (anchor-box ctx [300 300] :bottom-left)
     (anchor-box ctx [350 300] :bottom-middle)
     (anchor-box ctx [400 300] :bottom-right)

     (anchor-circle ctx [300 325] :top-left)
     (anchor-circle ctx [350 325] :top-middle)
     (anchor-circle ctx [400 325] :top-right)
     (anchor-circle ctx [300 375] :middle-left)
     (anchor-circle ctx [350 375] :center)
     (anchor-circle ctx [400 375] :middle-right)
     (anchor-circle ctx [300 425] :bottom-left)
     (anchor-circle ctx [350 425] :bottom-middle)
     (anchor-circle ctx [400 425] :bottom-right)

     (make-stack ctx [50 50] :top-left)
     (make-stack ctx [120 50] :top-middle)
     (make-stack ctx [190 50] :top-right)])
   "s:/temp/svg_stack1.svg")
  )
