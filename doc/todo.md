# TODO

## Bugs

* Curvy arrow width and height changes (make it 20, 20), cause the
  arrowhead to render away from the end of the line
* Transforms in the parents of elements are not taken into account by
  the layout code. Also anything else that would affect dimensions is
  not taken into account (like font properties).
* Fix and test all markers so that their points don't overshoot the
  end of the line.

## Features

* Pass style map to :connect
* Bounds calculation caching
* Native calculation of "easy" bounds
* Embedded CSS
* Smart markers that are sensitive to colour etc (how??)
