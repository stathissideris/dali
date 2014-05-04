(ns dali.svg-translate
  (:require [clojure.string :as string]
            [clojure.pprint :refer [cl-format]]
            [hiccup.core :as hiccup]
            [hiccup.page]
            [dali.core :as core]))

(def attr-key-lookup
  (read-string (slurp "resources/attr-key-lookup.edn")))

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
     (concat [:svg {:xmlns "http://www.w3.org/2000/svg" :version "1.2"}] content))
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

(defn- process-attr-value [k v]
  (cond
   (and (= k :stroke-dasharray) (sequential? v) (every? number? v))
     (string/join "," v)
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

(defn to-hiccup [element]
  (let [[type sec & r] element
        style-map (when (map? sec) sec)
        params (if style-map r (rest element))
        convert-fn (or (convertors type)
                       (fn [content] ;;generic containment tag
                         (concat [type {}] content)))]
    (let [[tag attr & content] (convert-fn params)
          merged-map (process-attr-map (merge style-map attr))
          content (unwrap-seq content)]
      (cond
       (and content (string? (first content)))
       [tag merged-map (first content)]
       content
       (into [] (concat [tag merged-map] (map to-hiccup content)))
       :else
       [tag merged-map]))))

(def svg-doctype "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.2//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n")

(defn spit-svg [document filename]
  (spit
   filename
   (str
    (hiccup.page/xml-declaration "UTF-8")
    svg-doctype
    (hiccup/html document))))

(comment
  (spit-svg
   (to-hiccup
    [:page
     {:height 500 :width 500, :stroke {:paint :black :width 2} :fill :none}

     [:rect {:stroke :blue} [110.0 10.0] [170.0 140.0]] ;;geometry bounds
     [:rect {:stroke :red} [100.80886 17.5] [188.38226 125.0]] ;;sensitive bounds
     [:path {:id "thick" :stroke-width 20} :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]]
     [:path {:stroke :white} :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]] ;;sensitive bounds

     [:path :M [45 10] :l [10 10] :l [-10 10] :l [-10 -10] :z]
     [:line [10 20] [100 100]]
     [:line [10 100] [100 20]]
     [:polyline
      (map #(vector %1 %2) (range 10 150 10) (cycle [110 120]))]
     [:rect [10 130] [100 60] 15]
     [:g {:stroke {:paint :green :width 4} :fill :white}
      (map
       #(vector :circle [% 160] 15)
       (range 35 85 15))]
     [:rect {:stroke-dasharray [5 10 5]} [10 200] [100 60] 15]

     #_[:flowRoot
      [:flowRegion
       [:rect [10 280] [150 200]]]
      [:flowPara
       "Jackson was born in Washington, D.C., and grew up as an only child in Chattanooga, Tennessee.[2] His father lived away from the family, in Kansas City, Missouri, and later died from alcoholism. Jackson had only met his father twice during his life.[3][4] Jackson was raised by his mother, Elizabeth Jackson (née Montgomery), who was a factory worker and later a supplies buyer for a mental institution, and by his maternal grandparents and extended family.[3][5] According to DNA tests, Jackson partially descends from the Benga people of Gabon.[6]
Jackson attended several segregated schools[7] and graduated from Riverside High School in Chattanooga. Between the third and twelfth grades, he played the French horn and trumpet in the school orchestra.[8] During childhood, he had a stuttering problem, which he conquered by developing an affinity for the use of the curse word, motherfucker, in his vocabulary.[9]
Initially intent on pursuing a degree in marine biology, he attended Morehouse College in Atlanta, Georgia. After joining a local acting group to earn extra points in a class, Jackson found an interest in acting and switched his major.[10] Before graduating in 1972, he co-founded the Just Us Theatre.[3][11]"]]

     [:text {:x 10 :y 280 :stroke :none :fill :black}
       "The only difference between me and a madman is that I'm not mad."]
     
     [:switch
      [:foreignObject {:x 10 :y 280 :width 50 :height 200}
       [:p {:xmlns "http://www.w3.org/1999/xhtml"} "This is a test with HTML p."]]
      [:text {:x 10 :y 280 :stroke :none :fill :black}
       "The only difference between me and a madman is that I'm not mad."]]])
   "s:/temp/svg.svg")
  )

(comment
  (spit-svg
   (to-hiccup
    [:page
     {:height 500 :width 500, :stroke {:paint :black :width 2} :fill :none}
     [:path :M [110 80] :C [140 10] [165 10] [195 80] :S [250 150] [280 80]]])
   "s:/temp/svg2.svg")
  )

(comment
  (spit-svg
   (to-hiccup
    [:page
     [:defs
      [:marker {:id :triangle :view-box [0 0 10 10]
                :ref-x 1 :ref-y 5
                :marker-width 6 :marker-height 6
                :orient :auto}
       [:path :M [0 0] :L [10 5] :L [0 10] :Z]]]
     [:polyline
      {:fill :none :stroke-width 2 :stroke :black :marker-end "url(#triangle)"}
      [10 90] [50 80] [90 20]]])
   "s:/temp/svg3.svg")
  )

