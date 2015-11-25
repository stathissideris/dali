# TODO

## Bugs

* Transforms in the parents of elements are not taken into account by
  the layout code. Also anything else that would affect dimensions is
  not taken into account (like font properties).

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
* `[:matrix]` layout (see TikZ)
