# TODO

## Tasks

* Remove retrograde dependency and re-write parts that use it
* Simplify/tidy up/clarify layout code
* Comment layout code
* Remove explicit batik dependency from layout code to open the way for browser dali

## Documentation

* z-index
* classes being in vectors

## Bugs

* Matrix layout does not support :_
* `:page` should be `:dali/page`
* Possible bug: I suspect that selector layouts that don't match
  anything throw an exception. It should be a warning at most.
* dali markers are not displayed in Safari.
* PNG transcoding of drop-shadow fails when the specified width is
  double the original (200->400). dali fails by not rendering the
  circle and drop shadow effect, batik's command-line transcoder
  throws an exception:

  `java -jar ../../../third-party/batik-1.8-bin/batik-rasterizer-1.8.jar -w 400 drop-shadow.svg`

  Interestingly, width 399 in the command line works ok, and using
  scale 1.9 in dali fixes it too.

## Features

* `:dali/place` support :distance attribute (or :gap?)
* `:dali/place` on path
* Align relative to biggest/smallest (which means widest/narrowest or
  tallest/shortest depending on the axis)
* Allow using `:dali/surround` as a nested layout.
* dali markers for arbirary paths
* Better error reporting by showing original form in the error (use meta)
* "path" connector type: a flowing bezier curve
* Connect the projected centers of elements
* `[:at]` to appear generically in the place of coordinates and to
  refer to other elements or attributes of elements (such as anchors)
  so that you can draw paths that start/end at anchors. Will require
  multiple passes for layout resolution, is this a good idea?
* Allow offset for connector start and/or end points
* Bounds calculation caching
* Native calculation of "easy" bounds
* A solution for multi-line text
* When it comes to positioning and/or scaling for layouts, allow the
  user to define a grid to snap to for positions, and, more
  importantly, a number of acceptable "stops" for scaling to minimise
  visual noise.
* Use a custom [XMLReader](http://docs.oracle.com/javase/7/docs/api/org/xml/sax/XMLReader.html) for Batik's [TranscoderInput](https://xmlgraphics.apache.org/batik/javadoc/org/apache/batik/transcoder/TranscoderInput.html) to avoid having to covert the document into an SVG string before rasterization.
* Allow setting width/height and/or DPI when rasterizing image (makes a huge differrence on retina screens - see how you can set DPI on inkscape). Also, see [here for code](http://www.programcreek.com/java-api-examples/index.php?api=org.apache.batik.transcoder.image.PNGTranscoder).

## Things that should be possible

* https://medium.com/the-year-of-the-looking-glass/what-kind-of-design-work-should-i-do-15726d81904f#.ayafjt7zi

# Done

* Nested layouts are not affected by the transforms of parent elements
  and this is surprising.
* `[:place]` layout that places something in relation to something
  else. Use case: connector labels.
* `[:surround]` layout to enclose elements in boxes
* Layouts are not applied in the order that they appear, nested
  layouts are applied first, then selectors are applied. Change that
  so that they are applied in document order.
* Transforms in the parents of elements are not taken into account by
  the layout code. Also anything else that would affect dimensions is
  not taken into account (like font properties).
