# Layouts and document tranformations

Layout functionality in dali allows the placement of elements without
knowing their exact dimensions in advance. All appear as custom tags,
and they all use the `:dali/` prefix. Use this page as a reference of
the differect layouts and operations, but make sure to read the
"Understanding the mechanism" section at the bottom before you start
using them.

In most cases, the children of the layout tag will be moved around to
conform to the layout. For some layouts this not the case, instead the
introduce new elements into the document.

Layouts tags can be nested within other layout tags.

Instead of acting on their children elements, layouts can also
"select" elements from other parts of the document and transform
them. For this, dali uses the
[enlive](https://github.com/cgrand/enlive) selector syntax.

Layout tags that contain elements are rendered as `<g>` tags in the
final SVG, while layout tags that operate on a different part of the
document via their selector, are completely removed from the SVG.

The same mechanism is used for document operations that do not involve
changing the positions of elements but may add new elements based on
the position and dimensions of other elements.

In order to use the built-in layouts you need to require the relevant
namespace (`dali.layout.stack`, `dali.layout.align` etc) despite the
fact that you are not using any of the functions directly.

[Apache Batik](http://xmlgraphics.apache.org/batik/) is used for
figuring out the sizes of various elements.

## Basic layouts: stack, distribute, align

### Stack

#### Quick ref:

```clojure
[:dali/stack {:direction :up, :anchor :bottom, :gap 0}]
```
* `:direction` - the direction of accumulation
  * one of `:up` `:down` `:left` `:right`
  * default: `:up`
  * optional
* `:anchor` - the anchor used to align the elements
  * one of: `:top` `:bottom` `:left` `:right` `:top-left` `:top-right` `:bottom-left` `:bottom-right`
  * default: sensible default selected based on `:direction`
  * optional
* `:gap` - the gap to leave between elements
  * double
  * default: `0`
  * optional
* `:select` - an enlive selector that will transform elements from
  elsewhere in the document instead of tranforming the direct children
  of the layout tag
* `:position` - the position of the top-left corner of the whole
  layout relative to its parent. Can only be applied when there is no
  `:select` attribute
  * `[x y]` - doubles
  * default: `[0 0]` - the position of the first element is used when
    no position is defined
  * optional

This is how you stack elements:

```clojure
(ns stack-example
  (:require [dali.io :as io]
            [dali.layout.stack])) ;;don't forget this

(def document
  [:page {:stroke :none}
   [:dali/stack
    {:position [10 10] :anchor :left :direction :right}
    [:rect {:fill :mediumslateblue} :_ [50 20]]
    [:rect {:fill :sandybrown} :_ [30 60]]
    [:rect {:fill :green} :_ [40 10]]
    [:rect {:fill :orange} :_ [20 40]]]
   [:circle {:fill :red} [10 40] 4]])

(io/render-svg document "stack.svg")
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/stack1.svg)

The rectangles are simply stacked next to each other, from left to
right (as defined by the `:direction` parameter). ???(link to a recipe
for rendering) The `:_` keyword is simply replaced by `[0 0]` before
resolving the layout and it is a visual cue for dimensions that don't
matter as the rectangles will be moved anyway.

When stacking to the right, the middle of the left edge of each shape
is aligned on the same line. In other words, the middle-left "anchors"
of the shapes are aligned. This is better illustrated with shapes of
different heights. Let's remix the previous example:

```clojure
[:page {:width 200 :height 80 :stroke :none}
 [:dali/stack
  {:position [10 10] :anchor :left :direction :right}
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
  {:position [10 10] :anchor :bottom-left :direction :right}
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
right, up and down) and it has an optional `:gap` parameter (0 by
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

If no `:position` attribute is passed, the stack layout is performed
based on the position of the first element:

```clojure
[:g
 [:dali/stack
  {:direction :right}
  [:rect {:fill :mediumslateblue} [10 10] [50 20]]
  [:rect {:fill :sandybrown} :_ [30 20]]
  [:rect {:fill :green} :_ [40 20]]
  [:rect {:fill :orange} :_ [20 20]]]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/stack5-5.svg)

#### Selector layouts

Where supported, layouts and other transformations can be used as
selector layouts, which instead of affecting their direct children,
they affect elements that can appear under different parts of the
tree. The elements that are affected are determined using an
[enlive selector](https://github.com/cgrand/enlive#selectors), defined
in the `:select` attribute. Selector layouts have no children.

Stack supports the `:select` attribute, and this is how you can use it:

```clojure
[:page {:stroke :none}
 [:rect {:class :stacked, :fill :mediumslateblue} [10 10] [50 20]]
 [:rect {:class :stacked, :fill :sandybrown} :_ [30 20]]
 [:rect {:class :stacked, :fill :green} :_ [40 20]]
 [:rect {:class :stacked, :fill :orange} :_ [20 20]]
 [:rect {:fill :red} [10 50] [30 30]]
 [:dali/stack
  {:select [:.stacked] :anchor :left :direction :right}]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/stack8.svg)

The class-based selector in this case selects all the `:rect`s with
the `:stacked` class, and stacks them as part of the same stack. The
red rectangle does not have the `:stacked` class, so it remains in it
original position. Selector layouts cannot have a `:position`
attribute, so in this case the layout starts at the position of the
first element matched by the selector.

### Distribute

#### Quick ref:

```
[:dali/distribute {:direction :right, :anchor :center, :gap 0}]
```
* `:direction` - the direction of accumulation
  * one of `:up` `:down` `:left` `:right`
  * default: `:right`
  * optional
* `:anchor` - the anchor used to align the elements
  * one of: `:top` `:bottom` `:left` `:right` `:top-left` `:top-right` `:bottom-left` `:bottom-right`
  * default: `:center`
  * optional
* `:gap` - the gap to leave between elements
  * double
  * default: `0`
  * optional
* `:select` - an enlive selector that will transform elements from
  elsewhere in the document instead of tranforming the direct children
  of the layout tag
* `:position` - the position of the top-left corner of the whole
  layout relative to its parent. Can only be applied when there is no
  `:select` attribute
  parent. Can only be applied when there is no `:select` attribute
  * `[x y]` - doubles
  * default: `[0 0]` - the position of the first element is used when
    no position is defined
  * optional

This is how to distribute the centers of the elements in equal
distances:

```clojure
[:page {:stroke :none}
 [:dali/distribute
  {:direction :right}
  [:rect {:fill :mediumslateblue} [10 10] [50 20]]
  [:rect {:fill :sandybrown}       [0 10] [30 20]]
  [:rect {:fill :green}            [0 10] [40 20]]
  [:rect {:fill :orange}           [0 10] [20 20]]]

 ;;show centers
 [:g (map #(vector
            :line
            {:stroke {:paint :red :width 2}} [% 40] [% 50])
          (range 35 200 50))]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/distribute1.svg)

In this example, no `:position` parameter was defined, so the whole
layout happened in relation to the position of the first element.

The exact distance between the centers is determined by the *widest*
or the *tallest* element (depending on the direction) and also by the
`:gap` parameter. The `dali/distribute` layout also supports the 4
directions supported by stack.

### Align

#### Quick ref:

```clojure
[:dali/align {:relative-to :first, :axis :left}]
```

* `:relative-to` - what to align the elements to.
  * Value is either:
    * one of `:first` `:last` - to align relative to the first or last
      elements
    * a number - to align elements relative to a particular horizontal
      or vertical depending on the `:axis`. This is in absolute
      coordinates.
  * default: `:first`
  * optional
* `:axis` - the "axis" of alignment. For example, `:left` will align
  the left edges of all elements relative to whatever is defined by
  `:relative-to`
  * one of: `:top` `:bottom` `:left` `:right` `:v-center` `:h-center` `:center`
  * default: `:center` - special case which means that elements are
    aligned both horizontally and vertically so they are centered on
    top of each other
  * optional
* `:select` - an enlive selector that will transform elements from
  elsewhere in the document instead of tranforming the direct children
  of the layout tag

This layout will align the edges of elements either in relation to the
corresponding edge of another element, or in relation to a "guide"
which a theoretical horizontal or vertical line on the screen.

Here's a simple case where the bottom edges of the last three circles
are aligned to the bottom edge of the first circle:

```clojure
[:page
 [:line {:stroke :lightgrey} [20 110] [240 110]]
 [:dali/align {:relative-to :first :axis :bottom}
  [:circle {:fill :mediumslateblue} [50 90] 20]
  [:circle {:fill :sandybrown}      [120 0] 40]
  [:circle {:fill :green}           [170 0] 30]
  [:circle {:fill :orange}          [220 0] 10]]
 [:circle {:fill :none :stroke {:paint :red :width 2}} [50 90] 20]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/align-test5.svg)

Note that when aligning vertically, the horizontal positions of
elements remain unchanged. That's why the second circle for example
has an initial position of `[120 0]`, the `0` will be replaced by the
new position to align it to the first circle, but `120` will remain
unchanged.

Here's a snippet that uses the `:center` axis alignment to align some
text, a circle and a rectangle all at the center of a circle:

```clojure
[:page {:width 120 :height 120}
 [:dali/align {:relative-to :first :axis :center :select [:.label]}]
 [:circle {:class :label :fill :none :stroke :gray :stroke-dasharray [5 5]} [60 60] 40]
 [:text {:class :label :text-family "Verdana" :font-size 17} "aligned"]
 [:circle {:class :label :fill :none :stroke :black} :_ 50]
 [:rect {:class :label :fill :none :stroke :gray} :_ [60 25]]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/align-test4.svg)

In this case, we elected to use the selector-style layout instead of
nesting the children elements within the `[:dali/align]` -- this style
of layout application is supported by most layouts, and the "first"
element in this case is the first element that matches the selector.


### Place

#### Quick ref:

```clojure
[:dali/place {:relative-to [:p1 :top-right] :anchor :top-left :offset [5 0]}
  [:circle {:fill :mediumslateblue} :_ 10]]
```

* `:relative-to`
  * Value is either:
    * a two-element vector of `[other-id anchor]` to place the element
      in relation to a particular anchor of another element.
    * a keyword referring to the id of another element, in which case
      the element being placed is placed in relation to the center of
      the `:relative-to` element (equivalent to `[other-id :center]`).
* `:anchor` - the anchor to
  * default: `:center`
  * optional
* `:offset`
  * default: `[0 0]`
  * optional

The `[:dali/place]` layout allows you to place an element in relation
to another element. So you can say things like "place this circle on
the left of that rectangle". The child of the `[:dali/place]` is
translated in relation to the `:relation-to` element. Here is an
example of using a larger rectangle as a reference element to place a
number of smaller elements:

```clojure
[:page {:stroke :black :fill :none}
 [:rect {:id :p1} [20 20] [100 100]]

 [:dali/place {:relative-to :p1}
  [:circle {:fill :lightblue} :_ 5]]

 [:dali/place {:relative-to [:p1 :top-right] :anchor :top-left :offset [5 0]}
  [:circle {:fill :mediumslateblue} :_ 10]]

 [:dali/place {:relative-to [:p1 :bottom-right] :anchor :bottom-left}
  [:rect {:fill :limegreen} :_ [20 40]]]

 [:dali/place {:relative-to [:p1 :bottom-left] :anchor :bottom-left :offset [10 -10]}
  [:rect {:fill :yellow} :_ [40 20]]]

 [:rect {:id :child :fill :orange} :_ [25 10]]

 [:dali/place {:select :child :relative-to [:p1 :top-left] :anchor :top-left :offset [10 10]}]

 [:dali/place {:relative-to [:p1 :top-right] :anchor :top-right :offset [-5 10]}
  [:text {:font-family "Verdana" :font-size 13 :stroke :none :fill :black} "foo bar"]]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/place1.svg)

The `:relative-to` value can either be keyword referring to the id of
an element, or a two-element vector of `[id anchor]` to place the
element in relation to a particular anchor of another element.

### Matrix

#### Quick ref:

```clojure
[:dali/matrix {:position [50 50] :columns 4 :row-gap 5 :column-gap 20} ...]
```

* `:columns` - how many columns the matrix has
* `:gap` - gap between the elements
* `:row-gap` - gap between rows - overrides `:gap`
* `:column-gap` - gap between columns - overrides `:gap`
* `:position` - the position of the top-left corner of the whole
  layout relative to its parent. Can only be applied when there is no
  `:select` attribute
  * `[x y]` - doubles
  * default: `[0 0]` - the position of the first element is used when
    no position is defined
  * optional

Matrices are just like grids -- the main difference being that
matrices are elastic: the width of each row is determined by the
tallest element in the row and the width of each column is determined
by the widest element in the column.

```clojure
[:page
 [:defs
  (s/css (str "polyline {fill: none; stroke: black;}\n"
              "rect {fill: none; stroke: black;}\n"))
  (prefab/sharp-arrow-marker :sharp)]
 [:dali/matrix {:position [50 50] :columns 4 :row-gap 5 :column-gap 20}
  [:rect :_ [50 50]]
  [:rect {:id :c} :_ [50 70]]
  [:rect {:id :b} :_ [70 50]]
  [:rect :_ [30 30]]

  [:rect {:id :e} :_ [30 90]]
  [:rect {:id :d} :_ [30 30]]
  [:rect {:id :a} :_ [50 50]]
  [:rect :_ [70 50]]

  [:rect :_ [100 100]]
  [:rect :_ [90 30]]
  [:rect :_ [50 50]]
  [:rect :_ [20 50]]]

 [:dali/connect {:from :a :to :b :dali/marker-end :sharp}]
 [:dali/connect {:from :b :to :c :dali/marker-end :sharp}]
 [:dali/connect {:from :c :to :d :dali/marker-end :sharp}]
 [:dali/connect {:from :d :to :e :dali/marker-end :sharp}]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/matrix3.svg)

## Document transformations

### Connect

#### Quick ref:

```clojure
[:dali/connect {:from :c, :to :e, :type :-|, :dali/marker-end :sharp}]
```

* `:from` - the id of the element frorm which the connection starts
* `:to` - the id of the element to which the connection ends
* `:type` - the type of line
  * one of: `:--` `:|-` `:-|`
    * `:--` straight line
    * `:|-` corner, first vertical then horizontal
    * `:-|` corner, first horizontal then vertical
  * default: `:--`
  * optional

`:dali/connect` adds a line that will connect the closest anchors of
two elements in the document. The anchors that can be connected are
`:top`, `:bottom`, `:left` or `:right`, and the pair is selected
automatically based on their distance. The connector is a straight
line by default, but you can instruct dali to create a corner
connector that starts vertically and then moves horizontally
(`:type :|-`) or the inverse (`:type :-|`).

Here is an example of `:connect` in action:

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

Note that the `:connect` tags appear at the bottom of the document to
ensure that all the other layout tranformations have been applied
first, and that everything is in its final position before connecting
the elements. Also see [Layout application order](#layout-application-order).

Apart from the `:from`, `:to` and `:type` keys, any other keys present
in the attributes of `:connect` get merged into the attribute map of
the `:polyline` tag that's inserted into the document as the line of
the connector. You can use this mechanism in various ways, for example:

* Pass an `:id` to be attached to the polyline and refer to it later.
* Pass a value for `:dali/marker-end` to add an arrowhead as a line
  marker to use at the end of the line. As explained
  [here](prefab.md), the value of this is either:
  * the id of a marker as defined in the `[:defs]` part of the
    document. dali prefabs allow you to pass this id when
    constructing them, or you can make your own marker.
  * a map containing an `:id` key to identify the marker to use and
    any other attributes that will get merged into the `[:use]` tag of
    the marker. Refer to the [prefab](prefab.md) documentation for
    more details.
* Pass `:dali/marker-start` - same as marker-end, but for the start of
  the connection.

If the above paints a slightly complex picture, just remember this:
The extra attributes end up on the line of the connector, any
attributes in maps under `:dali/marker-end` or `:dali/marker-start`
end up on the marker `[:use]` tag. In this way, the fill etc of both
the line and the markers can be controlled.

### Surround

#### Quick ref:

```clojure
[:dali/surround {:select [:.foo] :padding 20 :rounded 15 :attrs {:id box-id :dali/z-index -1}}]
```

* `:select` - an enlive selector that selects the elements to be
  surrounded with the rectangle. You can also use a single keyword as
  a value, and just a single element with that `:id` will be
  surrounded.
* `:padding` - how much space to leave between the edge of the
  surrounded elements and the edge of the rectangle. Optional,
  defaults to 20.
* `:rounded` - the radius of the rounding of the rectangle. Optional,
  defaults to 0.
* `:attrs` - the attribute map of the produced `[:rect]`
  tag. Optional, defaults to empty.

The surround transformation adds a `[:rect]` to the document that will
completely surround the elements that are matched by the selector.

Here is an example of it in action:

```clojure
[:page
 [:circle {:class :left} [50 50] 20]
 [:circle {:class :left} [50 100] 20]
 [:circle {:class :left} [50 150] 20]

 [:circle {:class :right} [150 50] 20]
 [:circle {:class :right} [150 100] 20]
 [:circle {:class :right} [150 150] 20]

 [:dali/surround {:select [:.left] :rounded 5 :attrs {:stroke :none :fill :grey, :dali/z-index -1}}]
 [:dali/surround {:select [:.right] :rounded 5 :attrs {:stroke :none :fill :green, :dali/z-index -1}}]]
```

`:dali/surround` can only be used as a selector layout. The map under
`:attrs` will be merged with the attributes of the generated `[:rect]`
and you can use it to define things like `:dali/z-index` to make sure
that the rectangle appears below all other elements, to give it an
`:id` to refer to from other layouts, or even a `:class` to control
its appearance.

## Ghosts

At any point in the document you can insert "ghost" elements to affect
the layout. Ghosts are essentially rectangles that participate in the
calculation of the layout but don't get inserted in the exported SVG,
so you use them to "push" other elements in the layout.

The syntax of ghosts is identical to `[:rect]`:

```clojure
[:dali/ghost [x y] [width height]]
```

Here is an example:

```clojure
[:page {:stroke :black :fill :none}
 [:rect {:fill :none :stroke :lightgrey} [110 10] [100 100]]
 [:rect {:fill :none :stroke :lightgrey} [310 10] [100 100]]
 [:dali/stack {:direction :right}
  [:rect [10 10] [100 100]]
  [:dali/ghost :_ [100 100]]
  [:rect :_ [100 100]]
  [:dali/ghost :_ [100 100]]
  [:rect :_ [100 100]]]]
```

## Understanding the mechanism

### Layout application order

dali's layout mechanism **is not** based on constraints, and this
choice was made because contraint-based systems try to satisfy all
constraints at once and you end up with unpredictable behaviour that's
hard to debug.

In dali, each layout operation is applied in a predictable order which
the order that Clojure expressions are evaluated: left-to-right, and
children are laid out before their parents (deepest first).

The implications of this is that operations that are applied later can
cancel out the effects of previous operations. The first layout
operation is given the document tree, it modifies it as necessary and
the result is passed to the next layout operation. So keep in mind
that with the exception of the first one, layout operations act on the
output of the previous operation, and not on the original document
that you pass to dali.

This means that if some elements are aligned to the left by one layout
operation, and then aligned to the right by a subsequent operation,
the last one wins.

??? example

Because you can use enlive selectors to refer to any part of the
document, you may be tempted to put layouts anywhere, even at the
beginning of the document. This would work in simple cases, but if you
don't think about order you'll get unexpected results. For example,
say you want to connect 2 boxes, and somehow decide to put the
`[:connect]` tags first:

??? wrong example

What happened? Because `[:connect]` was evaluated first, the arrow was
placed according to the *original* positions of the boxes, and then
`[:stack]` changed these positions. The correct way to do it is to
make sure that `[:connect]` is applied *after* the positions of the
boxes have been finalised by any layout operations that may affect
them:

??? example

### Layouts and tranformations are composable

It is possible to apply a series of layout operations and/or document
tranformations in a composable way without having to use
selectors. This is done with a generic "layout" operation:

```clojure
[:dali/layout {:layouts [...]}]
```

For example, say you have a few elements that you'd like to stack
together and also surround them with a rounded box. This is how you
could do it with selectors (in a non-composable way):

??? example

To make your life easier, you can avoid selectors by composing the two
layouts:

```clojure
[:page
 [:dali/layout
  {:layouts
   [[:dali/stack {:direction :right :gap 10}]
    [:dali/surround {:rounded 10 :attrs {:stroke :grey :fill :none}}]]}
  [:rect {:fill :mediumslateblue :stroke-width 20} [30 50] [50 20]]
  [:rect {:fill :sandybrown} :_ [30 60]]
  [:rect {:fill :green} :_ [40 10]]
  [:rect {:fill :orange} :_ [20 40]]]]
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/composite-layout2.svg)

The tranformations are applied in the order that they appear: the
elements are stacked first and the resulting transformed elements are
implicitly selected and passed to the `:dali/surround`
transformation. This implicit selection is why in this particular
case, `:dali/surround` looks like it's being used as a nested layout,
which, as mentioned, is something that is currently not supported by
this transformation.

### Layouts and transformations are extensible

As you may have realised by now, both layouts and transformations are
driven by the same mechanism. This mechanism is uniform and extensible
and it is based on the `dali.layout/layout-nodes` multimethod, which
is defined like so:

```clojure
(defmulti layout-nodes (fn [doc tag elements bounds-fn] (:tag tag)))
```

`doc` is the whole document in a
[clojure.xml format](https://clojuredocs.org/clojure.xml/parse) (which
is what dali uses internally) instead of a hiccup format.

`tag` is the actual layout tag (for example `{:tag :dali/stack ...}`

`elements` is a collection of the elements being transformed. These
can either be the direct children of the layout tag, or some elements
selected via the `:select` selector. If `:select` is in the
attributes, collecting the elements is done automatically by the
layout mechanism, otherwise the direct children are passed.

`bounds-fn` is a function that when called on an element, will return
its bounds as `[:rect [x-pos y-pos] [width height]]`.

In order to define your own layout/transformation tag (e.g. `:foo`),
you need to `defmethod` the `dali.layout/layout-nodes` multi-method,
using `:foo` as the dispatch value, and you also need to register the
name of your tag by calling `(dali/register-layout-tag :foo)`.

`dali.layout.stack` is a good example of a dali layout, and a good
starting point if you'd like to extend the mechanism.
