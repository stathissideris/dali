# dali

> I do not understand why, when I ask for grilled lobster in a restaurant, I'm never served a cooked telephone.
> -- Salvador Dalí

dali is a Clojure library for representing the SVG graphics format. It
allows the creation and manipulation of SVG file. The syntax used to
describe the graphical elements is based on
[hiccup](https://github.com/weavejester/hiccup) with a few extensions.

Here's a hello world for dali:

```clojure
(require '[dali.syntax :as s])

(def document
  [:page {:width 100 :height 100}
   [:circle
    {:stroke :indigo :stroke-width 4 :fill :darkorange}
    [50 50] 40]])

(-> document (s/dali->hiccup) (s/spit-svg "simple.svg"))
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/hello-world.svg)

## Using dali in your project

Before adding dali as a dependency, please consider that it still a
very immature project where the API and syntax can (an very likely
will) change. Having said that, you are very welcome to give it a
go. Just add this to the dependencies of your `project.clj`:

```
[dali "0.5.0"]
```

## Syntax

### Basics

In order to use dali you need to know the SVG syntax, because dali's
syntax is essentially equivalent to how you would represent SVG in
hiccup. For example, here is how you would write a very simple SVG
document in hiccup:

```clojure
[:svg {:width 100 :height 100}
 [:circle {:cx 50 :cy 50 :r 40}]]
```

This is almost valid dali syntax, with the small exception of the
`svg` tag being replaced by `page`:

```clojure
[:page {:width 100 :height 100}
 [:circle {:cx 50 :cy 50 :r 40}]]
```

You could use dali like that, but there is a shorter and more
convenient way to represent circles:

```clojure
[:svg {:width 100 :height 100}
 [:circle [50 50] 40]]
```

You can still add attributes to the circle in the usual way (the
normal hiccup convention of an optional attribute map in the second
position still applies):

```clojure
[:circle {:stroke :green :stroke-width 4 :fill :yellow} [50 50] 40]
```

In fact, `:stroke` is an attribute that can be nested for clarity and
conciseness:

```clojure
[:circle {:stroke {:paint :green :width 4} :fill :yellow} [50 50] 40]
```

Because it is not constrained by the limitations of XML, dali can use
the flexibility of the [EDN format](https://github.com/edn-format/edn)
to re-organise the hiccup SVG representation so that it, hopefully,
makes more sense. Because the dali syntax is a superset of hiccup, you
can choose to ignore the shortcuts provided and stick with the normal
hiccup representation of SVG and everything will still work as normal.

### Shapes syntax

As seen, circles can be written like so (in the following snippets the
optional attribute map is ommitted):

```clojure
[:circle [center-x center-y] radius]
```

Lines can be written like so:

```clojure
[:line [x1 y1] [x2 y2]]
```

Ellipses:
```clojure
[:ellipse [center-x center-y] radius-x radius-y]
```

Polylines and polygons:
```clojure
[:polyline [x1 y1] [x2 y2] [x3 y3] [x4 y4] ...]

[:polygon [x1 y1] [x2 y2] [x3 y3] [x4 y4] ...]
```

Rectangles:
```clojure
[:rect [x y] [width height]]
```

It is also possible to get rounded rectangles (a standard SVG feature)
by writing:

```clojure
[:rect [x y] [width hight] radius] ;;same radius for both axes

[:rect [x y] [width hight] [x-radius y-radius]] ;;different radii
```

### Paths

If you are familiar with the SVG syntax for paths, you'll know that
the actual path is represented as text within the "d" attribute. dali
raises the representation to the level of EDN, so instead of writing:

```clojure
[:path {:d "M 0 0 L 10 0 L 5 10 z"}]
```

...you can write:

```clojure
[:path :M [0 0] :L [10 0] :L [5 10] :z]
```

Or you can use the long version of the commands:

```clojure
[:path :move-to [0 0] :line-to [10 0] :line-to [5 10] :close]
```

The following table summarizes the diffent commands available for
paths:

| Standard | Long form       | Parameters                        |
|----------|-----------------|-----------------------------------|
| :M       | :move-to        | [x y]                             |
| :m       | :move-by        | [dx dy]                           |
| :L       | :line-to        | [x y]                             |
| :l       | :line-by        | [dx dy]                           |
| :H       | :horizontal-to  | x                                 |
| :h       | :horizontal-by  | dx                                |
| :V       | :vertical-to    | y                                 |
| :v       | :vertical-by    | dy                                |
| :C       | :cubic-to       | [x1 y1] [x2 y2] [x y]             |
| :c       | :cubic-by       | [x1 y1] [x2 y2] [x y]             |
| :S       | :symmetrical-to | [x2 y2] [x y]                     |
| :s       | :symmetrical-by | [x2 y2] [x y]                     |
| :Q       | :quad-to        | [x1 y1] [x y]                     |
| :q       | :quad-by        | [x1 y1] [x y]                     |
| :A       | :arc-to         | [rx ry] x-rot large? sweep? [x y] |
| :a       | :arc-by         | [rx ry] x-rot large? sweep? [x y] |
| :Z       | :close          |                                   |
| :z       | :close          |                                   |

### Attributes

Generally, attributes are represented as a map which is the standard
hiccup way. All the usual conversions apply, so keywords and numbers
are converted to strings. There are however some optional extensions
in attribute handling as well:

`:transform` can be passed as a vector of vectors that describe the
transformations to be applied:

```clojure
[:page {:width 90 :height 50 :stroke :black :stroke-width 2 :fill :none}
     
     [:rect {:transform [[:rotate [30 30 20]]]} ;;rotate around center marked by circle
      [20 10] [20 20]]
     [:circle {:stroke :none :fill :red} [30 20] 2]

     [:rect {:transform [[:rotate [10 60 20]] [:skew-x [30]]]}
      [50 10] [20 20]]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/transform.svg)

`:stroke-dasharray` can be passed as a sequence of numbers, for example:

```clojure
[:page {:width 120 :height 30 :stroke :black :stroke-width 2}
     [:line {:stroke-dasharray [10 5]} [10 10] [110 10]]
     [:line {:stroke-dasharray [5 10]} [10 20] [110 20]]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/dasharray.svg)

In the example `[10 5]` becomes `"10,5"`. A similar rule applies to
any attribute which is a sequence that contains just numbers: the
numbers are converted to a **space-delimited** string.

### Layout

There are currently two ways to layout elements in dali: you can stack
them on top of each other or you can distribute them at equal
distances. They both involve custom syntax. The
[Apache Batik](http://xmlgraphics.apache.org/batik/) is used for
figuring out the sizes of various elements. Please note that for some
reason the performance of layouts is currently quite poor, but this
will be investigated and hopefully fixed in the next version.

This is how you can stack elements:

```clojure
(s/dali->hiccup
 (layout/resolve-layout
  [:page {:width 200 :height 40 :stroke :none}
   [:stack
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
 [:stack
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
 [:stack
  {:position [10 70] :anchor :bottom-left :direction :right}
  [:rect {:fill :mediumslateblue} :_ [50 20]]
  [:rect {:fill :sandybrown} :_ [30 60]]
  [:rect {:fill :green} :_ [40 10]]
  [:rect {:fill :orange} :_ [20 40]]]
 [:circle {:fill :red} [10 70] 4]
 
 [:stack
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
 [:stack {:position [20 20] :direction :right} (shapes "right")]
 [:stack {:position [130 70] :gap 5 :direction :left} (shapes "left")]
 [:stack {:position [40 150] :gap 5 :direction :down} (shapes "down")]
 [:stack {:position [90 250] :gap 18 :direction :up} (shapes "up")]]
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
 [:distribute
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

## Stock shapes etc

The `dali.stock` namespace defines some markers for arrows, which are
parameterizable. See the individual functions in the namespace for
more details.

```clojure
[:page {:stroke {:width 2 :paint :black}}
 [:defs
  (stock/sharp-arrow-end :sharp)
  (stock/triangle-arrow-end :triangle)
  (stock/curvy-arrow-end :curvy)
  (stock/dot-end :dot)
  (stock/sharp-arrow-end :very-sharp :height 32)]
 [:polyline
  {:fill :none :marker-end "url(#sharp)"}
  [50 80] [90 30]]
 [:polyline
  {:fill :none :marker-end "url(#triangle)"}
  [80 80] [120 30]]
 [:polyline
  {:fill :none :marker-end "url(#curvy)"}
  [110 80] [150 30]]
 [:polyline
  {:fill :none :marker-end "url(#dot)"}
  [140 80] [180 30]]
 [:polyline
  {:fill :none :marker-end "url(#very-sharp)"}
  [170 80] [210 30]]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/markers1.svg)

## Examples

Altough dali is a generic graphics lirbary, it's really easy to create
bar charts with it, so here are some examples:

```clojure
[:page {:width 260 :height 140}
 [:stack
  {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
  (map (fn [h] [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]])
       [10 30 22 56 90 59 23 12 44 50])]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/graph1.svg)

Nested layouts make adding text easy:

```clojure
[:stack
 {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
 (map (fn [h]
        [:stack
         {:direction :up :gap 6}
         [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]]
         [:text {:text-family "Verdana" :font-size 12} (str h)]])
      [10 30 22 56 90 59 23 12 44 50])]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/graph2.svg)

Stacked bar charts are a piece of cake:

```clojure
[:page {:width 270 :height 150}
 [:stack
  {:position [10 140], :direction :right, :anchor :bottom-left, :gap 2}
  (map (fn [[a b c]]
         [:stack
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

## Roadmap

Planned for the future:

* Better validation of the syntax using the Prismatic schema library.
* Porting basic functionality to ClojureScript.
* More stock shapes.
* Support for graph-oriented elements (axes etc).
* More layout functionality
    * Easier ways to connect boxes by anchor.
    * Placing an element in the center of another element.

## License

Copyright © 2014 Stathis Sideris

Distributed under the Eclipse Public License, the same as Clojure.
