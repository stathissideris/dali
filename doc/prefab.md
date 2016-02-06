# Markers and pre-fabricated shapes etc

The `dali.prefab` namespace defines some markers for arrows, fills and
SVG effects which are parameterizable.

See the individual functions in the namespace for more details.

```clojure
[:dali/page
 [:defs
  (s/css (str "polyline {stroke: black; stroke-width: 2;}"))
  (prefab/sharp-arrow-marker :sharp {:scale 2})
  (prefab/triangle-arrow-marker :triangle {:scale 2})
  (prefab/curvy-arrow-marker :curvy {:scale 2})
  (prefab/dot-marker :dot {:radius 6})
  (prefab/sharp-arrow-marker :very-sharp {:width 16 :height 36})]
 [:line {:stroke :lightgrey} [50 30] [230 30]]
 [:polyline {:dali/marker-end {:id :sharp :fill :red}} [50 80] [90 30]]
 [:polyline {:dali/marker-end :triangle} [80 80] [120 30]]
 [:polyline {:dali/marker-end :curvy} [110 80] [150 30]]
 [:polyline {:dali/marker-end :dot} [140 80] [180 30]]
 [:polyline {:dali/marker-end :very-sharp} [170 80] [210 30]]]
```
![](https://rawgit.com/stathissideris/dali/master/examples/output/markers1.svg)

## dali markers mechanism

In the markers example you may have noticed that instead of simply
using `:marker-end` we used `:dali/marker-end`, implying that this
this dali-specific syntax. This is indeed the case, and it was
necessary to provide a custom mechanism for markers because of various
shortcomings of the SVG mechanism.

The most important problem with SVG markers is that they only allow
one reference point that acts as the location at which the end of the
line is attached. The problem with that is that if for example you
have an arrowhead marker and you attach the end of the line to the tip
of the arrow, a thick line will be visible on either side of the arrow:

![](https://rawgit.com/stathissideris/dali/master/doc/marker-problem.svg)

dali overcomes that problem by defining its own syntax for markers,
which allows for two points: a base and a tip. The base is always at
point `[0 0]` while the tip is defined using the `:dali/marker-tip`
attribute. When attaching a dali marker arrowhead to the end of a
`:line` or `:polyline`, the end of the line is moved to the base of
the arrowhead, while the tip of the arrowhead ends up at the location
of the original end of the line, thus ensuring that the line will not
show through.

Calling `(dali.prefab/sharp-arrow-marker :sharp)` will produce a such
dali marker with the default dimensions. This is what the output looks
like:

```clojure
[:symbol {:id :sharp
          :class [:marker-def :sharp-arrow-marker]
          :dali/marker-tip [7.333333492279053 0]
          :style "overflow:visible;"}
 [:path
  :M [0 0]
  :L [-3.6666667461395264 4.0]
  :L [7.333333492279053 0]
  :L [-3.6666667461395264 -4.0] :z]]
```

Like normal markers, this symbol should appear in the `[:defs ...]`
part of the document so that it's reusable. All markers in the
`dali.prefab` namespace produce symbols that follow this dali marker
convention, and therefore need to be used via the `:dali/marker-end`
and `:dali/marker-start` attributes, which both take a keyword which
is the id of the symbol to use as a marker.

Another reason for having a custom marker mechanism is to allow
control of the fill of the marker form where it is being used from. As
seen in the example, `:dali/marker-end` and `:dali/marker-start` can
take a map instead of a keyword:

```clojure
[:polyline {:dali/marker-end {:id :sharp :fill :red}} [50 80] [90 30]]
```

When passing a map, all other keys apart from `:id` are used as
attributes in the generated `[:use]` keyword and can therefore affect
what the particular instance of the symbol looks like. Refer to the
[SVG spec](https://www.w3.org/TR/SVG/struct.html#UseElement) to find
out all the details on how `[:use]` tags affect the linked symbol.

If you have no need for the dali marker mechanism, you can always fall
back to normal SVG syntax markers.
