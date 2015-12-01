# TODO

## Bugs

* Layouts are not applied in the order that they appear, nested
  layouts are applied first, then selectors are applied. Change that
  so that they are applied in document order.
* Nested layouts are not affected by the transforms of parent elements
  and this is surprising.
* Dali markers are not displayed in Safari.

## Features

* Bounds calculation caching
* Native calculation of "easy" bounds
* Dali markers for arbirary paths
* "path" connector type: a flowing bezier curve
* Allow offset for connector start and/or end points
* `[:place]` layout that places something in relation to something
  else. Use case: connector labels.
* `[:at]` to appear generically in the place of coordinates and to
  refer to other elements or attributes of elements (such as anchors)
  so that you can draw paths that start/end at anchors. Will require
  multiple passes for layout resolution, is this a good idea?
* A solution for multi-line text
* A solution for a box that will scale and position itself to include
  another element, e.g. text.
* When it comes to positioning and/or scaling for layouts, allow the
  user to define a grid to snap to for positions, and, more
  importantly, a number of acceptable "stops" for scaling to minimise
  visual noise.

## Things that should be possible

* https://medium.com/the-year-of-the-looking-glass/what-kind-of-design-work-should-i-do-15726d81904f#.ayafjt7zi

# Done

* Transforms in the parents of elements are not taken into account by
  the layout code. Also anything else that would affect dimensions is
  not taken into account (like font properties).
