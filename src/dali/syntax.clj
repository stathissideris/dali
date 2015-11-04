(ns dali.syntax
  (:require [clojure.java.io :as java-io]
            [clojure.pprint :refer [cl-format]]
            [clojure.string :as string]))

(def attr-key-lookup
  (-> "attr-key-lookup.edn" java-io/resource slurp read-string))

(def map-path-command
  {:move-to :M ;;:M [x y]
   :move-by :m
   :line-to :L ;;:L [x y]
   :line-by :l
   :horizontal-to :H ;;:H 10
   :horizontal-by :h
   :vertical-to :V ;;:V 10
   :vertical-by :v
   :cubic-to :C ;;:C [x1 y1] [x2 y2] [x y]
   :cubic-by :c
   :symmetrical-to :S ;;:S [x2 y2] [x y]
   :symmetrical-by :s
   :quad-to :Q ;;:Q [x1 y1] [x y]
   :quad-by :q
   :arc-to :A ;;:A [rx ry] x-axis-rotation large-arc-flag(true/false) sweep-flag(true/false) [x y]
   :arc-by :a
   :close :Z})

(defn- split-params-by-keyword [params]
  (->> params
       (partition-by keyword?)
       (partition-all 2)
       (map (fn [[k v]] [(first k) v]))))

(defn- convert-path-command [[command v]]
  (let [bool (fn [x] (if x 1 0))
        c (or (map-path-command command) command)]
    (string/trim
     (str
      (name c)
      " "
      (condp = (-> c name string/lower-case keyword)
        :m (let [[[x y]] v] (str x " " y))
        :l (let [[[x y]] v] (str x " " y))
        :h (first v)
        :v (first v)
        :c (let [[[x1 y1] [x2 y2] [x y]] v]
             (cl-format nil "~d ~d, ~d ~d, ~d ~d" x1 y1, x2 y2, x y))
        :s (let [[[x2 y2] [x y]] v]
             (cl-format nil "~d ~d, ~d ~d" x2 y2, x y))
        :q (let [[[x1 y1] [x y]] v]
             (cl-format nil "~d ~d, ~d ~d" x1 y1, x y))
        :t (let [[[x y]] v]
             (cl-format nil "~d ~d" x y))
        :a (let [[[rx ry] x-axis-rotation large-arc-flag sweep-flag [x y]] v]
             (cl-format nil "~d ~d, ~d, ~d, ~d, ~d ~d"
                        rx ry
                        x-axis-rotation
                        (bool large-arc-flag)
                        (bool sweep-flag)
                        x y))
        :z nil)))))

(defn- convert-path-spec [spec]
  (let [params (split-params-by-keyword spec)]
    (string/join
     " " (map convert-path-command params))))

;;the following implements a behaviour that also exists in hiccup: if
;;the first element of the content is a seq, it is unwrapped in order
;;to make the use of map, filter etc more convenient. See tests for
;;group and polygon for an examples.
(defn- unwrap-seq [coll]
  (when coll
    (if (seq? (first coll)) (first coll) coll)))

(def
  convertors
  {:page
   (fn [content]
     (concat [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.2"
                    :xmlns:xlink "http://www.w3.org/1999/xlink"}] content))
   :use
   (fn [[ref [x y]]]
     (if (and ref x y)
       [:use {:xlink:href (str "#" (name ref)) :x x :y y}]
       [:use {}]))
   :line
   (fn [[[x1 y1] [x2 y2]]]
     [:line {:x1 x1, :y1 y1, :x2 x2, :y2 y2}])
   :circle
   (fn [[[cx cy] r]]
     [:circle {:cx cx, :cy cy, :r r}])
   :ellipse
   (fn [[[cx cy] rx ry]]
     [:ellipse {:cx cx, :cy cy, :rx rx, :ry ry}])
   :rect
   (fn [[[x y] [w h] rounded]]
     (if-not rounded
       [:rect {:x x, :y y, :width w, :height h}]
       (if (vector? rounded)
         [:rect {:x x, :y y, :width w, :height h, :rx (first rounded) :ry (second rounded)}]
         [:rect {:x x, :y y, :width w, :height h, :rx rounded :ry rounded}])))
   :polyline
   (fn [points]
     (let [points (unwrap-seq points)]
       [:polyline {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}]))
   :polygon
   (fn [points]
     (let [points (unwrap-seq points)]
       [:polygon {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}]))
   :path
   (fn [spec]
     [:path {:d (convert-path-spec spec)}])})

(def transform-attr-mapping
  {:matrix "matrix"
   :translate "translate"
   :scale "scale"
   :rotate "rotate"
   :skew-x "skewX"
   :skey-y "skewY"})

(defn- process-transform-attr
  "Converts things like [[:translate [10 20]] [:scale [20]]] to
  \"translate(10 20) scale(20)\" to be used in the transform
  attribute."
  [tr]
  (if (string? tr) tr
      (string/join
       " " (map (fn [[transform params]]
                  (str (transform-attr-mapping transform)
                       "(" (string/join " " params) ")")) tr))))

