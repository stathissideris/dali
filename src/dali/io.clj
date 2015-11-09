(ns dali.io
  (:refer-clojure :exclude [namespace])
  (:require [clojure.java.io :as io]
            [net.cgrand.xml :as xml]
            [net.cgrand.enlive-html :as en]
            [clojure.walk :as walk]
            [clojure.xml :as cxml]
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
  :defs that contain maps of SVG IDs (as keywords) to SVG elements (in
  clojure.xml format). Only tags and attrs with the namespace svg or
  default namespace are included."
  [document & {:keys [namespaces]}]
  (let [namespaces  (or namespaces #{:svg :dali/default-namespace})
        clean       (en/transformation
                     [:metadata] (en/substitute nil)
                     [(tag-namespace-not= namespaces)] (en/substitute nil))
        all-content (->
                     (clean document)
                     (en/select [:svg :> :*])
                     (en/transform [:*] (attr-ns-remover namespaces)))
        defs        (:content (first (en/select all-content [:defs])))
        content     (en/transform all-content [:defs] (en/substitute nil))
        get-id      #(-> % :attrs :id keyword)
        make-map    (fn [nodes] (->> nodes
                                     (remove (complement map?))
                                     (map #(vector (get-id %) %))
                                     (into {})))]
    {:defs (make-map defs)
     :content (make-map content)}))

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

(def svg-doctype "<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.2//EN\" \"http://www.w3.org/Graphics/SVG/1.2/DTD/svg12.dtd\">\n")

(defn- xml-declaration
  "Create a standard XML declaration for the following encoding."
  [encoding]
  (str "<?xml version=\"1.0\" encoding=\"" encoding "\"?>\n"))

(defn xml->xml-string ;;TODO placeholder implementation, replace with something more performant
  "Converts clojure.xml representation to an XML string."
  [xml]
  (with-out-str
    (cxml/emit-element xml)))

(defn xml->svg-document-string
  "Converts clojure.xml representation to an SVG document string,
  complete with doctype and XML declaration."
  [xml]
  (str
   (xml-declaration "UTF-8")
   svg-doctype
   (xml->xml-string xml)))

(defn spit-svg [xml filename]
  (spit
   filename
   (xml->svg-document-string xml)))

#_(-> "resources/symbol.svg" load-enlive-svg extract-svg-content :content enlive->hiccup pprint)
