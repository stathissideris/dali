(ns dev
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.test :as t]
            [dali.examples :as examples]))

(defn load-print []
  (require '[print :refer [pprint-analyze]]))

(defn run-tests []
  (t/run-tests
   'dali.geom-test
   'dali.syntax-test
   'dali.layout-test
   'dali.utils-test
   'dali.regression-test
   'dali.schema-test
;;   'dali.batik-test
   ))

(defn render-examples []
  (examples/render-examples "examples/output/" examples/examples))

(defn render-examples-png []
  (examples/render-examples-png "examples/output-png/" examples/examples))

(defn render-fixtures []
  (examples/render-examples "examples/fixtures/" examples/examples))

(defn render-example [filename]
  (let [document (->> examples/examples
                      (filter #(-> % :filename (= filename)))
                      first
                      :document)]
    (when (nil? document) (throw (ex-info (format "Document %s not found" filename) {})))
    (examples/render-example "examples/output/" filename document)))


(defmacro hiccup->xml [node]
  (let [tag (first node)
        attrs (if (map? (second node)) (second node) nil)
        content (if (map? (second node))
                  (drop 2 node)
                  (rest node))]
    (merge {:tag tag}
           (when-not (empty? attrs) {:attrs attrs})
           (when-not (empty? content) {:content content}))))

(defmacro equals-assertion [x]
  `(is (= ~(eval x) ~x)))

(defn generate-attr-lookup-map
  "Loads all the attribute names from the SVG documentation, extracts
  the ones that are camelcase and produces the mapping of the
  equivalents with dashes. It saves the resulting map the the
  resources folder as EDN. This map is then used in the
  dali.svg-translate namespace to translate the dashed attribute names
  to the camelcased SVG equivalents. All in the name of consistency."
  []
  (let [trim-name #(subs % 1 (dec (.length %)))
        camelcase? #(some (fn [letter] (Character/isUpperCase letter)) %)
        de-camelcase (fn [str] (string/join "-" (map string/lower-case (string/split str #"(?=[A-Z])"))))
        page (html/html-resource (java.net.URL. "http://www.w3.org/TR/SVG11/attindex.html"))]
    (spit
     "resources/attr-key-lookup.edn"
     (str
      ";; generated during development from http://www.w3.org/TR/SVG11/attindex.html\n"
      ";; see dev/user.clj, function generate-attr-lookup-map\n"
      (with-out-str
        (clojure.pprint/pprint
         (->> (html/select page [:span.attr-name])
              (map (comp trim-name first :content))
              (filter camelcase?)
              (into #{})
              (map (fn [k] [(keyword (de-camelcase k)) (keyword k)]))
              (into (sorted-map)))))))))
