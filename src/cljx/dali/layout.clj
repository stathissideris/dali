(ns cljx.dali.layout
  (:require [clojure.walk :as walk]
            [retrograde :as retro]
            [dali.syntax :as s]
            [dali.batik :as batik]
            [dali.geom :as geom :refer [v+ v- v-half]]))

(defn- replace-blanks [element replacement]
  (walk/postwalk (fn [f] (if (= f :_) replacement f)) element))

(defn set-top-left
  "Adds a translation transform to an element so that its top-left
  corner is at the passed position."
  [element top-left bounds]
  (let [type (first element)
        [_ current-pos [w h]] bounds
        transform
        (condp = type
          :circle (v+ top-left (v-half [w h]))
          :text top-left ;;TODO
          (v- top-left current-pos))]
    (s/add-transform element [:translate transform])))

(defn stack [ctx {:keys [position direction gap] :as params} & elements]
  (let [gap (or gap 2)
        [x y] position
        elements (map #(replace-blanks % [0 0]) elements)]
    (into [:g]
     (retro/transform
      [this-gap 0 gap
       bounds nil (batik/rehearse-bounds ctx element)
       size 0 (let [[_ _ [_ h]] bounds] h)
       pos 0 (let [[_ [_ y] _] bounds] y)
       this-pos y (+ this-pos' size' this-gap')
       element (set-top-left element [x this-pos] bounds)]
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
  (def anchors #{:top-left :top-middle :top-right :middle-left :middle-right :bottom-left :bottom-middle :bottom-right :center}))

(comment
  (def ctx (batik/batik-context (batik/parse-svg-uri "file:///s:/temp/svg.svg") :dynamic? true))
  (let [circle-pos [200 50]]
   (s/spit-svg
    (s/dali->hiccup
     [:page
      {:height 500 :width 500, :stroke {:paint :black :width 1} :fill :none}

      [:rect circle-pos [100 100]]
      (set-top-left
       [:circle [0 0] 50]
       circle-pos
       (batik/rehearse-bounds ctx [:circle [0 0] 50]))

      [:circle [200 200] 5]
      (set-top-left
       [:rect [-100 -100] [50 50]]
       [200 200]
       (batik/rehearse-bounds ctx [:rect [-100 -100] [50 50]]))
      
      [:circle [50 50] 5]
      (stack
       ctx
       {:position [50 50] :gap 5}
       [:rect :_ [10 100]]
       [:circle :_ 20]
       [:rect :_ [10 30]]
       [:rect :_ [10 5]]
       [:rect :_ [10 5]])])
    "s:/temp/svg_stack1.svg"))
  )
