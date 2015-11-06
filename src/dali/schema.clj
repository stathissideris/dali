(ns dali.schema
  (:require [dali.syntax :as dali]
            [dali.syntax :as syntax]
            [schema.core :as s]
            [clojure.pprint :refer [pprint]]))

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
  (s/either ;;cond-pre fails with complex schemas
   (elms [(s/eq :matrix) :op    (elms s/Num [:a :b :c :d :e :f]) :params])
   (elms [(s/eq :translate) :op (one-of (elms s/Num [:x :y]) (elms s/Num [:x])) :params])
   (elms [(s/eq :scale) :op     (one-of (elms s/Num [:x :y]) (elms s/Num [:x])) :params])
   (elms [(s/eq :rotate) :op    (one-of (elms s/Num [:a :x :y]) (elms s/Num [:a])) :params])
   (elms [(s/eq :skew-x) :op    (elms s/Num [:a]) :params])
   (elms [(s/eq :skew-y) :op    (elms s/Num [:a]) :params])))

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
           (s/either (elms [Point :pos Dimensions :dimensions])
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
  (s/either ;;cond-pre fails with complex schemas
   (elms [(s/enum :move-to :M) :op        [(s/one Point :point)] :params])
   (elms [(s/enum :move-by :m) :op        [(s/one Point :point)] :params])
   (elms [(s/enum :line-to :L) :op        [(s/one Point :point)] :params])
   (elms [(s/enum :line-by :l) :op        [(s/one Point :point)] :params])
   (elms [(s/enum :horizontal-to :H) :op  [(s/one s/Num :delta)] :params])
   (elms [(s/enum :horizontal-by :h) :op  [(s/one s/Num :delta)] :params])
   (elms [(s/enum :vertical-to :V) :op    [(s/one s/Num :delta)] :params])
   (elms [(s/enum :vertical-by :v) :op    [(s/one s/Num :delta)] :params])
   (elms [(s/enum :cubic-to :C) :op       (elms [Point :p1 Point :p2 Point :p3]) :params])
   (elms [(s/enum :cubic-by :c) :op       (elms [Point :p1 Point :p2 Point :p3]) :params])
   (elms [(s/enum :symmetrical-to :S) :op (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :symmetrical-by :s) :op (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :quad-to :Q) :op        (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :quad-by :q) :op        (elms [Point :p1 Point :p2]) :params])
   (elms [(s/enum :arc-to :A) :op         PathArcSpec :params])
   (elms [(s/enum :arc-by :a) :op         PathArcSpec :params])
   (elms [(s/enum :close :Z :z) :op       (s/eq nil) :params])))

(def PathTag
  {:tag (s/eq :path)
   :attrs (cattr [PathOperation])})

(declare Tag)

(def GenericTag
  {:tag TagName
   (opt :attrs) (s/both AttrMap (s/pred #(not (:dali/content-attr %)) :no-content-attr))
   (opt :content) (s/either
                   [s/Str]
                   [(s/pred #(nil? (s/check Tag %)) :nested-tag)])})

(def Tag (s/either LineTag CircleTag EllipseTag RectTag PolylineTag PolygonTag PathTag GenericTag))
;;(def Tag GenericTag)

(def Document Tag)

(defn validate [document]
  (let [c (s/check Document (syntax/dali->ixml document))]
    (if (nil? c)
      document
      (throw (ex-info "document does not match dali schema"
                      {:document document
                       :bad-parts c})))))

(defn print-fail [e]
  (-> e ex-data :bad-parts pprint))
