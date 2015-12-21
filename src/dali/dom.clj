(ns dali.dom
  (:require [dali :as dali]
            [dali.syntax :as s]
            [dali.utils :as utils]))

(def SVG-NS "http://www.w3.org/2000/svg")

(def ns-mapping {"xlink" "http://www.w3.org/1999/xlink"
                 "svg"   SVG-NS})

(defn- create-element [dom name]
  (if (dali/dali-tag? name)
    (.createElementNS dom SVG-NS "g")
    (.createElementNS dom SVG-NS name)))

(defn- has-namespace? [k]
  (.contains k ":"))

(defn- namespace-name [k]
  (rest (re-find #"(.+)\:(.+)" k)))

(defn set-attr! [element k v]
  (when-not k (throw (ex-info "Cannot set attribute with nil key" {})))
  (when-not v (throw (ex-info "Cannot set attribute with nil value" {})))
  (let [k (name k)]
    (if (has-namespace? k)
      (let [[ns k] (namespace-name k)]
        ;;(println "setting attribute" (pr-str k) "with ns" (pr-str ns) "to" (pr-str (str v)))
        (.setAttributeNS element (ns-mapping ns) k (str v)))
      (do
        ;;(println "setting attribute" (pr-str (name k)) "to" (pr-str (str v)))
        (.setAttributeNS element nil k (str v))
        (when (= k "id")
          (.setIdAttributeNS element nil k true))))))

(defn- dali-attr? [k]
  (= "dali" (utils/keyword-ns k)))

(defn children [element]
  (let [c (.getChildNodes element)]
    (if (zero? (.getLength c))
      nil
      (map #(.item c %) (range (.getLength c))))))

(defn ->xml
  ([element]
   (->xml element true))
  ([element children?]
   (if (instance? org.w3c.dom.Text element)
     (.getWholeText element)
     (let [attrs (.getAttributes element)]
       (merge
        {:tag (keyword (.getLocalName element))
         :ns  (.getNamespaceURI element)}
        (when (and attrs (not (zero? (.getLength attrs))))
          {:attrs (into {}
                        (map (fn [i] (let [attr (.item attrs i)]
                                       [(keyword (.getName attr))
                                        (.getNodeValue attr)]))
                             (range (.getLength attrs))))})
        (when children?
          (when-let [c (children element)]
            {:content (mapv #(->xml % children?) c)})))))))

(defn append-child! [parent child]
  (when-not (.appendChild parent child)
    (throw (ex-info "Could node append DOM child to parent" {:parent (->xml parent)
                                                             :child (->xml child)}))))

(defn- cdata? [s]
  (.startsWith s "<![CDATA["))

(defn- create-cdata-section [dom data]
  (let [data (second (re-find #"<!\[CDATA\[(.+?)\]\]>" data))]
    (.createCDATASection dom data)))

(defn xml->dom-element [dom element]
  (when-not element
    (throw (ex-info "Cannot convert nil xml element to DOM")))
  (let [{:keys [tag attrs content]} element
        _ (when-not tag (throw (ex-info "tag cannot be nil" {:element element})))
        e (create-element dom (utils/keyword-name tag))]
    (do
      (when attrs
        (doseq [[k v] attrs]
          (when-not (dali-attr? k)
              (when-not k (throw (ex-info "Cannot set attribute with nil key" {:element element})))
              (when-not v (throw (ex-info "Cannot set attribute with nil value" {:element element})))
            (set-attr! e k v))))
      (when content
        (if (string? (first content))
          (let [c (first content)]
           (if (cdata? c)
             (append-child! e (create-cdata-section dom c))
             (append-child! e (.createTextNode dom c)))) ;;TODO only first??
          (doseq [child content]
            (append-child! e (xml->dom-element dom child))))) ;;TODO mind the stack
      e)))

(defn first-by-tag [dom tag]
  (-> dom (.getElementsByTagName tag) (.item 0)))

(defn add-to-svg! [dom element]
  (let [svg (first-by-tag dom "svg")]
    (if-not svg
      (throw (ex-info "Could not find SVG element in DOM" {:dom (->xml dom)}))
      (append-child! svg element))))

(defn remove-from-svg! [dom element]
  (let [svg (first-by-tag dom "svg")]
    (.removeChild svg element)))

(defn nth-child [parent index]
  (some-> parent .getChildNodes (.item index)))

(defn replace-child! [parent index new-element]
  (.replaceChild parent new-element (nth-child parent index)))

(defn replace-node! [dom path new-element]
  (try
    (loop [parent dom
           path   path]
      (if (= 1 (count path))
        (let [d (replace-child! parent (first path) new-element)]
          d)
        (do
;;          (println "RECUR")
          (recur (nth-child parent (first path))
                 (rest path)))))
    (catch Exception e
      (throw (ex-info "Could not replace DOM node" {:dom (->xml dom)
                                                    :cause e
                                                    :path path
                                                    :new-element (->xml new-element)})))))

(defn get-node [dom path]
  (loop [parent dom
         path   path]
    (if (= 1 (count path))
      (nth-child parent (first path))
      (recur (nth-child parent (first path))
             (rest path)))))

