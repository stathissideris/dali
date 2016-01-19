# How-to

## Render to SVG

```clojure
(require '[dali.io :as io])

(def document
 [:page
  [:circle
   {:stroke :indigo :stroke-width 4 :fill :darkorange}
   [30 30] 20]])

(io/render-svg document "hello-world.svg")
```

## Rasterize to PNG

Same as above, but the last line would be:

```clojure
(io/render-png document "hello-world.png")
```

## Add an arrowhead to a line

["Prefabs"](prefab.md) are prefabricated elements that ship with dali:

```clojure
[:page
 [:defs
  (s/css (str "polyline {stroke: black; stroke-width: 2;}"))
  (prefab/sharp-arrow-marker :sharp {:scale 2})]
 [:polyline {:dali/marker-end :sharp} [50 80] [90 30]]]
```

You can also pass attributes that will be merged into the attribute
map of the shape:

```clojure
[:page
 [:defs
  (s/css (str "polyline {stroke: black; stroke-width: 2;}"))
  (prefab/sharp-arrow-marker :sharp {:scale 2})]
 [:polyline {:dali/marker-end {:id :sharp :fill :red}} [50 80] [90 30]]]
```
 
## Bar Charts

Although dali is a generic graphics library, it's really easy to create
bar charts with it, so here are some examples:

```clojure
[:page {:width 260 :height 140}
 [:dali/stack
  {:position [10 10], :direction :right, :anchor :bottom-left, :gap 2}
  (map (fn [h] [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]])
       [10 30 22 56 90 59 23 12 44 50])]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/graph1.svg)

Nested layouts make adding text easy:

```clojure
[:page {:width 260 :height 160}
 [:dali/stack
  {:position [10 10], :direction :right, :anchor :bottom-left, :gap 2}
  (map (fn [h]
         [:dali/stack
          {:direction :up :gap 6}
          [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]]
          [:text {:text-family "Verdana" :font-size 12} (str h)]])
       [10 30 22 56 90 59 23 12 44 50])]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/graph2.svg)

Stacked bar charts are a piece of cake:

```clojure
[:page {:width 270 :height 150}
 [:dali/stack
  {:position [10 10], :direction :right, :anchor :bottom-left, :gap 2}
  (map (fn [[a b c]]
         [:dali/stack
          {:direction :up}
          [:rect {:stroke :none, :fill "#D46A6A"} :_ [20 a]]
          [:rect {:stroke :none, :fill "#D49A6A"} :_ [20 b]]
          [:rect {:stroke :none, :fill "#407F7F"} :_ [20 c]]])
       [[10 10 15]
        [30 10 20]
        [22 10 25]
        [56 10 10]
        [90 10 30]
        [59 22 25]
        [23 10 13]
        [12 6 8]
        [44 22 18]
        [50 20 10]])]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/graph3.svg)

## Flowchart

```clojure
[:page {:stroke :black :fill :none}
 [:defs
  (s/css (str ".marker {fill: black; stroke: none;}"
              ".grey {fill: lightgrey;}\n"
              "rect {fill: white;}\n"
              "rect:hover {fill: orange;}\n"
              "text {fill: black; stroke: none;}"))
  (prefab/sharp-arrow-marker :sharp)
  (prefab/sharp-arrow-marker :big-sharp {:scale 2})
  (prefab/triangle-arrow-marker :triangle)]
 [:dali/align {:axis :center}
  [:rect {:id :c :transform [:translate [0 -20]]} [200 70] [120 150]]
  [:text "center"]]

 ;;No :relative-to attribute, so the layout happens relative to the
 ;;first child (the box) -- the center of the :text is alinged to
 ;;the center of the box.
 [:dali/align {:axis :center}
  [:rect {:id :a :class :grey} [20 20] [100 100]]
  [:text "A"]]

 [:dali/align {:axis :center}
  [:rect {:id :b :transform [:translate [0 -20]]} [440 70] [50 50]]
  [:text "B"]]

 [:dali/align {:axis :center}
  [:rect {:id :d} [20 350] [50 50]]
  [:text "D"]]

 [:dali/align {:axis :center}
  [:rect {:id :e} [440 230] [50 50]]
  [:text "E"]]

 [:dali/align {:axis :center}
  [:rect {:id :f} [500 70] [50 50]]
  [:text "F"]]

 [:dali/align {:axis :center}
  [:rect {:id :g} [350 300] [50 50]]
  [:text "G"]]

 ;;Everything is in place, time to connect them
 [:dali/connect {:from :a :to :c :dali/marker-end :sharp}]

 ;; :fill :green doesn't work because CSS wins
 [:dali/connect {:from :c :to :b :stroke :green :stroke-width 2.5
                 :dali/marker-end {:id :big-sharp :style "fill: green;"}}]

 [:dali/connect {:from :d :to :c :class :myclass :dali/marker-end :sharp}]
 [:dali/connect {:from :c :to :e :type :-| :dali/marker-end :sharp}]
 [:dali/connect {:from :e :to :f :type :-| :dali/marker-end :sharp}]
 [:dali/connect {:from :e :to :g :type :|- :class :foo :dali/marker-end :triangle}]
 [:dali/connect {:from :e :to :g :type :-| :dali/marker-end :sharp}]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/connect1.svg)
