# Related tech

## TikZ & PGF

[LaTeX library](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf)

TikZ allows the user to define `text height` and `text depth` in order to get consistent alignment of text in different boxes when the actual text has varying sizes [(p. 63)](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=63).

The use of a "matrix" (something like a flexible grid) for tackling tricky placement is beautiful ([p. 66](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=66) and [p. 255](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=255)).

[p. 264](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=264) shows how to connect nodes of a flowchart.

For example, here is a connection:

```
\path (decide) -| node [near start] {yes} (update);
```

This reads: "draw a path (arrow?) from node 'decide' to node 'update',
by drawing a horizontal line first and then a verical line, and near
the start of the whole path, place the 'yes' text". `near start` is
explained on [p. 195](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=195).

There are also the concepts of `at start`, `very near start`, `near
start`, `midway`, `near end`, `very near end`, and `at end` to place
symbols and text on paths ([p. 351](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=351)).

`|-` and `-|` are path operations that can be used to connect two
points via straight lines. `|-` means "vertical then orizontal" and
`-|` means "horizontal then vertical" ([p. 142](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=142)).

For example, this produces 2 line segments:

```
\draw (a.north) |- (b.west);
```

and this produces 4 line segments:

```
\draw[color=red] (a.east) -| (2,1.5) -| (b.north);
```

## SVG 1.1

### def and use

You can define and re-use elements from anywhere in the document, and
if the original element has no style definition, [you can set your own style](http://tutorials.jenkov.com/svg/use-element.html).

### Markers

SVG 1.1 connectors are crap, if they are triangular it's impossible to avoid the [overshoot problem](https://bugs.launchpad.net/inkscape/+bug/171284).

## SVG 2

### Markers

SVG 1.1 markers are a bit disappointing, so SVG 2 attempts to fix them
by allowing them to inherit some properties of the path that they are
applied to. [Here is an example](https://svgwg.org/svg2-draft/painting.html#VertexMarkerProperties)
of how this is done.

