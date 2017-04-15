# Version history

## 0.7.4

* Fix destructuring in prefab namespace
  [(issue 8)](https://github.com/stathissideris/dali/issues/8).

## 0.7.2

### Bug fix

* Layout was
  [failing for particular valid documents](https://github.com/stathissideris/dali/issues/5)
  and it's now fixed.

## 0.7.1

### Features

* Support hiccup shorthand for classes and element ID: `[:foo.bar.baz#id]`
* `:_` elements are implicitly expanded to ghost elements when they
  appear in the content part of elements that are not `:use`, `:line`,
  `:circle`, `:ellipse`, `:rect`, `:polyline`, `:polygon` or `:path`.
  See example matrix4.

### Internals

* Make the use of batik less stateful and abstract it better to open
  the way for ClojureScript support.

## 0.7.0

### Features

* Make transform syntax consistent: instead of, `[[:translate [10 20]]
  [:scale [20]]]` you now say `[:translate [10 20] :scale [20]]`.
* Prismatic schema to validate dali documents (error reporting needs
  work).
* ixml (intermediate XML) internal format as a first step of
  transforming dali to SVG. `dali->ixml` and `ixml->xml` functions.
* Enlive selector-based layouts. Stack and distribute layouts
  converted.
* New align layout.
* New composite layout.
* Stack layout `:position` attribute is now optional (position of
  first element is used if not passed explicitly).
* `drop-shadow` prefab.
* Syntax for embedded CSS
* dali markers that solve the marker overshoot problem and being able
  to affect fill/stroke etc of markers. There were 2 pain points:
  Inheritance of fill and/or stroke from the parent path, and the fact
  that either the arrow point overshoots the end of the path, or the
  path shows on either side of triangular markers.
* Matrix layout (similar to a grid, but with row/column dimensions
  that depend on the contents)
* z-index via the `dali/z-index` attribute
* :dali/surround layout
* :dali/place layout
