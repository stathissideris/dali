(ns dali.examples
  (:require [clojure.java.io :as java-io]
            [clojure.zip :as zip]
            [dali.batik :as batik]
            [dali.io :as io]
            [dali.layout :as layout]
            [dali.prefab :as prefab]
            [dali.schema :as schema]
            [dali.syntax :as s]
            [dali.utils :as utils]))

(def examples
  [{:filename "hello-world.svg"
    :document
    [:page
     [:circle
      {:stroke :indigo :stroke-width 4 :fill :darkorange}
      [30 30] 20]]}

   {:filename "zig-zag.svg"
    :document
    [:page {:stroke-width 2 :stroke :black :fill :none}
     [:polyline (map #(vector %1 %2) (range 10 210 20) (cycle [10 30]))]
     [:polyline (map #(vector %1 %2) (range 10 210 5) (cycle [60 80]))]
     [:polyline (map #(vector %1 %2) (range 10 210 10) (cycle [100 100 120 120]))]]}
   
   ;;transform syntax demonstrated
   {:filename "transform.svg"
    :document
    [:page {:stroke :black :stroke-width 2 :fill :none}
     
     [:rect {:transform [:rotate [30 30 20]]} ;;rotate around center marked by circle
      [20 10] [20 20]]
     [:circle {:stroke :none :fill :red} [30 20] 2]

     [:rect {:transform [:rotate [10 60 20] :skew-x [30]]}
      [50 10] [20 20]]]}

   {:filename "dasharray.svg"
    :document
    [:page {:width 120 :height 30 :stroke :black :stroke-width 2}
     [:line {:stroke-dasharray [10 5]} [10 10] [110 10]]
     [:line {:stroke-dasharray [5 10]} [10 20] [110 20]]]}

   ;;basic stacking
   {:filename "stack1.svg"
    :document
    [:page {:width 200 :height 40 :stroke :none}
     [:stack
      {:position [10 20] :anchor :left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 20]]
      [:rect {:fill :green} :_ [40 20]]
      [:rect {:fill :orange} :_ [20 20]]]]}

   ;;stacking with anchors
   {:filename "stack2.svg"
    :document
    [:page {:width 200 :height 80 :stroke :none}
     [:stack
      {:position [10 40] :anchor :left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [10 40] 4]]}

   ;;stacking with other anchors
   {:filename "stack3.svg"
    :document
    [:page {:width 310 :height 80 :stroke :none}
     [:stack
      {:position [10 70] :anchor :bottom-left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [10 70] 4]
     
     [:stack
      {:position [170 10] :anchor :top-left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [170 10] 4]]}

   ;;stacking with different directions and gaps
   {:filename "stack4.svg"
    :document
    (let [shapes (fn [s]
                   (list
                    [:text {:font-family "Georgia" :font-size 20
                            :stroke :none :fill :black} s]
                    [:rect :_ [20 20]]
                    [:circle :_ 15]
                    [:polyline [0 0] [20 0] [10 20] [20 20]]))]
      [:page {:width 150 :height 260 :stroke {:paint :black :width 2} :fill :none}
       [:stack {:position [20 20] :direction :right} (shapes "right")]
       [:stack {:position [130 70] :gap 5 :direction :left} (shapes "left")]
       [:stack {:position [40 150] :gap 5 :direction :down} (shapes "down")]
       [:stack {:position [110 250] :gap 18 :direction :up} (shapes "up")]])}

   {:filename "distribute1.svg"
    :document
    [:page {:width 200 :height 60 :stroke :none}
     [:distribute
      {:position [10 20] :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 20]]
      [:rect {:fill :green} :_ [40 20]]
      [:rect {:fill :orange} :_ [20 20]]]

     ;;show centers
     [:g (map #(vector
                :line
                {:stroke {:paint :red :width 2}} [% 40] [% 50])
              (range 35 200 50))]]}

   {:filename "markers1.svg"
    :document
    [:page {:width 250 :height 90 :stroke {:width 2 :paint :black}}
     [:defs
      (prefab/sharp-arrow-end :sharp)
      (prefab/triangle-arrow-end :triangle {:scale 2})
      (prefab/curvy-arrow-end :curvy)
      (prefab/dot-end :dot)
      (prefab/sharp-arrow-end :very-sharp {:height 32})]
     [:polyline
      {:fill :none :marker-end "url(#sharp)"}
      [50 80] [90 30]]
     [:polyline
      {:fill :none :dali/marker-end :triangle}
      [80 80] [120 30]]
     [:polyline
      {:fill :none :marker-end "url(#curvy)"}
      [110 80] [150 30]]
     [:polyline
      {:fill :none :marker-end "url(#dot)"}
      [140 80] [180 30]]
     [:polyline
      {:fill :none :marker-end "url(#very-sharp)"}
      [170 80] [210 30]]]}

   {:filename "markers2-dali.svg"
    :document
    (let [make-arrows
          (fn [translate marker]
            [:g {:transform [:translate translate]}
             [:polyline
              {:fill :none :dali/marker-end marker}
              [80 80] [120 30]]
             [:polyline
              {:fill :none :dali/marker-end marker}
              [80 80] [60 30]]
             [:polyline
              {:fill :none :dali/marker-end marker}
              [80 80] [80 130]]
             [:polyline
              {:fill :none :dali/marker-end marker}
              [80 80] [20 80]]])]
     [:page {:width 220 :height 150 :stroke {:width 2 :paint :black}} ;;TODO page bounds calc blows up with dali markers
      [:defs
       (prefab/sharp-arrow-end :sharp)
       (prefab/triangle-arrow-end :triangle)
       (prefab/curvy-arrow-end :curvy)
       (prefab/dot-end :dot)
       (prefab/sharp-arrow-end :very-sharp {:height 32})]
      [:polyline
       {:fill :none :marker-end "url(#sharp)"}
       [50 80] [90 30]]

      (make-arrows [0 0] :triangle)
      
      [:polyline
       {:fill :none :marker-end "url(#curvy)"}
       [110 80] [150 30]]
      [:polyline
       {:fill :none :marker-end "url(#dot)"}
       [140 80] [180 30]]
      [:polyline
       {:fill :none :marker-end "url(#very-sharp)"}
       [170 80] [210 30]]])}

   {:filename "drop-shadow.svg"
    :document
    [:page {:width 200 :height 200}
     [:defs
      (prefab/drop-shadow :ds {:opacity 0.8 :offset [10 10] :radius 10})
      (prefab/stripe-pattern :stripes {:angle -30 :fill :pink})]
     [:rect {:fill "url(#stripes)"} [0 0] [200 200]]
     [:circle {:fill :green :filter "url(#ds)"} [100 100] 75]]}
   
   {:filename "graph1.svg"
    :document
    [:page {:width 260 :height 140}
     [:stack
      {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [h] [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]])
           [10 30 22 56 90 59 23 12 44 50])]]}

   {:filename "graph2.svg"
    :document
    [:page {:width 270 :height 140}
     [:stack
      {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [h]
             [:stack
              {:direction :up :gap 6}
              [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]]
              [:text {:text-family "Verdana" :font-size 12} (str h)]])
           [10 30 22 56 90 59 23 12 44 50])]]}

   {:filename "graph3.svg"
    :document
    [:page {:width 270 :height 150}
     [:stack
      {:position [10 140], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [[a b c]]
             [:stack
              {:direction :up}
              [:rect {:stroke :none, :fill "#D46A6A"} :_ [20 a]]
              [:rect {:stroke :none, :fill "#D49A6A"} :_ [20 b]]
              [:rect {:stroke :none, :fill "#407F7F"} :_ [20 c]]])
           [[10 10 15]
            [30 10 20]
            [22 10 25]
            [56 10 10]
            [90 10 30]
            [59 22 25]
            [23 10 13]
            [12 6 8]
            [44 22 18]
            [50 20 10]])]]}
   {:filename "graph4.svg"
    :document
    [:page {:width 270 :height 150}
     [:stack {:position [10 140]
              :direction :right
              :anchor :bottom-left
              :gap 2
              :select [:.col]}]
     (map (fn [[a b c]]
            [:stack
             {:direction :up :class :col}
             [:rect {:stroke :none, :fill "#D46A6A"} :_ [20 a]]
             [:rect {:stroke :none, :fill "#D49A6A"} :_ [20 b]]
             [:rect {:stroke :none, :fill "#407F7F"} :_ [20 c]]])
          [[10 10 15]
           [30 10 20]
           [22 10 25]
           [56 10 10]
           [90 10 30]
           [59 22 25]
           [23 10 13]
           [12 6 8]
           [44 22 18]
           [50 20 10]])]}
   {:filename "align-test.svg"
    :document
    [:page {:width 350 :height 220}
     (map (fn [[guide axis]]
            [:g
             [:align
              {:relative-to guide :axis axis}
              [:rect {:stroke :none, :fill "#D49A6A"} [20 0] [20 30]]
              [:rect {:stroke :none, :fill "#D46A6A"} [45 0] [20 20]]
              [:rect {:stroke :none, :fill "#D49A6A"} [70 0] [20 10]]
              [:rect {:stroke :none, :fill "#407F7F"} [95 0] [20 25]]
              [:rect {:stroke :none, :fill "#D49A6A"} [120 0] [20 15]]]
             [:line {:stroke :black} [10 guide] [150 guide]]])
          [[10 :top]
           [100 :bottom]
           [150 :h-center]])
     (map (fn [[guide axis]]
            [:g
             [:align
              {:relative-to guide :axis axis}
              [:rect {:stroke :none, :fill "#D49A6A"} [0 20] [30 20]]
              [:rect {:stroke :none, :fill "#D46A6A"} [0 45] [20 20]]
              [:rect {:stroke :none, :fill "#D49A6A"} [0 70] [10 20]]
              [:rect {:stroke :none, :fill "#407F7F"} [0 95] [25 20]]
              [:rect {:stroke :none, :fill "#D49A6A"} [0 120] [15 20]]]
             [:line {:stroke :black} [guide 10] [guide 150]]])
          [[170 :left]
           [260 :right]
           [330 :v-center]])
     [:align {:relative-to :first :axis :center}
      [:circle {:fill :none :stroke :gray :stroke-dasharray [5 5]} [215 170] 30]
      [:text {:text-family "Verdana" :font-size 12} "aligned!"]]]}
   {:filename "align-test2.svg"
    :document
    [:page {:width 120 :height 120}
     [:align {:relative-to :first :axis :center :select [:.label]}]
     [:circle {:class :label :fill :none :stroke :gray :stroke-dasharray [5 5]} [60 60] 40]
     [:text {:class :label :text-family "Verdana" :font-size 17} "aligned"]
     [:circle {:class :label :fill :none :stroke :black} :_ 50]
     [:rect {:class :label :fill :none :stroke :gray} :_ [60 25]]]}
   {:filename "align-test3.svg"
    :document
    [:page {:width 240 :height 140}
     [:stack {:direction :down :anchor :top-left :gap 10}
      [:align {:relative-to :first :axis :bottom}
       [:rect {:fill :mediumslateblue} [20 60] [50 20]]
       [:rect {:fill :sandybrown} [70 0] [30 60]]
       [:rect {:fill :green} [100 0] [40 10]]
       [:rect {:fill :orange} [140 0] [20 40]]]
      [:text {:text-family "Helvetica" :font-size 14}
       "tests alignment with :relative-to :first"]]]}
   {:filename "composite-layout.svg"
    :document
    [:page {:width 200 :height 120}
     [:layout {:layouts [[:stack {:direction :right}]
                         [:align {:relative-to :first :axis :bottom}]]}
      [:rect {:fill :mediumslateblue :stroke-width 20} [10 80] [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]]}

   {:filename "venn.svg"
    :document
    (let [r 130
          y 200
          x1 200
          x2 370
          outline 3]
      [:page {:width 570 :height 400}
       [:defs
        (prefab/stripe-pattern :stripes {:angle 0 :width 2 :width2 12 :fill :lightgray :fill2 :blue})
        (prefab/stripe-pattern :stripes2 {:angle 90 :width 2 :width2 12 :fill :lightgray :fill2 :red})]
       [:circle {:stroke :none :fill :white} [x1 y] r]
       [:circle {:stroke :none :fill :white} [x2 y] r]
       [:circle {:stroke :none :fill "url(#stripes)" :opacity 0.2} [x1 y] r]
       [:circle {:stroke :none :fill "url(#stripes2)" :opacity 0.2} [x2 y] r]
       [:circle {:stroke {:paint :gray :width 3} :fill :none} [x1 y] r]
       [:circle {:stroke {:paint :gray :width 3} :fill :none} [x2 y] r]])}

   {:filename "connect1.svg"
    :document
    [:page {:stroke :black :fill :none}
     [:defs
      (prefab/sharp-arrow-end :sharp)
      (prefab/triangle-arrow-end :triangle)
      (s/css (str ".grey {fill: lightgrey;}\n"
                  "rect {fill: white;}\n"
                  "rect:hover {fill: orange;}\n"
                  "text {fill: black; stroke: none;}\n"
                  ".connector {marker-end: url(#sharp);}"))]
     [:align {:axis :center}
      [:rect {:id :c} [200 70] [120 150]]
      [:text "center"]]

     [:align {:axis :center}
      [:rect {:id :a :class :grey} [20 20] [100 100]]
      [:text "A"]]

     [:align {:axis :center}
      [:rect {:id :b} [440 70] [50 50]]
      [:text "B"]]

     [:align {:axis :center}
      [:rect {:id :d} [20 350] [50 50]]
      [:text "D"]]

     [:align {:axis :center}
      [:rect {:id :e} [440 230] [50 50]]
      [:text "E"]]

     [:align {:axis :center}
      [:rect {:id :f} [500 70] [50 50]]
      [:text "F"]]

     [:align {:axis :center}
      [:rect {:id :g} [350 300] [50 50]]
      [:text "G"]]
     
     [:connect {:from :a :to :c}]
     [:connect {:from :c :to :b :stroke :green :stroke-width 2.5}]
     [:connect {:from :d :to :c :class :myclass}]
     [:connect {:from :c :to :e :type :-|}]
     [:connect {:from :e :to :f :type :-|}]
     [:connect {:from :e :to :g :type :|- :marker-end "url(#triangle)" :class :foo}]
     [:connect {:from :e :to :g :type :-|}]]}])

(defn render-example [dir filename document]
  (-> document
      ;;schema/validate
      s/dali->ixml
      ;;utils/ixml-zipper utils/zipper-last (utils/dump-zipper zip/prev)
      layout/resolve-layout
      s/ixml->xml
      ;;>pprint
      (io/spit-svg (str dir filename))
      ))

(defn render-examples [dir documents]
  (doseq [{:keys [filename document]} documents]
    (println (format "Rendering example \"%s\"" filename))
    (render-example dir filename document)))

(comment ;;TODO

 ;;render to png
 dali/ixml->xml
 ;;(spit-svg "/tmp/venn2.svg")
 io/xml->svg-document-string
 btk/parse-svg-string
  
 (defn render-folder-to-png [from-folder to-folder]
   (let [dir (io/file from-folder)
         files (file-seq dir)]
     files)))

(comment
 (render-examples examples)
 )
