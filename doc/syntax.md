# Syntax

## Basics

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
[:dali/page {:width 100 :height 100}
 [:circle {:cx 50 :cy 50 :r 40}]]
```

If you leave out width and height, they will be calculated
automatically to include all the graphics in the document:

```clojure
[:dali/page
 [:circle {:cx 50 :cy 50 :r 40}]]
```

There is a shorter and more convenient way to represent circles:

```clojure
[:dali/page
 [:circle [50 50] 40]]
```

You can still add attributes to the circle in the usual way (the
normal hiccup convention of an optional attribute map in the second
position still applies):

```clojure
[:circle {:stroke-paint :green :stroke-width 4 :fill :yellow} [50 50] 40]
```

In fact, `:stroke` is an attribute that can be nested for clarity and
conciseness. This is equivalent to the previous snippet:

```clojure
[:circle {:stroke {:paint :green :width 4} :fill :yellow} [50 50] 40]
```

Because it is not constrained by the limitations of XML, dali can use
the flexibility of the [EDN format](https://github.com/edn-format/edn)
to re-organise the hiccup SVG representation so that it, hopefully,
makes more sense. Because the dali syntax is a superset of hiccup, you
can choose to ignore the shortcuts provided and stick with the normal
hiccup representation of SVG and everything will still work as normal
for most things. More advanced features like layouts, markers etc,
dali assumes that you are using dali's syntax, so it's preferrable to
stick to that to get the most out of dali.

## Shapes syntax

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

If you are using any [dali markers](prefab.md) in your document, you
*have* to express your polylines in this way instead of using the
standard SVG `:points` attribute, because dali needs to be able to
replace the last and/or first point so that it rests at the base of
the marker(s).

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

## Paths

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

## Attributes

Generally, attributes are represented as a map, which is the standard
hiccup way. All the usual conversions apply, so keywords and numbers
are converted to strings. There are however some optional extensions
in attribute handling as well:

`:transform` can be passed as a vector of vectors that describe the
transformations to be applied:

```clojure
[:dali/page {:width 90 :height 50 :stroke :black :stroke-width 2 :fill :none}
  [:rect {:transform [:rotate [30 30 20]]} ;;rotate around center marked by circle
   [20 10] [20 20]]
  [:circle {:stroke :none :fill :red} [30 20] 2]
  [:rect {:transform [:rotate [10 60 20] :skew-x [30]]}
   [50 10] [20 20]]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/transform.svg)

`:stroke-dasharray` can be passed as a sequence of numbers, for example:

```clojure
[:dali/page {:width 120 :height 30 :stroke :black :stroke-width 2}
  [:line {:stroke-dasharray [10 5]} [10 10] [110 10]]
  [:line {:stroke-dasharray [5 10]} [10 20] [110 20]]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/dasharray.svg)

In the example `[10 5]` becomes `"10,5"`. A similar rule applies to
any attribute which is a sequence that contains just numbers: the
numbers are converted to a **space-delimited** string.

The valus of the `:class` attribute, can be a single keyword for the
case of having a single class, or a vector of keywords for the case of
multiple classes.

## z-index

SVG does not support z-indexes, instead complecting the order of the
elements in the source of the document with their appearance on the
screen. dali, defines a `:dali/z-index` attribute which allows you to
affect the z order of elements. Notice that the rectangle is at the
end of the document, but appears underneath the circles because of its
negative z-index:

```clojure
(require '[dali.syntax :as s])

[:dali/page
 [:defs
  (s/css (str "polyline {fill: none; stroke: black;}\n"
              "circle {fill: lightgreen; stroke: black;}\n"
              "rect {fill: green; stroke: black;}\n"))
  (prefab/sharp-arrow-marker :sharp)]
 [:dali/stack {:id :st :direction :down :position [50 50]}
  [:circle :_ 50]
  [:circle :_ 50]]
 [:rect {:id :the-rect :dali/z-index -1} [25 100] [150 100]]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/send-to-bottom.svg)

## Helper functions

dali defines helper functions to help with the generation of hiccup
syntax. In the previous example, `dali.syntax/css` allows you to
easily embed verbatim CSS. Similarly, `dali.syntax/javascript` allows
the embedding of JavaScript code.
