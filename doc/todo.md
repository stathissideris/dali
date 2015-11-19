# TODO

## Bugs

* Curvy arrow width and height changes (make it 20, 20), cause the
  arrowhead to render away from the end of the line
* Transforms in the parents of elements are not taken into account by
  the layout code. Also anything else that would affect dimensions is
  not taken into account (like font properties).

## Features

* Smart markers that are sensitive to colour etc (how??). There are 2
  pain points: Inheritance of fill and/or stroke from the parent path,
  and the fact that either the arrow point overshoots the end of the
  path, or the path shows on either side of triangular markers.
* Bounds calculation caching
* Native calculation of "easy" bounds
