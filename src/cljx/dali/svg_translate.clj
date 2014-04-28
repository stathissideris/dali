(ns dali.svg-translate
  (:require [clojure.string :as string]
            [clojure.pprint :refer [cl-format]]
            [dali.core :as core]))

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

(defn split-params-by-keyword [params]
  (->> params
       (partition-by keyword?)
       (partition-all 2)
       (map (fn [[k v]] [(first k) v]))))

(defn- convert-path-command [[command v]]
  (let [bool (fn [x] (if x 1 0))
        c (or (map-path-command command) command)]
    (condp = (-> c name string/lower-case keyword)
      :m (let [[[x y]] v] (str (name c) " " x " " y))
      :l (let [[[x y]] v] (str (name c) " " x " " y))
      :h (let [[h] v] (str (name c) " " h))
      :v (let [[dv] v] (str (name c) " " dv))
      :c (let [[[x1 y1] [x2 y2] [x y]] v]
           (cl-format true "~a ~d ~d, ~d ~d, ~d ~d"
                      (name c) x1 y1, x2 y2, x y))
      :s (let [[[x2 y2] [x y]] v]
           (cl-format true "~a ~d ~d, ~d ~d"
                      (name c) x2 y2, x y))
      :q (let [[[x1 y1] [x y]] v]
           (cl-format true "~a ~d ~d, ~d ~d"
                      (name c) x1 y1, x y))
      :t (let [[[x y]] v]
           (cl-format true "~a ~d ~d"
                      (name c) x y))
      :a (let [[[rx ry] x-axis-rotation large-arc-flag sweep-flag [x y]] v]
           (cl-format true "~a ~d ~d, ~d, ~d, ~d, ~d ~d"
                      (name c)
                      rx ry
                      x-axis-rotation
                      (bool large-arc-flag)
                      (bool sweep-flag)
                      x y))
      :z "Z")))

(defn- convert-path-spec [spec]
  (let [params (split-params-by-keyword spec)]
    (string/join
     " " (map convert-path-command params))))

(def
  convertors
  {:line
   (fn [[[x1 y1] [x2 y2]]]
     [:line {:x1 x1, :y1 y1, :x2 x2, :y2 y2}])
   :circle
   (fn [[[cx cy] r]]
     [:circle {:cx cx, :cy cy, :r r}])
   :ellipse
   (fn [[[cx cy] rx ry]]
     [:ellipse {:cx cx, :cy cy, :rx rx, :ry ry}])
   :rectangle
   (fn [[[x y] [w h] rounded]]
     (if-not rounded
       [:rect {:x x, :y y, :width w, :height h}]
       (if (vector? rounded)
         [:rect {:x x, :y y, :width w, :height h, :rx (first rounded) :ry (second rounded)}]
         [:rect {:x x, :y y, :width w, :height h, :rx rounded :ry rounded}])))
   :polyline
   (fn [points]
     [:polyline {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}])
   :path
   (fn [spec]
     [:path {:d (convert-path-spec spec)}])})

(defn to-svg [shape]
  (let [[type sec & r] shape
        style-map (when (map? sec) sec)
        params (if style-map r (rest shape))
        convert-fn (convertors type)
        [tag attr & [content]] (convert-fn params)]
    (if content
      [tag (merge style-map attr) content]
      [tag (merge style-map attr)])))
