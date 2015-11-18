(ns dali.syntax
  (:require [clojure.java.io :as java-io]
            [clojure.pprint :refer [cl-format]]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [dali.utils :as utils]))

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

(defn- convert-path-spec [commands]
  (string/join
   " " (map convert-path-command commands)))

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
   (fn svg-tranform [_ _ content]
     {:tag :svg
      :attrs
      {:xmlns "http://www.w3.org/2000/svg"
       :version "1.2"
       :xmlns:xlink "http://www.w3.org/1999/xlink"}
      :content content})
   :use
   (fn use-tranform [_ [ref [x y]] _]
     (if (and ref x y)
       {:tag :use :attrs {:xlink:href (str "#" (name ref)) :x x :y y}}
       {:tag :use}))
   :line
   (fn line-tranform [_ [[x1 y1] [x2 y2]] _]
     {:tag :line :attrs {:x1 x1 :y1 y1 :x2 x2 :y2 y2}})
   :circle
   (fn circle-tranform [_ [[cx cy] r] _]
     {:tag :circle :attrs {:cx cx :cy cy :r r}})
   :ellipse
   (fn ellipse-tranform [_ [[cx cy] rx ry] _]
     {:tag :ellipse :attrs {:cx cx :cy cy :rx rx :ry ry}})
   :rect
   (fn rect-tranform [_ [[x y] [w h] rounded] _]
     (if-not rounded
       {:tag :rect :attrs {:x x :y y :width w :height h}}
       (if (vector? rounded)
         {:tag :rect
          :attrs {:x x :y y
                  :width w :height h
                  :rx (first rounded) :ry (second rounded)}}
         {:tag :rect
          :attrs {:x x :y y
                  :width w :height h
                  :rx rounded :ry rounded}})))
   :polyline
   (fn polyline-tranform [_ points _]
     (let [points (unwrap-seq points)]
       {:tag :polyline
        :attrs {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}}))
   :polygon
   (fn polygon-transform [_ points _]
     (let [points (unwrap-seq points)]
       {:tag :polygon
        :attrs {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}}))
   :path
   (fn path-transform [_ spec _]
     {:tag :path :attrs {:d (convert-path-spec spec)}})})

(def transform-attr-mapping
  {:matrix "matrix"
   :translate "translate"
   :scale "scale"
   :rotate "rotate"
   :skew-x "skewX"
   :skey-y "skewY"})

(defn- process-transform-attr
  "Converts attributes that look like [:translate [10 20] :scale [20]]
  to \"translate(10 20) scale(20)\" to be used in the transform
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
   (keyword? v)
     (name v)
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

(defn add-transform [node transform]
  (update-in node [:attrs :transform] conj transform))

(defn- process-attr-map
  "Unwraps nested attribute maps (mainly for :stroke and :fill).

  Renames some dashed attibutes into camelcase to follow the SVG
  convention. Does the lookup in the attr-key-lookup map.

  Converts attribute values that are sequences of numbers into
  space-delimited string, except when it comes to :stroke-dasharray
  which becomes a comma-delimited string."
  [m]
  (as-> m m
    (reduce-kv
     (fn [m k v]
       (if (map? v)
         (merge m (process-nested-attr-map k v))
         (assoc m k v))) {} m)
    (reduce-kv
     (fn [m k v]
       (let [k (keyword k)]
         (assoc m
                (or (attr-key-lookup k) k)
                (process-attr-value k v)))) {} m)
    (dissoc m :dali/content)))

(defn dali-tag? [element]
  (and (vector? element) (keyword? (first element))))

(defn normalize-hiccup-node
  "Makes all the elements look like [tag {...} content], even if the
  attrs were skipped or the content was nil. Deprecated, will be
  phased out."
  [[tag sec & r]]
  (let [attrs (if (map? sec) sec {})
        content (if (seq? (first r)) (first r) r)]
    [tag attrs content]))

(defn- attrs->ixml [attrs]
  (if (and attrs (:transform attrs) (not (string? (:transform attrs))))
    (update attrs :transform (partial partition 2))
    attrs))

(defn- flatten-1 [coll]
  (mapcat (fn [x] (if (seq? x) x [x])) coll))

(defn dali-node->ixml-node ;;TODO find out why this gets called with nodes that are already XML
  [node]
  (if-not (dali-tag? node)
    node
    (let [[tag & r] node
          attrs         (attrs->ixml (when (map? (first r)) (first r)))
          r             (if (map? (first r)) (rest r) r)
          content       (not-empty (flatten-1 r))
          content-attr? (and (not-empty content)
                             (not (every? string? content))
                             (every? (complement dali-tag?) content))

          attrs         (if content-attr?
                          (assoc attrs :dali/content (vec content))
                          attrs)
          content       (if content-attr? nil content)

          xml-node      (merge
                         {:tag tag}
                         (when attrs {:attrs attrs})
                         (when content {:content (vec content)}))]
      (if (= :path tag)
        (update-in xml-node [:attrs :dali/content] (comp vec split-params-by-keyword))
        xml-node))))

(defn- replace-empty-coords [document]
  (utils/transform-zipper
   (utils/generic-zipper document)
   #(let [node (zip/node %)] (if (= :_ node) [0 0] node))))

(defn dali->ixml [document]
  (let [document (replace-empty-coords document)]
    (utils/transform-zipper (utils/ixml-zipper document)
                            (comp dali-node->ixml-node zip/node))))

(defn ixml-node->xml-node
  [{:keys [tag attrs content] :as node}]
  (if (string? node)
    node
    (let [original-attrs              attrs
          convert-fn                  (or (convertors tag)
                                          (fn identity-convertor [_ _ _]
                                            {:tag tag :attrs attrs :content content}))
          {:keys [tag attrs content]} (convert-fn tag (:dali/content attrs) content)
          merged-attrs                (process-attr-map (merge attrs original-attrs))
          content                     (unwrap-seq content)]
      (merge
       {:tag tag}
       (when-not (empty? merged-attrs) {:attrs merged-attrs})
       (when-not (empty? content) {:content content})))))

(defn ixml->xml [document]
  (utils/transform-zipper (utils/ixml-zipper document) (comp ixml-node->xml-node zip/node)))

(defn dali->xml [document]
  (let [document (replace-empty-coords document)]
    (utils/transform-zipper (utils/ixml-zipper document) (comp ixml-node->xml-node
                                                               dali-node->ixml-node
                                                               zip/node))))

(defn css [css-string]
  [:style {:type "text/css"} (str "<![CDATA[" css-string "]]>")])

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
      (-> "resources/symbol.svg" io/load-enlive-svg io/extract-svg-content :content :symbol io/enlive->hiccup)]
     [:use :symbol [50 50]]
     [:use :symbol [150 70]]])
   "/tmp/svg5.svg")
  )
