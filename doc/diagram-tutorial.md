# Diagram Tutorial

Creating graphics with the dali syntax may be convenient for simple
use cases, but for more involved diagrams are better expressed in
terms that fit your "visual language". In this tutorial I will explain
how you can use dali to build this diagram:

![](https://rawgit.com/stathissideris/dali/master/examples/output/architecture.svg)

We are going to build the visual language of the diagram from the
bottom-up by defining functions that produce snippets of dali
syntax. Notice that the most commonly used visual element is a circle
representing a component with some text in it. This is easy to achieve
with this function:

```clojure
(def circle-radius 50)

(defn circle-text
  ([id text cl]
   (circle-text id text cl nil))
  ([id text cl radius]
   (let [radius (or radius circle-radius)]
    [:dali/align {:axis :center}
     [:circle {:id id :class cl :filter "url(#ds)"} :_ radius]
     [:text {:font-family "Verdana" :font-size 14} text]])))
```

We use `[:dali/align]` with a `center` axis to place the text and the
circle on top of each other, with their centers aligned. The align tag
is not passed a `:relative-to` parameter, so it defaults to aligning
everything relative to the first element, in this case the circle. We
pass in an id and we assign it to the circle so that we have a way to
refer to the circles when drawing the arrows. The class parameter will
allow us to control the fill colour of the circle.

Let's test this out:

??? code

??? diagram

But wait! The observant reader will have noticed that the final
diagram has circles with multi-line text. Multiline text is bits of
text stacked on top of each other (not entirely correct, but for this
tutorial let's pretend that it is). You can render this like so:

```clojure
(defn text-stack [texts]
  (vec (concat [:dali/stack {:direction :down :gap 6}]
               (map #(vector :text {:font-family "Verdana" :font-size 14} %) texts))))
```

Here we have a stack tag and we concat multiple `:text` tags as
children, each containing a string from `texts`. Notice that the
anonymous function passed to `map` uses the `vector` function to
construct the tag, which is simply equivalent to saying
`[:text ...]`. Also, because `concat` returns a lazy sequence, you
have to use `vec` to turn `concat`'s result back into a vector,
because dali tags have to be represented as vectors.

So the circle text function now becomes:

```clojure
(defn circle-text
  ([id text cl]
   (circle-text id text cl nil))
  ([id text cl radius]
   (let [radius (or radius circle-radius)]
    [:dali/align {:axis :center}
     [:circle {:id id :class cl :filter "url(#ds)"} :_ radius]
     (text-stack (string/split-lines text))])))
 ```
