(ns dali.io
  (:refer-clojure :exclude [namespace])
  (:require [clojure.java.io :as io]
            [net.cgrand.xml :as xml]
            [net.cgrand.enlive-html :as en]
            [clojure.walk :as walk]
            [clojure.string :as string]
            [hiccup.core :as hiccup]
            [hiccup.page]))

(defn load-enlive-svg [filename]
  (xml/parse (io/file filename)))

(defn namespace [tag-name]
  (if-let [r (->> tag-name name (re-find #"^(.+?)\:") second)]
    (keyword r)
    :dali/default-namespace))

(defn tag-namespace=
  "Custom enlive selector to select tags with particular
  namespaces. Pass one namespace as a keyword or a set to select
  several namespaces."
  [ns]
  (if (set? namespace)
    (en/pred #(ns (namespace (:tag %))))
   (en/pred #(= ns (namespace (:tag %))))))

(defn tag-namespace-not=
  "Custom enlive selector to select tags *without* particular
  namespaces. Pass one namespace as a keyword or a set to select
  several namespaces."
  [ns]
  (if (set? ns)
    (en/pred #(not (ns (namespace (:tag %)))))
   (en/pred #(not= ns (namespace (:tag %))))))

(defn attr-ns-remover [nss]
  #(assoc % :attrs
          (into {} (filter (fn [[attr _]] (nss (namespace attr))) (:attrs %)))))

(defn extract-svg-content
  "Extract \"useful\" SVG content from an enlive document for
  inclusion to another SVG document. Returns a map with :content and
  :defs (both can be empty). Only tags and attrs with the namespace
  svg or default namespace are included."
  [document & {:keys [namespaces]}]
  (let [namespaces (or namespaces #{:svg :dali/default-namespace})
        clean (en/transformation
               [:metadata] (en/substitute nil)
               [(tag-namespace-not= namespaces)] (en/substitute nil))
        all-content (->
                     (clean document)
                     (en/select [:svg :> :*])
                     (en/transform [:*] (attr-ns-remover namespaces)))
        defs (:content (first (en/select all-content [:defs])))
        content (en/transform all-content [:defs] (en/substitute nil))]
    {:defs defs
     :content content}))

(defn enlive-tag? [x]
  (and (map? x) (:tag x)))

(defn enlive->hiccup [document]
  (let [ws-string? #(and (string? %) (re-matches #"^\W+$" %))]
   (walk/postwalk
    (fn [x]
      (if (enlive-tag? x)
        (let [{:keys [tag attrs content]} x]
          (vec (concat [tag attrs] (remove ws-string? content))))
        x))
    document)))

(defn- add-attrs [tag attrs]
  (if(or (not attrs) (empty? attrs))
    tag
    (vec (concat [(first tag) attrs] (rest tag)))))

(def svg-doctype "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.2//EN\" \"http://www.w3.org/Graphics/SVG/1.2/DTD/svg12.dtd\">\n")

(defn hiccup->svg-document-string [hiccup]
  (str
   (hiccup.page/xml-declaration "UTF-8")
   svg-doctype
   (hiccup/html hiccup)))

(defn spit-svg [hiccup-string filename]
  (spit
   filename
   (hiccup->svg-document-string hiccup-string)))

#_(def
  hiccup->dali-convertors
  {:use ;;;TODO
   (fn [[ref [x y]]]
     (if (and ref x y)
       [:use {:xlink:href (str "#" (name ref)) :x x :y y}]
       [:use {}]))
   :line
   (fn [[_ {:keys [x1 y1 x2 y2] :as attrs}]]
     (add-attrs [:line [[x1 y1] [x2 y2]]]
                (dissoc attrs :x1 :y1 :x2 :y2)))
   :circle
   (fn [[_ {:keys [cx cy r] :as attrs}]]
     (add-attrs [:circle [cx cy] r]
                (dissoc attrs :cx :cy :r)))
   :ellipse
   (fn [[_ {:keys [cx cy rx ry] :as attrs}]]
     (add-attrs [:ellipse [cx cy] rx ry]
                (dissoc attrs :cx :cy :rx :ry)))
   :rect
   (fn [[_ {:keys [x y w h rx ry] :as attrs}]]
     (let [attrs (dissoc attrs :x :y :w :h :rx :ry)]
      (if (and rx ry)
        (if (= rx ry)
          (add-attrs [:rect [x y] [w h] rx] attrs)
          (add-attrs [:rect [x y] [w h] rx ry] attrs))
        (add-attrs [:rect [x y] [w h]] attrs))))
   :polyline ;;TODO
   (fn [points]
     (let [points (unwrap-seq points)]
       [:polyline {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}]))
   :polygon ;;TODO
   (fn [points]
     (let [points (unwrap-seq points)]
       [:polygon {:points (string/join " " (map (fn [[x y]] (str x "," y)) points))}]))
   :path
   (fn [spec]
     [:path {:d (convert-path-spec spec)}])})

(defn hiccup->dali [document])

#_(-> "resources/symbol.svg" load-enlive-svg extract-svg-content :content enlive->hiccup pprint)