(defn- process-attr-value [k v]
  (cond
   (and (= k :stroke-dasharray) (sequential? v) (every? number? v))
     (string/join "," v)
   (= k :transform)
     (process-transform-attr v)
   (and (sequential? v) (every? number? v))
     (string/join " " v)
   :else
     v))

(defn- process-nested-attr-map [parent-key m]
  (let [make-key (fn [k]
                   (if (= k :paint) ;;to produce :stroke or :paint keys
                    parent-key 
                    (keyword (str (name parent-key) "-" (name k)))))]
    (reduce-kv
     (fn [m k v] (assoc m (make-key k) v))
     {} m)))

(defn add-attrs [element]
  (if (map? (second element))
    element
    (into [(first element) {}] (rest element))))

(defn transform-attrs [element fun]
  (let [element (add-attrs element)
        attrs (second element)]
    (assoc element 1 (fun attrs))))

(defn assoc-attr [element k v]
  (transform-attrs element #(assoc % k v)))

(defn add-transform [element transform]
  (update-in (add-attrs element)
             [1 :transform]
             (fn [x]
               (let [x (or x [])]
                 (conj x transform)))))

(defn- process-attr-map
  "Unwraps nested attribute maps (mainly for :stroke and :fill).

  Renames some dashed attibutes into camelcase to follow the SVG
  convention. Does the lookup in the attr-key-lookup map.

  Converts attribute values that are sequences of numbers into
  space-delimited string, except when it comes to :stroke-dasharray
  which becomes a comma-delimited string."
  [m]
  (->> m
       (reduce-kv
        (fn [m k v]
          (if (map? v)
            (merge m (process-nested-attr-map k v))
            (assoc m k v))) {})
       (reduce-kv
        (fn [m k v]
          (let [k (keyword k)]
            (assoc m
              (or (attr-key-lookup k) k)
              (process-attr-value k v)))) {})))

(defn normalize-element
  "Makes all the elements look like [tag {...} content], even if the
  attrs were skipped or the content was nil."
  [[tag sec & r]]
  (let [attrs (if (map? sec) sec {})
        content (if (seq? (first r)) (first r) r)]
    [tag attrs content]))

(defn dali->hiccup [element]
  (let [[type sec & r] element
        style-map (when (map? sec) sec)
        params (if style-map r (rest element))
        convert-fn (or (convertors type)
                       (fn [content] ;;generic containment tag
                         (concat [type {}] content)))]
    (let [[tag attr & content] (convert-fn params)
          merged-map (process-attr-map (merge attr style-map))
          content (unwrap-seq content)]
      (cond
       (and content (string? (first content)))
       [tag merged-map (first content)]
       content
       (into [] (concat [tag merged-map] (map dali->hiccup content)))
       :else
       [tag merged-map]))))

(comment
  (spit-svg
   (dali->hiccup
    [:page
     {:height 500 :width 500, :stroke {:paint :black :width 2} :fill :none}

     [:rect {:stroke :blue} [110.0 10.0] [170.0 140.0]] ;;geometry bounds
     [:rect {:stroke :red} [100.80886 17.5] [188.38226 125.0]] ;;sensitive bounds
     [:path {:id :thick :stroke-width 20} :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]]
     [:path {:stroke :white} :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]] ;;sensitive bounds

     [:path :M [45 10] :l [10 10] :l [-10 10] :l [-10 -10] :z]
     [:line [10 20] [100 100]]
     [:line [10 100] [100 20]]
     [:polyline
      (map #(vector %1 %2) (range 10 150 10) (cycle [110 120]))]
     [:rect {:id :rounded} [10 130] [100 60] 15]
     [:g {:stroke {:paint :green :width 4} :fill :white}
      (map
       #(vector :circle [% 160] 15)
       (range 35 85 15))]
     [:rect {:stroke-dasharray [5 10 5]} [10 200] [100 60] 15]

     [:rect {:stroke :green} [10.28125 271.26172] [357.16016 11.265625]] ;;bounds of the text
     [:text {:id :the-text :x 10 :y 280 :stroke :none :fill :black}
       "The only difference between me and a madman is that I'm not mad."]])
   "/tmp/svg.svg")
  )

(comment
  (spit-svg
   (dali->hiccup
    [:page {:width 500 :height 500}
     [:defs
      [:g {:id :logo :stroke {:paint :green :width 4} :fill :white}
       (map
        #(vector :circle [% 0] 15)
        (range 0 55 15))]
      [:circle {:id :cc} [0 0] 15]]
     [:use :cc [50 50]]
     [:use {:xlink:href "#cc" :x 20 :y 30}]
     [:use :logo [20 100]]
     [:use :logo [20 150]]])
   "/tmp/svg4.svg")
  )

(comment
  (require '[dali.io :as io])
  (spit-svg
   (dali->hiccup
    [:page {:width 500 :height 500}
     [:defs
      (-> "resources/symbol.svg" io/load-enlive-svg io/extract-svg-content :content first io/enlive->hiccup)]
     [:use :symbol [50 50]]
     [:use :symbol [150 70]]])
   "/tmp/svg5.svg")
  )
