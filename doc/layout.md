# Layout

Layout functionality in dali allows the placement of elements without
knowing their exact dimensions in advance. All layouts have been
implemented as custom tags, that all use the `:dali/` prefix.

In most cases, the layout tag will contain elements that will be
tranformed accordingly to conform to the desired layout. This can mean
that the element is translated to a new position, but this is not
always the case.

Layouts tags can be nested within other layout tags.

dali's layout mechanism **is not** based on constraints. Each layout
operation is applied to its elements in a predictable order, and
operations that are applied later can cancel out the effects of
previous operations. Layouts are resolved in the same order that
Clojure expressions are evaluated: left-to-right, and children are
laid out before their parents. The implications of this may not be
immediately clear, but they will hopefully become obvious with a few
examples.

Apart from acting on children elements, layouts can also "select"
elements from other parts of the document and transform them. For
this, dali uses the [enlive](https://github.com/cgrand/enlive)
selector syntax. Those "selector layouts" participate in the normal
order of resolution of layouts.

[Apache Batik](http://xmlgraphics.apache.org/batik/) is used for
figuring out the sizes of various elements.

## Basic layouts

### Stack

#### Quick ref:

```
[:dali/stack {:direction :up, :anchor :bottom, :gap 0}]
```
* `:direction` - the direction of accumulation
  * one of `:up`, `:down`, `:left`, `:right`
  * default: `:up`
  * optional
* `:anchor` - the anchor used to align the elements
  * one of: `:top`, `:bottom`, `:left`, `:right`, `:top-left`, `:top-right`, `:bottom-left`, `:bottom-right`
  * default: sensible default selected based on `:direction`
  * optional
* `:gap` - the gap to leave between elements
  * double
  * default: `0`
  * optional
* `:position` - the position of the whole layout relative to its parent
  * `[x y]` - double
  * default: `[0 0]`
  * optional
* `:select` - an enlive selector that will transform elements from the
  document instead of tranforming the children of the layout tag

This is how you stack elements:

```clojure
[:page {:width 200 :height 40 :stroke :none}
 [:dali/stack
  {:position [10 10] :anchor :left :direction :right}
  [:rect {:fill :mediumslateblue} :_ [50 20]]
  [:rect {:fill :sandybrown} :_ [30 20]]
  [:rect {:fill :green} :_ [40 20]]
  [:rect {:fill :orange} :_ [20 20]]]]
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

### Align

## Understanding the mechanism

### Layout application order

### Layouts are composable

## More placement layouts

### Place

### Matrix

## "Layouts" that produce stuff 

### Connect

### Surround

## Make your own
