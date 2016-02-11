# Limitations

Too many libraries leave the nasty bits out of the documentation, but
it's preferable to be upfront with your users. Here is a list of
limitations of the dali library that you should be aware of:

* **Fonts and the web:** dali cannot magically fix the normal problems
  that you encounter when using various fonts on the web -- if you
  have used a fancy font, but the computer displaying does not have
  it, the size/position will look wrong. This could be solved by
  turning all text into curves (sometimes called "baking") but:
* **dali cannot bake text:** currently there is no way to bake the
  text into curves to avoid the missing font problems. Baking would
  have certain disadvantages (it increases the size of the document).
* **Batik rasterization is buggy:** I have found that in certain
  sizes, shapes with filters will not render when you rasterize the
  document. This is a Batik bug, and I found that changing the size by
  a single unit seems to fix it. If you have a fully automated
  pipeline that produces PNGs from SVGs, you may want to try using
  Inkscape instead
  [to rasterize your SVGs](http://tavmjong.free.fr/INKSCAPE/MANUAL/html/CommandLine-Export.html).
* **dali markers for `line` and `multiline` only:** The dali markers
  cannot currently be attached to a `path` -- this will be fixed in
  the future. Of course, normal SVG markers can.
* **No ClojureScript version:** But hopefully there will be one soon.
