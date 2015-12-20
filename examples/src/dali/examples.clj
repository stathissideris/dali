(ns dali.examples
  (:require [clojure.java.io :as java-io]
            [clojure.string :as string]
            [clojure.zip :as zip]
            [dali.batik :as batik]
            [dali.io :as io]
            [dali.layout :as layout]
            [dali.prefab :as prefab]
            [dali.schema :as schema]
            [dali.syntax :as s]
            [dali.utils :as utils]))

(def examples
  [{:filename "hello-world"
    :document
    [:page
     [:circle
      {:stroke :indigo :stroke-width 4 :fill :darkorange}
      [30 30] 20]]}

   {:filename "zig-zag"
    :document
    [:page {:stroke-width 2 :stroke :black :fill :none}
     [:polyline (map #(vector %1 %2) (range 10 210 20) (cycle [10 30]))]
     [:polyline (map #(vector %1 %2) (range 10 210 5) (cycle [60 80]))]
     [:polyline (map #(vector %1 %2) (range 10 210 10) (cycle [100 100 120 120]))]]}
   
   ;;transform syntax demonstrated
   {:filename "transform"
    :document
    [:page {:stroke :black :stroke-width 2 :fill :none}
     
     [:rect {:transform [:rotate [30 30 20]]} ;;rotate around center marked by circle
      [20 10] [20 20]]
     [:circle {:stroke :none :fill :red} [30 20] 2]

     [:rect {:transform [:rotate [10 60 20] :skew-x [30]]}
      [50 10] [20 20]]]}

   {:filename "dasharray"
    :document
    [:page {:width 120 :height 30 :stroke :black :stroke-width 2}
     [:line {:stroke-dasharray [10 5]} [10 10] [110 10]]
     [:line {:stroke-dasharray [5 10]} [10 20] [110 20]]]}

   ;;basic stacking
   {:filename "stack1"
    :document
    [:page {:width 200 :height 40 :stroke :none}
     [:dali/stack
      {:position [10 20] :anchor :left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 20]]
      [:rect {:fill :green} :_ [40 20]]
      [:rect {:fill :orange} :_ [20 20]]]]}

   ;;stacking with anchors
   {:filename "stack2"
    :document
    [:page {:width 200 :height 80 :stroke :none}
     [:dali/stack
      {:position [10 40] :anchor :left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [10 40] 4]]}

   ;;stacking with other anchors
   {:filename "stack3"
    :document
    [:page {:width 310 :height 80 :stroke :none}
     [:dali/stack
      {:position [10 70] :anchor :bottom-left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [10 70] 4]
     
     [:dali/stack
      {:position [170 10] :anchor :top-left :direction :right}
      [:rect {:fill :mediumslateblue} :_ [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]
     [:circle {:fill :red} [170 10] 4]]}

   ;;stacking with different directions and gaps
   {:filename "stack4"
    :document
    (let [shapes (fn [s]
                   (list
                    [:text {:font-family "Georgia" :font-size 20
                            :stroke :none :fill :black} s]
                    [:rect :_ [20 20]]
                    [:circle :_ 15]
                    [:polyline [0 0] [20 0] [10 20] [20 20]]))]
      [:page {:width 150 :height 260 :stroke {:paint :black :width 2} :fill :none}
       [:dali/stack {:position [20 20] :direction :right} (shapes "right")]
       [:dali/stack {:position [130 70] :gap 5 :direction :left} (shapes "left")]
       [:dali/stack {:position [40 150] :gap 5 :direction :down} (shapes "down")]
       [:dali/stack {:position [110 250] :gap 18 :direction :up} (shapes "up")]])}

   {:filename "distribute1"
    :document
    [:page {:width 200 :height 60 :stroke :none}
     [:dali/distribute
      {:direction :right}
      [:rect {:fill :mediumslateblue} [10 20] [50 20]]
      [:rect {:fill :sandybrown} [0 20] [30 20]]
      [:rect {:fill :green} [0 20] [40 20]]
      [:rect {:fill :orange} [0 20] [20 20]]]

     ;;show centers
     [:g (map #(vector
                :line
                {:stroke {:paint :red :width 2}} [% 40] [% 50])
              (range 35 200 50))]]}

   {:filename "markers1"
    :document
    [:page
     [:defs
      (s/css (str "polyline {stroke: black; stroke-width: 2;}"))
      (prefab/sharp-arrow-marker :sharp {:scale 2})
      (prefab/triangle-arrow-marker :triangle {:scale 2})
      (prefab/curvy-arrow-marker :curvy {:scale 2})
      (prefab/dot-marker :dot {:radius 6})
      (prefab/sharp-arrow-marker :very-sharp {:width 16 :height 36})]
     [:line {:stroke :lightgrey} [50 30] [230 30]]
     [:polyline {:dali/marker-end {:id :sharp :fill :red}} [50 80] [90 30]]
     [:polyline {:dali/marker-end :triangle} [80 80] [120 30]]
     [:polyline {:dali/marker-end :curvy} [110 80] [150 30]]
     [:polyline {:dali/marker-end :dot} [140 80] [180 30]]
     [:polyline {:dali/marker-end :very-sharp} [170 80] [210 30]]]}

   {:filename "markers2-dali"
    :document
    (let [make-end-arrows
          (fn make-end-arrows
            ([translate marker]
             (make-end-arrows translate marker :polyline))
            ([translate marker tag]
             (let [attrs {:fill :none :dali/marker-end marker}]
               [:g {:transform [:translate translate]}
                [tag attrs [80 80] [120 30]]
                [tag attrs [80 80] [60 30]]
                [tag attrs [80 80] [80 130]]
                [tag attrs [80 80] [20 80]]])))
          make-start-arrows
          (fn make-start-arrows
            ([translate marker]
             (make-start-arrows translate marker :polyline))
            ([translate marker tag]
             (let [attrs {:fill :none :dali/marker-start marker}]
               [:g {:transform [:translate translate]}
                [tag attrs [95 60] [120 30]]
                [tag attrs [72.5 60] [60 30]]
                [tag attrs [80 100] [80 130]]
                [tag attrs [60 80] [20 80]]])))
          make-both-arrows
          (fn [translate marker-start marker-end]
            (let [attrs {:fill :none :dali/marker-start marker-start :dali/marker-end marker-end}]
              [:g {:transform [:translate translate]}
               [:polyline attrs [95 60] [120 30]]
               [:polyline attrs [72.5 60] [60 30]]
               [:polyline attrs [80 100] [80 130]]
               [:polyline attrs [60 80] [20 80]]]))

          arrow-column
          (fn [x marker]
            [:g
             (make-end-arrows [x 0] marker)
             (make-start-arrows [x 130] marker)
             (make-both-arrows [x 260] marker marker)
             (make-end-arrows [x 390] marker :line)
             (make-start-arrows [x 520] marker :line)])]
     [:page
      [:defs
       (s/css (str "polyline {stroke: black; stroke-width: 2;}\n"
                   "line {stroke: black; stroke-width: 2;}"))
       (prefab/triangle-arrow-marker :triangle)
       (prefab/sharp-arrow-marker :sharp)
       (prefab/curvy-arrow-marker :curvy)
       (prefab/dot-marker :dot)]

      (arrow-column 0 :triangle)
      (arrow-column 150 :sharp)
      (arrow-column 300 :curvy)
      (arrow-column 450 :dot)
      
      [:polyline {:dali/marker-end :triangle :fill :none}
       (map #(vector %1 %2) (range 10 210 20) (cycle [680 700]))]])}

   {:filename "drop-shadow"
    :document
    [:page {:width 200 :height 200}
     [:defs
      (prefab/drop-shadow-effect :ds {:opacity 0.8 :offset [10 10] :radius 10})
      (prefab/stripe-pattern :stripes {:angle -30 :fill :pink})]
     [:rect {:fill "url(#stripes)"} [0 0] [200 200]]
     [:circle {:fill :green :filter "url(#ds)"} [100 100] 75]]}
   
   {:filename "graph1"
    :document
    [:page {:width 260 :height 140}
     [:dali/stack
      {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [h] [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]])
           [10 30 22 56 90 59 23 12 44 50])]]}

   {:filename "graph2"
    :document
    [:page {:width 270 :height 140}
     [:dali/stack
      {:position [10 130], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [h]
             [:dali/stack
              {:direction :up :gap 6}
              [:rect {:stroke :none, :fill :darkorchid} :_ [20 h]]
              [:text {:text-family "Verdana" :font-size 12} (str h)]])
           [10 30 22 56 90 59 23 12 44 50])]]}

   {:filename "graph3"
    :document
    [:page {:width 270 :height 150}
     [:dali/stack
      {:position [10 140], :direction :right, :anchor :bottom-left, :gap 2}
      (map (fn [[a b c]]
             [:dali/stack
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
   {:filename "graph4"
    :document
    [:page {:width 270 :height 150}
     (map (fn [[a b c]]
            [:dali/stack
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
           [50 20 10]])
     [:dali/stack {:position [10 140]
                   :direction :right
                   :anchor :bottom-left
                   :gap 2
                   :select [:.col]}]]}
   {:filename "align-test"
    :document
    [:page {:width 350 :height 220}
     (map (fn [[guide axis]]
            [:g
             [:dali/align
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
             [:dali/align
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
     [:dali/align {:relative-to :first :axis :center}
      [:circle {:fill :none :stroke :gray :stroke-dasharray [5 5]} [215 170] 30]
      [:text {:text-family "Verdana" :font-size 12} "aligned!"]]]}
   {:filename "align-test2"
    :document
    [:page {:width 120 :height 120}
     [:circle {:class :label :fill :none :stroke :gray :stroke-dasharray [5 5]} [60 60] 40]
     [:text {:class :label :text-family "Verdana" :font-size 17} "aligned"]
     [:circle {:class :label :fill :none :stroke :black} :_ 50]
     [:rect {:class :label :fill :none :stroke :gray} :_ [60 25]]
     [:dali/align {:relative-to :first :axis :center :select [:.label]}]]}
   {:filename "align-test3"
    :document
    [:page {:width 240 :height 140}
     [:dali/stack {:direction :down :anchor :top-left :gap 10}
      [:dali/align {:relative-to :first :axis :bottom}
       [:rect {:fill :mediumslateblue} [20 60] [50 20]]
       [:rect {:fill :sandybrown} [70 0] [30 60]]
       [:rect {:fill :green} [100 0] [40 10]]
       [:rect {:fill :orange} [140 0] [20 40]]]
      [:text {:text-family "Helvetica" :font-size 14}
       "tests alignment with :relative-to :first"]]]}
   {:filename "align-test4" ;;line align-test2, but invalid, this is here to ensure that selector layouts that don't match can still render
    :document
    [:page {:width 120 :height 120}
     [:dali/align {:relative-to :first :axis :center :select [:.label]}]
     [:circle {:class :label :fill :none :stroke :gray :stroke-dasharray [5 5]} [60 60] 40]
     [:text {:class :label :text-family "Verdana" :font-size 17} "aligned"]
     [:circle {:class :label :fill :none :stroke :black} :_ 50]
     [:rect {:class :label :fill :none :stroke :gray} :_ [60 25]]]}
   {:filename "composite-layout"
    :document
    [:page
     [:dali/layout {:layouts [[:dali/stack {:direction :right}]
                              [:dali/align {:relative-to :first :axis :bottom}]]}
      [:rect {:fill :mediumslateblue :stroke-width 20} [10 80] [50 20]]
      [:rect {:fill :sandybrown} :_ [30 60]]
      [:rect {:fill :green} :_ [40 10]]
      [:rect {:fill :orange} :_ [20 40]]]]}

   {:filename "venn"
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

   {:filename "connect1"
    :document
    [:page {:stroke :black :fill :none}
     [:defs
      (s/css (str ".marker {fill: black; stroke: none;}"
                  ".grey {fill: lightgrey;}\n"
                  "rect {fill: white;}\n"
                  "rect:hover {fill: orange;}\n"
                  "text {fill: black; stroke: none;}"))
      (prefab/sharp-arrow-marker :sharp)
      (prefab/sharp-arrow-marker :big-sharp {:scale 2})
      (prefab/triangle-arrow-marker :triangle)]
     [:dali/align {:axis :center}
      [:rect {:id :c :transform [:translate [0 -20]]} [200 70] [120 150]]
      [:text "center"]]

     [:dali/align {:axis :center}
      [:rect {:id :a :class :grey} [20 20] [100 100]]
      [:text "A"]]

     [:dali/align {:axis :center}
      [:rect {:id :b :transform [:translate [0 -20]]} [440 70] [50 50]]
      [:text "B"]]

     [:dali/align {:axis :center}
      [:rect {:id :d} [20 350] [50 50]]
      [:text "D"]]

     [:dali/align {:axis :center}
      [:rect {:id :e} [440 230] [50 50]]
      [:text "E"]]

     [:dali/align {:axis :center}
      [:rect {:id :f} [500 70] [50 50]]
      [:text "F"]]

     [:dali/align {:axis :center}
      [:rect {:id :g} [350 300] [50 50]]
      [:text "G"]]
     
     [:dali/connect {:from :a :to :c :dali/marker-end :sharp}]

     ;; :fill :green doesn't work because CSS wins
     [:dali/connect {:from :c :to :b :stroke :green :stroke-width 2.5
                :dali/marker-end {:id :big-sharp :style "fill: green;"}}]

     [:dali/connect {:from :d :to :c :class :myclass :dali/marker-end :sharp}]
     [:dali/connect {:from :c :to :e :type :-| :dali/marker-end :sharp}]
     [:dali/connect {:from :e :to :f :type :-| :dali/marker-end :sharp}]
     [:dali/connect {:from :e :to :g :type :|- :class :foo :dali/marker-end :triangle}]
     [:dali/connect {:from :e :to :g :type :-| :dali/marker-end :sharp}]]}

   {:filename "matrix1"
    :document
    [:page
     [:defs
      (s/css (str "polyline {fill: none; stroke: black;}\n"
                  "rect {fill: none; stroke: black;}\n"))
      (prefab/sharp-arrow-marker :sharp)]
     [:dali/matrix {:position [100 100] :columns 4 :row-padding 5 :column-padding 20}
      [:rect :_ [50 50]]
      [:rect {:id :c} :_ [50 70]]
      [:rect {:id :b} :_ [70 50]]
      [:rect :_ [30 30]]

      [:rect {:id :e} :_ [30 90]]
      [:rect {:id :d} :_ [30 30]]
      [:rect {:id :a} :_ [50 50]]
      [:rect :_ [70 50]]

      [:rect :_ [100 100]]
      [:rect :_ [90 30]]
      [:rect :_ [50 50]]
      [:rect :_ [20 50]]]

     [:dali/connect {:from :a :to :b :dali/marker-end :sharp}]
     [:dali/connect {:from :b :to :c :dali/marker-end :sharp}]
     [:dali/connect {:from :c :to :d :dali/marker-end :sharp}]
     [:dali/connect {:from :d :to :e :dali/marker-end :sharp}]

     [:dali/matrix {:columns 5 :padding 10 :position [50 400]}
      (take 25 (repeat [:rect :_ [20 20]]))]]}

   {:filename "send-to-bottom"
    :document
    [:page
     [:defs
      (s/css (str "polyline {fill: none; stroke: black;}\n"
                  "circle {fill: lightgreen; stroke: black;}\n"
                  "rect {fill: green; stroke: black;}\n"))
      (prefab/sharp-arrow-marker :sharp)]
     [:dali/stack {:id :st :direction :down :position [100 50]}
      [:circle :_ 50]
      [:circle :_ 50]]
     [:rect {:id :the-rect :dali/z-index -1} [25 100] [150 100]]]}])

(defn render-example [dir filename document]
  (with-redefs [dali.layout/group-for-composite-layout (constantly :node-group-38348)]
    (io/render-svg document (str dir filename ".svg"))))

(defn render-examples [dir documents]
  (doseq [{:keys [filename document]} documents]
    (print (format "Rendering example SVG \"%s\"" filename))
    (try
      (render-example dir filename document)
      (println)
      (catch Exception e
        (println " <- FAILED:" (-> e .getClass .getName) (.getMessage e))))))

(defn render-example-png [dir filename document]
  (io/render-png document (str dir filename ".png") {:scale 2}))

(defn render-examples-png [dir documents]
  (doseq [{:keys [filename document]} documents]
    (print (format "Rendering example PNG \"%s\"" filename))
    (try
      (render-example-png dir filename document)
      (println)
      (catch Exception e
        (println " <- FAILED:" (-> e .getClass .getName) (.getMessage e))))))
