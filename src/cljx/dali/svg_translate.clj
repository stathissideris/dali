(ns dali.svg-translate
  (:require [clojure.string :as string]
            [clojure.pprint :refer [cl-format]]
            [hiccup.core :as hiccup]
            [hiccup.page]
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
             (cl-format true "~d ~d, ~d ~d, ~d ~d" x1 y1, x2 y2, x y))
        :s (let [[[x2 y2] [x y]] v]
             (cl-format true "~d ~d, ~d ~d" x2 y2, x y))
        :q (let [[[x1 y1] [x y]] v]
             (cl-format true "~d ~d, ~d ~d" x1 y1, x y))
        :t (let [[[x y]] v]
             (cl-format true "~d ~d" x y))
        :a (let [[[rx ry] x-axis-rotation large-arc-flag sweep-flag [x y]] v]
             (cl-format true "~d ~d, ~d, ~d, ~d, ~d ~d"
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

(def
  convertors
  {:page
   (fn [content]
     (concat [:svg {:xmlns "http://www.w3.org/2000/svg"}] content))
   :line
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
   :polygon
   (fn [points]
     [:polygon {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}])
   :path
   (fn [spec]
     [:path {:d (convert-path-spec spec)}])})

(defn to-svg [element]
  (let [[type sec & r] element
        style-map (when (map? sec) sec)
        params (if style-map r (rest element))
        convert-fn (convertors type)
        [tag attr & [content]] (convert-fn params)]
    (if content
      [tag (merge style-map attr) content]
      [tag (merge style-map attr)])))

(def svg-doctype "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">\n")

(defn spit-xml [document filename]
  (spit
   filename
   (str
    (hiccup.page/xml-declaration "UTF-8")
    svg-doctype
    (hiccup/html (to-svg document)))))

(comment
  (spit-xml
   [:page
    {:height 500 :width 500}
    [:line {:y2 200, :x2 100, :y1 20, :x1 10, :stroke "black", :stroke-width 2}]]
   "s:/temp/svg.svg"))

(comment
  (import [org.apache.batik.transcoder.image PNGTranscoder]
          [org.apache.batik.transcoder
           TranscoderInput
           TranscoderOutput])
  (with-open [out-stream (FileOutputStream. "out.jpg")
              out (TranscoderOutput. out-stream)
              in (TranscoderInput. ...)]
   (doto (PNGTranscoder.)
     (.transcode in out))))
