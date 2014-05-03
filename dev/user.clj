(ns user)

(defn generate-attr-lookup-map
  "Loads all the attribute names from the SVG documentation, extracts
  the ones that are camelcase and produces the mapping of the
  equivalents with dashes. It saves the resulting map the the
  resources folder as EDN. This map is then used in the
  dali.svg-translate namespace to translate the dashed attribute names
  to the camelcased SVG equivalents. All in the name of consistency."
  []
  (require '[net.cgrand.enlive-html :as html]
           '[clojure.string :as string])
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
