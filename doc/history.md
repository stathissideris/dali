# Version history
## 0.7.0

* Prismatic schema to validate dali documents (error reporting needs work).
* Make transform syntax consistent: instead of, `[[:translate [10 20]] [:scale [20]]]` you now say `[:translate [10 20] :scale [20]]`.
* ixml (intermediate XML) internal format as a first step of transforming dali to SVG. `dali->ixml` and `ixml->xml` functions.
* Enlive selector-based layouts. Stack and distribute layouts converted.
* New align layout.
* New composite layout.
* Stack layout `:position` attribute is now optional (position of first element is used if not passed explicitly).
* Automatic page calculation based on contents (applied during `resolve-layout`).
* `drop-shadow` prefab.
