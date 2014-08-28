(ns cljx.dali.layout
  (:require [clojure.walk :as walk]
            [dali.syntax :as s]))

(defn- replace-blanks [element replacement]
  (walk/postwalk (fn [f] (if (= f :_) replacement f)) element))

(defn stack [ctx params & elements]
  (let [elements (map #(replace-blanks % [0 0]) elements)
        bounds (map (partial batik/get-bounds ctx) elements)]
    ))

(comment
  (distribute
   ctx
   {:position [10 10] :direction :right}
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
  (stack
   ctx
   {:position [10 10]}
   [rect :_ [10 100]]
   [rect :_ [10 30]]))
