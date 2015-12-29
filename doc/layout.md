# Layout

There are currently two ways to layout elements in dali: you can stack
them on top of each other or you can distribute them at equal
distances. They both involve custom syntax.
[Apache Batik](http://xmlgraphics.apache.org/batik/) is used for
figuring out the sizes of various elements.

This is how you can stack elements:

```clojure
(s/dali->hiccup
 (layout/resolve-layout
  [:page {:width 200 :height 40 :stroke :none}
   [:dali/stack
    {:position [10 20] :anchor :left :direction :right}
    [:rect {:fill :mediumslateblue} :_ [50 20]]
    [:rect {:fill :sandybrown} :_ [30 20]]
    [:rect {:fill :green} :_ [40 20]]
    [:rect {:fill :orange} :_ [20 20]]]]))
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/stack1.svg)

The rectangles are simply stacked next to each other, from left to
right (as defined by the `:direction` parameter). Note that in order
to calculate the new positions of the rectangles, you need to call
`resolve-layout` on the document. The weird `:_` syntax is simply
replaced by `[0 0]` when resolving the layout. This is simply to have
a visual queue of the parameters that don't matter as the rectangles
will be moved anyway.

When stacking to the right, the middle of the left edge of each shape
is aligned on the same line. In other words, the middle-left "anchors"
of the shapes are aligned. This is better illustrated with shapes of
different heights. Let's remix the previous example:

```clojure
[:page {:width 200 :height 80 :stroke :none}
 [:dali/stack
  {:position [10 40] :anchor :left :direction :right}
  [:rect {:fill :mediumslateblue} :_ [50 20]]
  [:rect {:fill :sandybrown} :_ [30 60]]
  [:rect {:fill :green} :_ [40 10]]
  [:rect {:fill :orange} :_ [20 40]]]
 [:circle {:fill :red} [10 40] 4]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/stack2.svg)

The `:left` anchor of the first shape is indicated by the red dot. It
is possible to perform stacking using different anchors:

```clojure
[:page {:width 310 :height 80 :stroke :none}
 [:dali/stack
  {:position [10 70] :anchor :bottom-left :direction :right}
  [:rect {:fill :mediumslateblue} :_ [50 20]]
  [:rect {:fill :sandybrown} :_ [30 60]]
  [:rect {:fill :green} :_ [40 10]]
  [:rect {:fill :orange} :_ [20 40]]]
 [:circle {:fill :red} [10 70] 4]
 
 [:dali/stack
  {:position [170 10] :anchor :top-left :direction :right}
  [:rect {:fill :mediumslateblue} :_ [50 20]]
  [:rect {:fill :sandybrown} :_ [30 60]]
  [:rect {:fill :green} :_ [40 10]]
  [:rect {:fill :orange} :_ [20 40]]]
 [:circle {:fill :red} [170 10] 4]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/stack3.svg)

The stack layout also supports stacking in different directions (left,
right, up and down) and it has an optional gap parameter (0 by
default):

```clojure
(defn shapes [s]
  (list
   [:text {:font-family "Georgia" :font-size 20} s]
   [:rect :_ [20 20]]
   [:circle :_ 15]
   [:polyline [0 0] [20 0] [10 20] [20 20]]))

[:page {:width 350 :height 500 :stroke {:paint :black :width 2} :fill :none}
 [:dali/stack {:position [20 20] :direction :right} (shapes "right")]
 [:dali/stack {:position [130 70] :gap 5 :direction :left} (shapes "left")]
 [:dali/stack {:position [40 150] :gap 5 :direction :down} (shapes "down")]
 [:dali/stack {:position [90 250] :gap 18 :direction :up} (shapes "up")]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/stack4.svg)

There are two noteworthy things about this example: First, when you
use text that participates in layouts, you should *always* be specific
about the font and size, so that the size the font is rendered on the
browser matches the size calculated when the SVG is generated and
everything aligns as expected (assuming that the font is available in
both places).

Second, when creating functions that return a collection of elements,
they have to be returned as lists and not vectors, because dali will
expand lists but will try to interpret vectors as tags (hiccup behaves
in the same way).

The other way is to distribute the centers of the elements in equal
distances:

```clojure
[:page {:width 200 :height 60 :stroke :none}
 [:dali/distribute
  {:position [10 20] :direction :right}
  [:rect {:fill :mediumslateblue} :_ [50 20]]
  [:rect {:fill :sandybrown} :_ [30 20]]
  [:rect {:fill :green} :_ [40 20]]
  [:rect {:fill :orange} :_ [20 20]]]

 ;;show centers
 [:g (map #(vector
            :line
            {:stroke {:paint :red :width 2}} [% 40] [% 50])
          (range 35 200 50))]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/distribute1.svg)

The exact distance between the centers is determined by the widest or
tallest element (depending on the direction) and also the gap
parameter. The distribute layout also supports the 4 directions
supported by stack.

Layouts can also be nested within each other. The deepest layouts are
resolved first and then they may be moved by their parents.
