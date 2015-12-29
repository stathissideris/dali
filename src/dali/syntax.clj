(ns dali.syntax
  (:require [clojure.java.io :as java-io]
            [clojure
             [pprint :refer [cl-format]]
             [string :as string]
             [zip :as zip]]
            [dali
             [geom :as geom]
             [utils :as utils]]
            [dali.math :as math]
            [net.cgrand.enlive-html :as en]))

(defn dali-content [node]
  (some-> node :attrs :dali/content))

(def attr-key-lookup
  (-> "attr-key-lookup.edn" java-io/resource slurp read-string))

(defn- update-in-content [e fun]
  (update-in e [:attrs :dali/content] fun))

(defmulti set-last-point (fn [e _] (:tag e)))
(defmethod set-last-point :line
  [e p]
  (update-in-content e #(assoc % 1 p)))
(defmethod set-last-point :polyline
  [e p]
  (update-in-content e #(assoc % (dec (count %)) p)))
;;TODO for generic path

(defmulti set-first-point (fn [e _] (:tag e)))
(defmethod set-first-point :line
  [e p]
  (update-in-content e #(assoc % 0 p)))
(defmethod set-first-point :polyline
  [e p]
  (update-in-content e #(assoc % 0 p)))
;;TODO for generic path

(defmulti last-point-angle (fn [e] (:tag e)))
(defmethod last-point-angle :line
  [e]
  (->> e :attrs :dali/content (apply geom/angle)))
(defmethod last-point-angle :polyline
  [e]
  (->> e :attrs :dali/content (take-last 2) (apply geom/angle)))
;;TODO for generic path

(defmulti first-point-angle (fn [e] (:tag e)))
(defmethod first-point-angle :line
  [e]
  (->> e :attrs :dali/content reverse (apply geom/angle)))
(defmethod first-point-angle :polyline
  [e]
  (->> e :attrs :dali/content (take 2) reverse (apply geom/angle)))
;;TODO for generic path

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

(defn- has-dali-marker? [{:keys [attrs]}]
  (or (:dali/marker-end attrs)
      (:dali/marker-start attrs)))

(defn- add-dali-marker [node document {:keys [location marker]}]
  (let [marker-id      (if (keyword? marker) marker (:id marker))
        marker-attrs   (if (keyword? marker) {} (dissoc marker :id :xlink:href))
        marker-node    (first (en/select document [(utils/to-enlive-id-selector marker-id)]))
        _              (when-not marker-node
                         (throw (ex-info "Marker node not found"
                                         {:node     node
                                          :document document
                                          :location location
                                          :marker   marker})))
        tip            (some-> marker-node :attrs :dali/marker-tip)
        _              (utils/assert-req tip)
        height         (first tip)
        base-point     (if (= location :end)
                         (-> node :attrs :dali/content last)
                         (-> node :attrs :dali/content first))
        a              (if (= location :end)
                         (last-point-angle node)
                         (first-point-angle node))
        new-base-point (geom/v- base-point (math/polar->cartesian [height a]))
        conj-or-concat (fn [a b] (if (sequential? b) (vec (concat a b)) (conj a b)))]
    ;;(utils/prn-names tip base height base-point a new-base-point)
    [(as-> node x
       (update x :attrs dissoc :dali/marker-end :dali/marker-start) ;;important, otherwise infinite recursion
       (if (= location :end)
         (set-last-point x new-base-point)
         (set-first-point x new-base-point)))
     {:tag :use
      :attrs (merge-with
              conj-or-concat
              {:xlink:href (utils/to-iri-id marker-id)
               :class     [:marker (utils/keyword-concat :marker "-" location)]
               :transform [[:translate new-base-point]
                           [:rotate [a]]]}
              marker-attrs)}]))

(defn- add-dali-markers [{:keys [attrs] :as original-node} document]
  (let [end? (some? (:dali/marker-end attrs))
        start? (some? (:dali/marker-start attrs))
        
        [node marker-end]
        (if-not end?
          [original-node nil]
          (add-dali-marker original-node document
                           {:location :end
                            :marker (:dali/marker-end attrs)}))
        [node marker-start]
        (if-not start?
          [node nil]
          (add-dali-marker node document
                           {:location :start
                            :marker (:dali/marker-start attrs)}))]
    (if (or end? start?)
      (merge
       {:tag :g
        :content
        (vec (remove nil? [node marker-start marker-end]))}
       (if-let [a (:dali/marker-group-attrs attrs)]
         {:attrs a}))
      original-node)))

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
   (and (sequential? v) (every? (some-fn keyword? string?) v))
     (string/join " " (map name v))
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
       (if (= "dali" (utils/keyword-ns k))
         m
         (assoc m k v))) {} m)
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

(defn- calc-attrs
  ([user-attrs]
   (process-attr-map user-attrs))
  ([dali-args user-attrs]
   (merge dali-args (process-attr-map user-attrs))))

(defmulti ixml-tag->xml :tag)

(defmethod ixml-tag->xml :page
  [{:keys [attrs content]} _]
  {:tag :svg
   :attrs
   (calc-attrs
    {:xmlns "http://www.w3.org/2000/svg"
     :version "1.2"
     :xmlns:xlink "http://www.w3.org/1999/xlink"}
    attrs)
   :content content})

(defmethod ixml-tag->xml :use
  [{:keys [attrs] :as node} _]
  (let [[ref [x y]] (dali-content node)]
    (if (and ref x y)
      {:tag :use :attrs (calc-attrs {:xlink:href (str "#" (name ref)) :x x :y y} attrs)}
      {:tag :use :attrs (calc-attrs attrs)})))

(defmethod ixml-tag->xml :line
  [{:keys [attrs] :as node} document]
  (let [[[x1 y1] [x2 y2]] (dali-content node)]
    (as-> node x
      (add-dali-markers x document)
      (if (= :g (:tag x))
        (update x :attrs calc-attrs) ;;it's been nested, leave it as it is so that it's processed deeper by the zipper
        {:tag :line :attrs (calc-attrs {:x1 x1 :y1 y1 :x2 x2 :y2 y2} attrs)}))))

(defmethod ixml-tag->xml :circle
  [{:keys [attrs] :as node} _]
  (let [[[cx cy] r] (dali-content node)]
    {:tag :circle :attrs (calc-attrs {:cx cx :cy cy :r r} attrs)}))

(defmethod ixml-tag->xml :ellipse
  [{:keys [attrs] :as node} _]
  (let [[[cx cy] rx ry] (dali-content node)]
    {:tag :ellipse :attrs (calc-attrs {:cx cx :cy cy :rx rx :ry ry} attrs)}))

(defmethod ixml-tag->xml :rect
  [{:keys [attrs] :as node} _]
  (let [[[x y] [w h] rounded] (dali-content node)]
    (if-not rounded
      {:tag :rect :attrs (calc-attrs {:x x, :y y, :width w, :height h} attrs)}
      (if (vector? rounded)
        {:tag :rect
         :attrs (calc-attrs {:x x :y y :width w :height h :rx (first rounded) :ry (second rounded)} attrs)}
        {:tag :rect
         :attrs (calc-attrs {:x x :y y :width w :height h :rx rounded :ry rounded} attrs)}))))

;;the following implements a behaviour that also exists in hiccup: if
;;an element of the content is a seq, it is unwrapped and spliced to
;;the main content in order to allow the use of map, filter etc more
;;convenient. See tests for group and polygon for an examples.
(defn- flatten-1 [coll]
  (mapcat (fn [x] (if (seq? x) x [x])) coll))

(defmethod ixml-tag->xml :polyline
  [{:keys [attrs] :as node} document]
  (as-> node x
    (add-dali-markers x document)
    (if (= :g (:tag x))
      (update x :attrs calc-attrs) ;;it's been nested, leave it as it is so that it's processed deeper by the zipper
      (assoc x :attrs (calc-attrs {:points (string/join
                                            " "
                                            (map (fn [[x y]] (str x "," y))
                                                 (-> x dali-content flatten-1)))} attrs)))))

(defmethod ixml-tag->xml :polygon
  [{:keys [attrs] :as node} _]
  (let [points (-> node dali-content flatten-1)]
    {:tag :polygon
     :attrs (calc-attrs {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))} attrs)}))

(defmethod ixml-tag->xml :path
  [{:keys [attrs] :as node} _]
  {:tag :path :attrs (calc-attrs {:d (convert-path-spec (dali-content node))} attrs)})

(defmethod ixml-tag->xml :default
  [{:keys [attrs] :as node} _]
  (update node :attrs calc-attrs))



(defn add-transform [node transform]
  (if-not transform
    node
    (update-in node [:attrs :transform] conj transform)))

(defn add-class [node cl]
  (update-in node [:attrs :class] conj cl))

(defn- dali-tag? [node]
  (and (vector? node) (keyword? (first node))))

(defn- attrs->ixml [attrs]
  (if (and attrs (:transform attrs) (not (string? (:transform attrs))))
    (update attrs :transform (partial partition 2))
    attrs))

(defn- replace-empty-coords [document]
  (utils/transform-zipper
   (utils/generic-zipper document)
   #(let [node (zip/node %)] (if (= :_ node) [0 0] node))))

(def dali-content-coords-tags
  #{:use :line :circle :ellipse :rect :polyline :polygon :path})



(defmulti process-dali-tag first)

(defmethod process-dali-tag :dali/ghost
  [x] (concat [:rect {:class :dali-ghost}]
              (if (map? (second x)) ;;has attributes?
                (drop 2 x)
                (rest x))))

(defmethod process-dali-tag :default [x] x)



(defn dali-node->ixml-node ;;TODO find out why this gets called with nodes that are already XML
  [node]
  (if-not (dali-tag? node)
    node
    (let [[tag & r]     (process-dali-tag node)
          attrs         (attrs->ixml (when (map? (first r)) (first r)))
          r             (if (map? (first r)) (rest r) r)
          content       (not-empty (remove nil? (flatten-1 r)))
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
      (cond-> xml-node
        (= :path tag)
          (update-in [:attrs :dali/content] (comp vec split-params-by-keyword))
        (dali-content-coords-tags tag)
          (update-in [:attrs :dali/content] replace-empty-coords)))))

(defn dali->ixml [document]
  (utils/transform-zipper (utils/ixml-zipper document)
                          (comp dali-node->ixml-node zip/node)))

(defn ixml-node->xml-node
  [document {:keys [tag attrs content] :as node}]
  (if (string? node)
    node
    (let [{:keys [tag attrs content]} (ixml-tag->xml node document)
          content                     (remove nil? (flatten-1 content))]
      (merge
       {:tag tag}
       (when-not (empty? attrs) {:attrs attrs})
       (when-not (empty? content) {:content content})))))

(defn ixml-fragment->xml-node
  "Convert an IXML fragment to an clojure.xml, using the context of
  the passed document to resolve references for markers etc."
  [document fragment]
  (utils/transform-zipper
   (utils/ixml-zipper fragment)
   (comp (partial ixml-node->xml-node document) zip/node)))

(defn ixml->xml [document]
  (when-not (= :page (:tag document))
    (throw (ex-info "ixml->xml is not applicable to dali fragments, use on whole documents only"
                    {:fragment document})))
  (utils/transform-zipper
   (utils/ixml-zipper document)
   (comp (partial ixml-node->xml-node document) zip/node)))

(defn dali->xml [document]
  (let [document (replace-empty-coords document)]
    (utils/transform-zipper (utils/ixml-zipper document)
                            (comp (partial ixml-node->xml-node document)
                                  dali-node->ixml-node
                                  zip/node))))

(defn css [css-string]
  [:style {:type "text/css"} (str "<![CDATA[" css-string "]]>")])

(defn javascript [javascript-string]
  [:script {:type "text/ecmascript"} (str "<![CDATA[" javascript-string "]]>")])

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
