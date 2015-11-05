(ns dali.schema
  (:require [dali.syntax :as dali]
            [dali.syntax :as syntax]
            [schema.core :as s]))

(def one-of s/cond-pre)
(def opt s/optional-key)

(def AttrName (s/named (one-of s/Str s/Keyword) :attr-name))

(def AttrValue s/Any)

(defn- elms
  "Produces a schema that matches vectors (or sequences) that contains
  the passed type and have a length same as names. The names are used
  for each element of the vector. In the single param arity, elms is
  simply [schema1 name1 schema2 name2 ...] that are partitioned to
  pairs and wrapped individually with prismatic.schema/one."
  ([pairs]
   (mapv (fn [[s n]] (s/one s n)) (partition 2 pairs)))
  ([s names]
   (mapv #(s/one s %) names)))

(def TransformOperation
  (one-of
   (elms [(s/eq :matrix) :operation (elms s/Num [:a :b :c :d :e :f]) :params])
   (elms [(s/eq :translate) :operation (one-of (elms s/Num [:x :y]) (elms s/Num [:x])) :params])
   (elms [(s/eq :scale) :operation (one-of (elms s/Num [:x :y]) (elms s/Num [:x])) :params])
   (elms [(s/eq :rotate) :operation (one-of (elms s/Num [:x :y]) (elms s/Num [:a])) :params])
   (elms [(s/eq :skew-x) :operation (elms s/Num [:a]) :params])
   (elms [(s/eq :skew-y) :operation (elms s/Num [:a]) :params])))

(def AttrMap {(opt :transform) [TransformOperation]
              AttrName AttrValue})

(def TagName (s/named s/Keyword :tag-name))

(def Radius (one-of s/Num (s/eq :_)))
(def Point (one-of (elms s/Num [:x :y]) (s/eq :_)))
(def Dimensions (one-of (elms s/Num [:w :h]) (s/eq :_)))

;;TODO ref

(defn cattr [value]
  (merge AttrMap {:dali/content-attr value}))

(def LineTag
  {:tag (s/eq :line)
   :attrs (cattr [Point])})

(def CircleTag
  {:tag (s/eq :circle)
   :attrs (cattr (elms [Point :center Radius :radius]))})

(def EllipseTag
  {:tag (s/eq :ellipse)
   :attrs (cattr (elms [Point :center Radius :radius1 Radius :radius2]))})

(def RectTag
  {:tag (s/eq :rect)
   :attrs (cattr
           (one-of (elms [Point :pos Dimensions :dimensions])
                   (elms [Point :pos Dimensions :dimensions s/Num :rounded])
                   (elms [Point :pos Dimensions :dimensions Dimensions :rounded])))})

(def PolylineTag
  {:tag (s/eq :polyline)
   :attrs (cattr [Point])})

(def PolygonTag
  {:tag (s/eq :polygon)
   :attrs (cattr [Point])})

(def PathArcSpec
  (elms
   [Point  :pos
    s/Num  :x-axis-rotation
    s/Bool :large-arc-flag
    s/Bool :sweep-flag
    Point  :pos2]))

(def PathOperation
  (one-of
   (elms [(s/enum :move-to :M) :operation        [Point] :params])
   (elms [(s/enum :move-by :m) :operation        [Point] :params])
   (elms [(s/enum :line-to :L) :operation        [Point] :params])
   (elms [(s/enum :line-by :l) :operation        [Point] :params])
   (elms [(s/enum :horizontal-to :H) :operation  [s/Num] :params])
   (elms [(s/enum :horizontal-by :h) :operation  [s/Num] :params])
   (elms [(s/enum :vertical-to :V) :operation    [s/Num] :params])
   (elms [(s/enum :vertical-by :v) :operation    [s/Num] :params])
   (elms [(s/enum :cubic-to :C) :operation       (elms [Point :p1 Point :p2 Point :p3]) :params])
   (elms [(s/enum :cubic-by :c) :operation       (elms [Point :p1 Point :p2 Point :p3]) :params])
   (elms [(s/enum :symmetrical-to :S) :operation (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :symmetrical-by :s) :operation (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :quad-to :Q) :operation        (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :quad-by :q) :operation        (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :arc-to :A) :operation         PathArcSpec :params])
   (elms [(s/enum :arc-by :a) :operation         PathArcSpec :params])
   (elms [(s/enum :close :Z) :operation          [] :params])))

(def PathTag
  {:tag (s/eq :path)
   :attrs (cattr [PathOperation])})

(def GenericTag
  {:tag TagName
   (opt :attrs) AttrMap
   (opt :content) [(s/recursive #'GenericTag)]})

(def Tag (one-of GenericTag LineTag CircleTag EllipseTag RectTag
                 PolylineTag PolygonTag
                 PathTag
                 ))

(def Document Tag)

(defn validate [document]
  (s/validate Document (syntax/dali->xml document))
  document)
