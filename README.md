# dali

> I do not understand why, when I ask for grilled lobster in a restaurant, I'm never served a cooked telephone.
> -- Salvador Dalí

dali is a Clojure library for representing the SVG graphics format. It
allows the creation and manipulation of SVG files. The syntax used to
describe the graphical elements is based on
[hiccup](https://github.com/weavejester/hiccup) with a few extensions.

Here's a hello world for dali:

```clojure
(require '[dali.io :as io])

(def document
 [:dali/page
  [:circle
   {:stroke :indigo :stroke-width 4 :fill :darkorange}
   [30 30] 20]])

(io/render-svg document "hello-world.svg")

;;you can also rasterize directly using Batik:
(io/render-png document "hello-world.png")
```
![](https://cdn.rawgit.com/stathissideris/dali/master/examples/output/hello-world.svg)

Here's a more substantial example of what you can do with dali:

![](https://cdn.rawgit.com/stathissideris/dali/master/examples/output/architecture.svg)

[source](blob/master/examples/src/dali/examples/architecture.clj)

## Using dali in your project

Before adding dali as a dependency, please consider that it's still
alpha quality and the API and syntax can (and very likely will)
change. Just add this to the dependencies of your `project.clj`:

```
[dali "0.7.0"]
```

## Documentation

* [Basic syntax](doc/syntax.md) -- Start here.
* Compare the
  [examples source](https://github.com/stathissideris/dali/blob/master/examples/src/dali/examples.clj)
  to the
  [rendered SVGs](https://github.com/stathissideris/dali/tree/master/examples/output).
* [Layout](doc/layout.md) -- the main value that dali adds on top of vanilla SVG.
* [Pre-fabricated elements](doc/prefab.md)
* [How-to](doc/howto.md) --
  Task-driven documentation, recipes for common tasks.

## Roadmap

Planned for the future:

* Porting basic functionality to ClojureScript.
* More pre-fabricated elements.

## License

Copyright © 2014-2016 Stathis Sideris

Distributed under the Eclipse Public License, the same as Clojure.
