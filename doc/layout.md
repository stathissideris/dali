# Layouts and document operations

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
* `:select` - an enlive selector that will transform elements from the
  document instead of tranforming the children of the layout tag
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

???(stack without a position)

???(stack with a selector)

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

This layout will align the edges of elements either in relation to the
corresponding edge of another element, or in relation to a "guide"
which a theoretical horizontal or vertical line on the screen.

This is a simple case where the bottom edges of the last three circles
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

### Matrix

#### Quick ref:

## Document operations

### Connect

### Surround

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

### Layouts are composable

## Make your own
