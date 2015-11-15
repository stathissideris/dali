# Related tech

## TikZ & PGF

[LaTeX library](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf)

TikZ allows the user to define `text height` and `text depth` in order to get consistent alignment of text in different boxes when the actual text has varying sizes [(p. 63)](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=63).

The use of a "matrix" (something like a flexible grid) for tackling tricky placement is beautiful ([p. 66](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=66) and [p. 255](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=255)).

[p. 264](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=264) shows how to connect nodes of a flowchart.

There are also the concepts of `at start`, `very near start`, `near start`, `midway`, `near end`,
`very near end`, and `at end` to place symbols and text on paths ([p. 351](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=351)).

`|-` and `-|` are path operations that can be used to connect two points via straight lines. `|-` means "vertical then orizontal" and `-|` means "horizontal then vertical" ([p. 142](http://www.texample.net/media/pgf/builds/pgfmanualCVS2012-11-04.pdf#page=142)).

For example, this produces 2 line segments:

```
\draw (a.north) |- (b.west);
```

and this produces 4 line segments:

```
\draw[color=red] (a.east) -| (2,1.5) -| (b.north);
```



