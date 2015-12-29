# Pre-fabricated shapes etc

The `dali.prefab` namespace defines some markers for arrows, fills and
SVG effects which are parameterizable.

[//]: # (TODO: Explain about Dali markers here)

See the individual functions in the namespace for more details.

```clojure
[:page
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
