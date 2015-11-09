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
