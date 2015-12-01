# Development Log

Thoughts and observations during development.

## 8th November 2015

**commit:** 7a4b4c1

In the past few days I have resurrected the project by cleaning up all
the hastily-written code (including tests and examples).

I then started refactoring the DSL to make the transform syntax
consistent with the rest of the language, but I quickly run into
unexplained breakages etc, that were hard to track down. Because of
that, I decided to add a prismatic schema to validate the Dali DSL.

It turned out pretty hard to use prismatic schema to validate the
irregular hiccup-like syntax, and the (also irregular) syntax that I
had introduced. The irregularity of hiccup was also a problem in
various other parts of the code (the fact that the attribute map is
optional was annoying), so I decided that the first step in the dali
pipeline would be to convert dali hiccup into an intermediate
clojure.xml-like format, that I call ixml. ixml will also be used as
the central representation for internal operations in dali.

ixml is essentially like clojure.xml, but the elements of the custom
dali syntax have been collected in the `dali/content-attr` element,
and they have been split in logical units that make then easier to
validate. So now there is a schema, but still with certain
shortcomings: I had to resort to using the now deprecated `either`,
which destroys the error information for schemas that don't match. I
have
[received some advice](https://github.com/Prismatic/schema/issues/295)
on how to handle this, but I need to look into schemas a bit more
closely. Also, the mutually recursive nature of `GenericTag` and `Tag`
proved quite awkward to handle.

The ixml refactor is still in progress.

## 2nd December 2015

**commit:** 1766ac0

Having worked on this for a few weeks now, it's time to stop and
think. I have managed to get Dali to a much more usable state with
quite a few of the planned features, but now that I understand it
better, the task seems much more daunting than before.

I have maintained the pattern of keeping all the purely syntactic
transformations in the `dali.syntax` namespace, and any
transformations that require geometry information in
`dali.layout`. Getting geometry information such as position and
dimensions still relies on Batik, and I encountered several gotchas
while trying to use it.

Initally, Dali took a very naive approach by using Batik minimally for
bounds: it would insert the element in question into an empty Batik
DOM document and it would request the bounds, removing it
afterwards. It soon became apparent that this completely disregarded
the transformations of the parents of the element. Fixing that
required major refactoring to build a `dali.layout` <-> Batik bridge,
similar to the Batik DOM <-> Batik GVT bridge, that keeps the document
being resolved by the layout mechanism in Dali in sync with the Batik
DOM and SVG. This is regrettably very stateful and hard to debug but
it works... buuut there is a gotcha: You still have to explicitly look
at all the parents of the GVT node that corresponds to your Dali node
and concatenate their transform matrices (although there is a
`getGlobalTransform()` methods that I should probably check). So this
is more correct than before, but at this stage, it's a bit unclear to
me whether that also takes into account the CSS transforms (which seem
to have precedence over attributes). Basic testing is showing that it
doesn't take into account CSS applied to text, which is bad news.

The other incomplete and confusing thing is that currently layouts are
not applied as they appear in the document, but instead nested layouts
(the ones containing elements) are applied first, followed by selector
layouts (the ones that refer to existing elements), making reasoning
of how the document is going to be laid out harder than it should
be. Also, I haven't been able to use the OR enlive selector to select
2 or 3 specific elements to participate in a layout.

Another suprising "feature" is that all dali layouts like `[:stack]`
are not affected by the transforms of parent elements. It's not the
end of the world, but the result is surprising given the conventions
of SVG.

It's worth noting that even after all this effort dali layouts are not
entirely correct--there is still a category of layouts that will be
incorrect, mainly because of the behaviour described in the previous
paragraph.

### SVG shortcomings

SVG markers for arrowheads were disappointing because of the
overshooting problem (depending on where you put the reference point,
either the arrow tip will overshoot the end of the path, or the end of
the path (especially thick strokes) will show on either side of the
tip of the arrow). The other shortcoming is that there is no easy way
to change the fill and stroke of the arrowhead to match those of the
path. I ended up writing a completely custom mechanism for markers
which uses `<symbol>` and `<use>` to address both problems for
`<line>` and `<polyline>` (support for arbitrary path still
pending). Dali markers move the last (or first) point to the base of
the marker, and make sure that the tip of the marker is located where
the path used to end. They also support passing attributes to the
generated `<use>` tag to allow for a per-case fill/stroke
customization. It works (but not in Safari!), but it still felt like a
huge amount of effort for something simple like arrowheads.

Multi-line text is something important that still needs to be tackled,
especially blocks of flowing text, as SVG have no viable solution for
it.

### A glimmer of hope

Despite all the negativity, there were a lot of important features
that were implemented, such as connector arrows, selector layouts,
align and matrix layouts, z-index, dali markers etc. The syntax and
layout mechanisms are now extensible. The testing workflow is also
very good, with fixtures being pre-rendered SVG documents that are
compared to SVG documents for differences, which provides a lot of
confidence during major refactors.

I was able to use dali for its first real use case to describe a
system architecture for a code review with very encouraging
results. It was a good use case that pushed the library to its limits
by combining most features (including generating CSS using the garden
library). I was even able to embed some basic Javascript to toggle the
visibility of part of the diagram.

### Future direction

I'm getting to the point where I'm beginning to wonder whether
targetting SVG directly is a good idea. Maybe defining a more
constrained language and just rendering to SVG is more viable--similar
to what `thi.ng` does. I also have a strong desire to remove Batik
from the equation, so far it has added a lot of complexity, obscurity
and state in the middle of the system without offering much that I
couldn't do by bringing my geometry up to speed. The two main things
make me think that I won't be getting rid of Batik anytime soon is CSS
parsing (does it even do that?) and text handling (which super-hard to
replace unless I seriously go into the neverland of font unicorns and
kerning fairies).

I also have to think what this all means for the
Dali-in-the-browser-via-Clojurescript story, am I asking for too much?
