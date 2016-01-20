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
 [:page
  [:circle
   {:stroke :indigo :stroke-width 4 :fill :darkorange}
   [30 30] 20]])

(io/render-svg document "hello-world.svg")

;;you can also rasterize directly using Batik:
(io/render-png document "hello-world.png")
```

![](https://rawgit.com/stathissideris/dali/master/examples/output/hello-world.svg)

## Using dali in your project

Before adding dali as a dependency, please consider that it's still
alpha quality and the API and syntax can (and very likely will)
change. Just add this to the dependencies of your `project.clj`:

```
[dali "0.7.0"]
```

## Documentation

* [Basic syntax](doc/syntax.md) -- Start here
* [Layout](doc/layout.md)
* [Pre-fabricated elements](doc/prefab.md)
* [How-to](doc/howto.md) -
  Task-driven documentation, recipes for common tasks.
* [The dali pipeline](pipeline.md)

## Roadmap

Planned for the future:

* Porting basic functionality to ClojureScript.
* More stock shapes.

## License

Copyright © 2014-2015 Stathis Sideris

Distributed under the Eclipse Public License, the same as Clojure.
