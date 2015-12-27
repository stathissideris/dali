(ns dali.examples.architecture
  (:require [garden.core :refer [css]]
            [clojure.string :as string]
            [dali.layout :as l]
            [dali.syntax :as d]
            [dali.layout.utils :refer [place-by-anchor bounds->anchor-point]]
            [dali.utils :as utils]
            [dali.geom :as geom]
            [dali.prefab :as prefab]
            [net.cgrand.enlive-html :as en]))

(def circle-radius 50)

(defmethod l/layout-nodes :place
  [doc {{:keys [offset relative-to location]} :attrs} elements bounds-fn]
  (let [relative-to-el (first (en/select doc [(keyword (str "#" (name relative-to)))]))
        _              (when-not relative-to-el
                         (throw (ex-info "Cannot find relative-to element" {:element relative-to
                                                                            :document doc})))
        element        (first elements)
        offset         (or offset [0 0])]
    [(place-by-anchor element
                      :top
                      (geom/v+ offset (bounds->anchor-point :bottom (bounds-fn relative-to-el)))
                      (bounds-fn element))]))
(dali/register-layout-tag :place)

(defn text-stack [texts]
  (vec (concat [:dali/stack {:direction :down :gap 6}]
               (map #(vector :text {:font-family "Verdana" :font-size 14} %) texts))))

(defn circle-text
  ([id text cl]
   (circle-text id text cl nil))
  ([id text cl radius]
   (let [radius (or radius circle-radius)]
    [:dali/align {:axis :center}
     [:circle {:id id :class cl :filter "url(#ds)"} :_ radius]
     (text-stack (string/split-lines text))])))

(defn text-box
  ([id text cl]
   (text-box id text cl [20 20]))
  ([id text cl pos]
   [:g {}
    [:dali/align {:axis :center}
     [:rect {:id id :class [cl :box-text] :filter "url(#ds)"} pos [80 80] 10]
     (text-stack (string/split-lines text))]]))

(defn tool-box
  ([id text cl pos]
   (assoc-in (text-box id text cl pos) [1 :class] :dep)))

(defn circle-stack [id & circle-texts]
  [:dali/stack {:id id :direction :down :gap 25}
   (for [[id text cl radius] circle-texts]
     (circle-text id text cl radius))])

(defn label-box [id selector label]
  (let [box-id (utils/keyword-concat id :-box)
        text-id (utils/keyword-concat id :-text)]
    (list
     [:dali/surround {:select selector
                      :padding 25
                      :rounded 10
                      :attrs {:class :file :id box-id :dali/z-index -2}}]
     [:text {:id text-id :font-family "Verdana" :font-size 18} label]
     [:place {:select text-id :relative-to box-id :offset [0 15]}])))

(defn connect
  ([from to]
   (connect from to {}))
  ([from to attrs]
   [:dali/connect (merge {:from from :to to :dali/marker-end :arrow-head} attrs)]))

(def document
  (let [faint-grey "#777777"
        dep        {:stroke-dasharray [7 7]
                    :dali/z-index -1
                    :dali/marker-end {:id :arrow-head :fill faint-grey}
                    :dali/marker-group-attrs {:class :dep}}
        dep-corner (assoc dep :type :|-)]
   [:page
    [:defs
     (prefab/drop-shadow-effect :ds {:opacity 0.3 :offset [5 5] :radius 6})
     (prefab/curvy-arrow-marker :arrow-head {:scale 1.5})
     (d/css (css [[:.dep {:display :block}]
                  [:circle {:fill :none :stroke faint-grey}]
                  [:g.dep [:polyline {:stroke faint-grey}]]
                  [:.box-text {:stroke :black}]
                  [:polyline {:stroke :black}]
                  [:.main-fun {:fill "#bf9af2"}]
                  [:.corp-fun {:fill "#e1cca1"}]
                  [:.tool {:fill "#bffdc7"}]
                  [:.file {:fill "#00c3cf" :stroke "#00acb6"}]
                  [:.button {:fill "#ffffff"}]
                  [:.connector {:fill :none}]]))]
    [:text {:font-family "Georgia" :font-size 30 :x 50 :y 60}
     "Architecture Overview"]
    (text-box :deps-toggle "show\ntools" :button [1200 20])
    [:dali/stack {:direction :right :gap 60 :position [30 400]}
     (circle-text :maintenance "corp\nmaintenance" :corp-fun)
     (circle-text :extraction "extraction" [:main-fun :extractor])
     (circle-text :importing "importing" [:main-fun :extractor])
     (circle-text :enrichment "enrichment" [:main-fun :extractor])
     (circle-stack :limits-clj
                   [:limits "limits" :main-fun]
                   [:corp-targets "corp targets" :corp-fun])
     (circle-stack :incidents-clj
                   [:limits-grouping "limits\ngrouping" :main-fun]
                   [:corp-targets-grouping "corp\ntargets\ngrouping" :corp-fun])
     (circle-stack :reporter-clj
                   [:limits-reporting "limits\nreporting" :main-fun]
                   [:corp-targets-reporting "corp\ntargets\nreporting" :corp-fun])
     (circle-text :corp-actions "corp\nactions" :corp-fun)]

    [:dali/distribute {:direction :right :gap 120}
     (tool-box :sc "External\nData\ntools" :tool [380 170])
     (tool-box :postgres "Database\nstorage\ntools" :tool [0 170])
     (tool-box :r "Stats\ntools" :tool [0 170])
     (tool-box :jira "Issue\ntracking\ntools" :tool [0 110])]
    
    (label-box :extractor  [:.extractor] "extractor")
    (label-box :limits-clj [:#limits-clj] "limits.clj")
    (label-box :incidents-clj [:#incidents-clj] "incidents.clj")
    (label-box :reporter-clj [:#reporter-clj] "reporter.clj")

    ;;data flow
    (connect :maintenance :extraction)
    (connect :extraction :importing)
    (connect :importing :enrichment)
    (connect :enrichment :limits)
    (connect :enrichment :corp-targets)
    (connect :limits :limits-grouping)
    (connect :corp-targets :corp-targets-grouping)
    (connect :limits-grouping :limits-reporting)
    (connect :corp-targets-grouping :corp-targets-reporting)
    (connect :corp-targets-reporting :corp-actions)

    ;;deps
    (connect :importing :postgres dep)
    (connect :enrichment :postgres dep)
    (connect :limits-clj-box :postgres dep-corner)
    (connect :limits :r dep)
    (connect :maintenance :jira dep-corner)
    (connect :extraction :sc dep-corner)
    (connect :corp-actions :jira dep-corner)

    (d/javascript
     (string/join
      "\n"
      ["function toggleDeps() {"
       "  s = document.styleSheets[0].cssRules[0].style;"
       "  if(s.display == 'block') { s.display = 'none' } else {s.display = 'block'}"
       "}"
       "document.getElementById('deps-toggle').addEventListener('click', toggleDeps);"]))]))

