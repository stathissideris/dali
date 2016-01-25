(ns dali.io
  (:refer-clojure :exclude [namespace])
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [clojure.data.codec.base64 :as b64]
            [dali.batik :as batik]
            [dali
             [layout :as layout]
             [syntax :as syntax]]
            [net.cgrand.enlive-html :as en])
  (:import [javax.imageio ImageIO]))

(defn- slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [x]
  (with-open [out (java.io.ByteArrayOutputStream.)]
    (io/copy (io/input-stream x) out)
    (.toByteArray out)))

(defn data-uri [bytes]
  (->> bytes
       b64/encode
       (new String)
       (str "data:image/png;base64,")))

(defn slurp-data-uri [filename]
  (->> filename
       io/file
       slurp-bytes
       data-uri))

(defn raster-image-attr [filename]
  (let [image (ImageIO/read (io/file filename))]
    {:width      (.getWidth image)
     :height     (.getHeight image)
     :xlink:href (slurp-data-uri filename)}))

(defn load-enlive-svg [filename]
  (en/xml-resource (io/file filename)))

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
  (str "<?xml version=\"1.0\" encoding=\"" encoding "\" standalone=\"no\"?>\n"))

(defn xml->xml-string
  "Converts clojure.xml representation to an XML string."
  [xml]
  (apply str (en/emit* xml)))

(defn xml->svg-document-string
  "Converts clojure.xml representation to an SVG document string,
  complete with doctype and XML declaration."
  [xml]
  (str
   (xml-declaration "UTF-8")
   ;;svg-doctype
   (xml->xml-string xml)))

(defn spit-svg [xml filename]
  (spit
   filename
   (xml->svg-document-string xml)))

(defn render-svg [doc filename]
  (-> doc
      syntax/dali->ixml
      layout/resolve-layout
      syntax/ixml->xml
      (spit-svg filename)))

(defn render-png
  ([doc filename]
   (render-png doc filename {}))
  ([doc filename options]
   (-> doc
       syntax/dali->ixml
       layout/resolve-layout
       syntax/ixml->xml
       xml->svg-document-string
       batik/parse-svg-string
       (batik/render-document-to-png filename options))))

#_(-> "resources/symbol.svg" load-enlive-svg extract-svg-content :content enlive->hiccup pprint)
