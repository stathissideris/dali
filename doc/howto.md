# How-to

Altough dali is a generic graphics lirbary, it's really easy to create
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
