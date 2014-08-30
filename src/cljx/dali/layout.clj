(ns cljx.dali.layout
  (:require [clojure.walk :as walk]
            [retrograde :as retro]
            [dali.syntax :as s]
            [dali.batik :as batik]
            [dali.geom :as geom :refer [v+ v- v-half]]))

(def anchors #{:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :center})

(def direction->default-anchor
  {:down :top
   :up :bottom
   :right :left
   :left :right})

(defn bounds->anchor-point
  [anchor [_ [x y] [w h]]]
  (condp = anchor
    :top-left     [x y]
    :top          [(+ x (/ w 2)) y]
    :top-right    [(+ x w) y]
    :left         [x (+ y (/ h 2))]
    :right        [(+ x w) (+ y (/ h 2))]
    :bottom-left  [x (+ y h)]
    :bottom       [(+ x (/ w 2)) (+ y h)]
    :bottom-right [(+ x w) (+ y h)]
    :center       [(+ x (/ w 2)) (+ y (/ h 2))]))

(defn- replace-blanks [element replacement]
  (walk/postwalk (fn [f] (if (= f :_) replacement f)) element))

(defn place-top-left
  "Adds a translation transform to an element so that its top-left
  corner is at the passed position."
  [element top-left bounds]
  (let [type (first element)
        [_ current-pos [w h]] bounds]
    (s/add-transform element [:translate (v- top-left current-pos)])))

(defn place-by-anchor
  [element anchor position bounds]
  (let [[_ original-position] bounds
        anchor-point (bounds->anchor-point anchor bounds)]
    (place-top-left
     element
     (v- position (v- anchor-point original-position))
     bounds)))

(defn stack [ctx {:keys [position direction anchor gap] :as params} & elements]
  (let [gap (or gap 0)
        position (or position [0 0])
        direction (or direction :down)
        anchor (or anchor (direction->default-anchor direction))
        elements (if (seq? (first elements)) (first elements) elements) ;;so that you map over elements etc
        
        vertical? (or (= direction :down) (= direction :up))
        [x y] position
        elements (map #(replace-blanks % [0 0]) elements)
        advance-pos (if (or (= direction :down) (= direction :right)) + -)
        get-size (if vertical?
                   (fn get-size [[_ _ [_ h]]] h)
                   (fn get-size [[_ _ [w _]]] w))
        get-pos (if vertical?
                   (fn get-pos [[_ [_ y] _]] y)
                   (fn get-pos [[_ [x _] _]] x))
        place-point (if vertical?
                      (fn place-point [x y pos] [x pos])
                      (fn place-point [x y pos] [pos y]))
        initial-pos (if vertical? y x)]
    (into [:g]
      (retro/transform
       [this-gap 0 gap
        bounds nil (batik/rehearse-bounds ctx element)
        size 0 (get-size bounds)
        pos 0 (get-pos bounds)
        this-pos initial-pos (advance-pos this-pos' size' this-gap')
        element (place-by-anchor element anchor (place-point x y this-pos) bounds)]
       elements))))

(defn distribute [ctx {:keys [position direction gap] :as params} & elements]
  (let [gap (or gap 0)
        position (or position [0 0])
        direction (or direction :right)
        anchor :center ;;not an option for now
        elements (if (seq? (first elements)) (first elements) elements) ;;so that you map over elements etc
        
        vertical? (or (= direction :down) (= direction :up))
        [x y] position
        elements (map #(replace-blanks % [0 0]) elements)
        bounds (map #(batik/rehearse-bounds ctx %) elements)
        step (+ gap (if vertical?
                      (apply max (map (fn [[_ _ [_ h]]] h) bounds))
                      (apply max (map (fn [[_ _ [w _]]] w) bounds))))
        place-point (if vertical?
                      (fn place-point [x y pos] [x pos])
                      (fn place-point [x y pos] [pos y]))

        first-offset (+ gap (/ step 2))
        positions (condp = direction
                    :down  (range (+ y first-offset) Integer/MAX_VALUE step)
                    :up    (range (- y first-offset) Integer/MIN_VALUE (- step))
                    :right (range (+ x first-offset) Integer/MAX_VALUE step)
                    :left  (range (- x first-offset) Integer/MIN_VALUE (- step)))]
    (into [:g]
      (map (fn [e pos bounds] (place-by-anchor e anchor (place-point x y pos) bounds))
           elements positions bounds))))

(defn resolve-layout
  ([doc]
     (resolve-layout (batik/context) doc))
  ([ctx doc]
     (walk/postwalk
      (fn [e]
        (if-not (and (vector? e) (#{:stack :distribute} (first e)))
          e
          (let [[tag attrs content] (s/normalize-element e)]
            (condp = tag
              :stack (stack ctx attrs content)
              :distribute (distribute ctx attrs content))))) doc)))

(comment
  (do
    (def ctx (batik/context))

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

    (defn make-stack [ctx pos anchor direction]
      [:g
       (marker pos)
       (stack
        ctx
        {:position pos :gap 5 :anchor anchor :direction direction}
        [:rect :_ [10 100]]
        [:circle :_ 15]
        [:rect :_ [20 20]]
        [:rect :_ [10 30]]
        [:rect :_ [10 5]]
        [:rect :_ [10 5]])]))
  
  (time
   (s/spit-svg
    (s/dali->hiccup
     (resolve-layout
      [:page
       {:height 750 :width 600, :stroke {:paint :black :width 1} :fill :none}

       ;;test that top-left works
       [:rect [300 50] [100 100]]
       (place-top-left
        [:circle [-100 40] 50]
        [300 50]
        (batik/rehearse-bounds ctx [:circle [-100 40] 50]))

       ;;test that top-left works
       (marker [250 200])
       (place-top-left
        [:rect [-100 -100] [25 25]]
        [250 200]
        (batik/rehearse-bounds ctx [:rect [-100 -100] [50 50]]))

       ;;test all cases of place-by-anchor
       (anchor-box ctx [300 200] :top-left)
       (anchor-box ctx [350 200] :top)
       (anchor-box ctx [400 200] :top-right)
       (anchor-box ctx [300 250] :left)
       (anchor-box ctx [350 250] :center)
       (anchor-box ctx [400 250] :right)
       (anchor-box ctx [300 300] :bottom-left)
       (anchor-box ctx [350 300] :bottom)
       (anchor-box ctx [400 300] :bottom-right)

       (anchor-circle ctx [300 325] :top-left)
       (anchor-circle ctx [350 325] :top)
       (anchor-circle ctx [400 325] :top-right)
       (anchor-circle ctx [300 375] :left)
       (anchor-circle ctx [350 375] :center)
       (anchor-circle ctx [400 375] :right)
       (anchor-circle ctx [300 425] :bottom-left)
       (anchor-circle ctx [350 425] :bottom)
       (anchor-circle ctx [400 425] :bottom-right)

       (make-stack ctx [50 50] :top-left :down)
       (make-stack ctx [120 50] :top :down)
       (make-stack ctx [190 50] :top-right :down)

       (make-stack ctx [50 500] :bottom-left :up)
       (make-stack ctx [120 500] :bottom :up)
       (make-stack ctx [190 500] :bottom-right :up)

       (make-stack ctx [50 575] :left :right)

       (make-stack ctx [165 650] :right :left)

       [:stack
        {:position [300 500], :direction :right, :anchor :bottom-left, :gap 0.5}
        (map (fn [h] [:rect {:stroke :none, :fill :gray} :_ [10 h]])
             (take 10 (repeatedly #(rand 50))))]

       [:stack
        {:position [300 525], :direction :right, :gap 3}
        (take
         10
         (repeat
          (stack ctx {:gap 3}
                 [:rect :_ [5 5]]
                 [:circle :_ 5]
                 [:circle :_ 1.5])))]
       
       [:stack
        {:position [300 500], :direction :right, :anchor :bottom-left, :gap 0.5}
        (map (fn [h] [:rect {:stroke :none, :fill :gray} :_ [10 h]])
             (take 10 (repeatedly #(rand 50))))]
       
       (marker [300 550])
       (let [tt [:text {:x 30 :y 30 :stroke :none :fill :black :font-family "Georgia" :font-size 40} "This is it"]
             bb (batik/rehearse-bounds ctx tt)]
         (place-by-anchor tt :top-left [300 550] bb))     

       [:stack
        {:position [300 650], :direction :right, :anchor :bottom-left, :gap 1}
        (map (fn [h]
               (stack
                ctx
                {:direction :up, :gap 5}
                [:text {:x 30 :y 30 :stroke :none :fill :black :font-family "Verdana" :font-size 6} (format "%.1f" h)]
                [:rect {:stroke :none, :fill :gray} :_ [20 h]]))
             (take 5 (repeatedly #(rand 50))))]

       [:line {:stroke :gray} [425 30] [425 70]]
       [:distribute
        {:position [425 50], :gap 2}
        [:rect :_ [10 10]]
        [:circle :_ 8]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]
      
       [:line {:stroke :gray} [425 75] [475 75]]
       [:distribute
        {:position [450 75] :direction :down}
        [:circle :_ 10]
        [:rect :_ [10 10]]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]

       [:line {:stroke :gray} [475 225] [525 225]]
       [:distribute
        {:position [500 225] :direction :up}
        [:circle :_ 10]
        [:rect :_ [10 10]]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]

       [:line {:stroke :gray} [575 225] [575 275]]
       [:distribute
        {:position [575 250], :direction :left}
        [:circle :_ 10]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]]))
    
    "s:/temp/svg_stack1.svg"))
  )
