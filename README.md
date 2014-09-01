# dali

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
    {:stroke :blue :stroke-width 4 :fill :yellow}
    [50 50] 40]])

(-> document (s/dali->hiccup) (s/spit-svg "simple.svg"))
```

![](https://raw.githubusercontent.com/stathissideris/dali/master/examples/output/hello-world.svg)

## Using dali in your project

Before adding dali as a dependency, please consider that it still a
very immature project where the API and syntax can (an very likely
will) change. Having said that, you are very welcome to give it a
go. Just add this to the dependencies of your project.clj:

???TODO

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

## Roadmap

???TODO

## License

Copyright © 2014 Stathis Sideris

Distributed under the Eclipse Public License, the same as Clojure.
