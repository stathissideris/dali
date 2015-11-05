(ns dali.dom
  (:require [dali.syntax :as s]))

(def SVG-NS "http://www.w3.org/2000/svg")

(defn element [dom name]
  (.createElementNS dom SVG-NS name))

(defn set-attr! [element k v]
  (.setAttributeNS element nil (name k) (str v)))

(defn hiccup->element [dom hiccup]
  (let [{:keys [tag attrs content]} (s/dali->xml hiccup)
        e (element dom (name tag))]
    (if-not attrs
      e
      (do
        (doseq [[k v] attrs]
          (set-attr! e k v))
        (when content
          (if (string? (first content))
            (.appendChild e (.createTextNode dom (first content)))
            (doseq [child content]
              (.appendChild e (hiccup->element dom child))))) ;;TODO mind the stack
        e))))

(defn first-by-tag [dom tag]
  (-> dom (.getElementsByTagName tag) (.item 0)))

(defn add-to-svg [dom element]
  (let [svg (first-by-tag dom "svg")]
    (when svg
      (.appendChild svg element))))

(defn remove-from-svg [dom element]
  (let [svg (first-by-tag dom "svg")]
    (.removeChild svg element)))

(defn children [element]
  (let [c (.getChildNodes element)]
    (map #(.item c %) (range (.getLength c)))))

(defn ->hiccup [element]
  (let [attrs (.getAttributes element)]
    [(keyword (.getLocalName element))
     (into {}
           (map (fn [i] (let [attr (.item attrs i)]
                          [(keyword (.getName attr))
                           (.getNodeValue attr)]))
                (range (.getLength attrs))))]))
