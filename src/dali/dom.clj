(ns dali.dom
  (:require [dali.syntax :as s]
            [dali.utils :as utils]))

(def SVG-NS "http://www.w3.org/2000/svg")

(defn element [dom name]
  (.createElementNS dom SVG-NS name))

(defn set-attr! [element k v]
  (.setAttributeNS element nil (name k) (str v)))

(defn- dali-attr? [k]
  (= "dali" (utils/keyword-ns k)))

(defn xml->dom-element [dom node]
  (let [{:keys [tag attrs content] :as node} node
        e (element dom (name tag))]
    (do
      (when attrs
        (doseq [[k v] attrs]
          (when-not (dali-attr? k)
            (set-attr! e k v))))
      (when content
        (if (string? (first content))
          (.appendChild e (.createTextNode dom (first content))) ;;TODO only first??
          (doseq [child content]
            (.appendChild e (xml->dom-element dom child))))) ;;TODO mind the stack
      e)))

(defn first-by-tag [dom tag]
  (-> dom (.getElementsByTagName tag) (.item 0)))

(defn add-to-svg! [dom element]
  (let [svg (first-by-tag dom "svg")]
    (when svg
      (.appendChild svg element))))

(defn remove-from-svg [dom element]
  (let [svg (first-by-tag dom "svg")]
    (.removeChild svg element)))

(defn children [element]
  (let [c (.getChildNodes element)]
    (if (zero? (.getLength c))
      nil
      (map #(.item c %) (range (.getLength c))))))

(defn ->xml
  ([element]
   (->xml element false))
  ([element children?]
   (if (instance? org.w3c.dom.Text element)
     (.getWholeText element)
     (let [attrs (.getAttributes element)]
       (merge
        
        {:tag      (keyword (.getLocalName element))}
        (when (and attrs (not (zero? (.getLength attrs))))
          {:attrs    (into {}
                           (map (fn [i] (let [attr (.item attrs i)]
                                          [(keyword (.getName attr))
                                           (.getNodeValue attr)]))
                                (range (.getLength attrs))))})
        (when children?
          (when-let [c (children element)]
            {:content (mapv #(->xml % children?) c)})))))))
