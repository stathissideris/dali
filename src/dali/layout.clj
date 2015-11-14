(ns dali.layout
  (:require [clojure.string :as string]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
            [dali.batik :as batik]
            [dali.geom :as geom :refer [v+ v- v-half]]
            [dali.syntax :as s]
            [dali.syntax :as syntax]
            [dali.utils :as utils]
            [net.cgrand.enlive-html :as en]
            [retrograde :as retro]))

(def anchors #{:top-left :top :top-right :left :right :bottom-left :bottom :bottom-right :center})

(def direction->default-anchor
  {:down :top
   :up :bottom
   :right :left
   :left :right})

(defn bounds->anchor-point
  [anchor [_ [x y] [w h]]]
  (condp = anchor
    :top-left     [x y]
    :top          [(+ x (/ w 2)) y]
    :top-right    [(+ x w) y]
    :left         [x (+ y (/ h 2))]
    :right        [(+ x w) (+ y (/ h 2))]
    :bottom-left  [x (+ y h)]
    :bottom       [(+ x (/ w 2)) (+ y h)]
    :bottom-right [(+ x w) (+ y h)]
    :center       [(+ x (/ w 2)) (+ y (/ h 2))]))

(defn- tree-path [path]
  (interleave (repeat :content) (rest path)))

(defn- get-in-tree [tree path]
  (get-in tree (tree-path path)))

(defn- assoc-in-tree [tree path value]
  (assoc-in tree (tree-path path) value))

(defn- update-in-tree [tree path fun & params]
  (apply update-in tree (tree-path path) fun params))

(defn- index-tree [document]
  (utils/transform-zipper
   (utils/ixml-zipper document)
   (fn [z]
     (let [node (zip/node z)]
       (if (string? node)
         node
         (let [parent-path (or (some-> z zip/up zip/node :attrs :dali/path) [])
               left-index  (or (some-> z zip/left zip/node :attrs :dali/path last) -1)
               this-path   (conj parent-path (inc left-index))]
           (assoc-in node [:attrs :dali/path] this-path)))))))

(defn- de-index-tree [document] ;;TODO this is probably simpler with enlive
  (utils/transform-zipper
   (utils/ixml-zipper document)
   (fn [z]
     (let [node (zip/node z)]
       (if (string? node)
         node
         (as-> node x
           (update x :attrs dissoc :dali/path)
           (if (empty? (:attrs x)) (dissoc x :attrs) x)))))))

(defn place-top-left
  "Adds a translation transform to an element so that its top-left
  corner is at the passed position."
  [element top-left bounds]
  (let [type (first element)
        [_ current-pos [w h]] bounds]
    (let [tr (v- top-left current-pos)]
      (if (every? zero? tr)
        element
        (s/add-transform element [:translate tr])))))

(defn place-by-anchor
  [element anchor position bounds]
  (let [[_ original-position] bounds
        anchor-point (bounds->anchor-point anchor bounds)]
    (place-top-left
     element
     (v- position (v- anchor-point original-position))
     bounds)))

(defn stack [{{:keys [position direction anchor gap]} :attrs} elements bounds-fn]
  (let [
        gap         (or gap 0)
        position    (or position [0 0])
        direction   (or direction :down)
        anchor      (or anchor (direction->default-anchor direction))
        elements    (if (seq? (first elements)) (first elements) elements) ;;so that you map over elements etc
        
        vertical?   (or (= direction :down) (= direction :up))
        [x y]       position
        advance-pos (if (or (= direction :down) (= direction :right)) + -)
        get-size    (if vertical?
                      (fn get-size [[_ _ [_ h]]] h)
                      (fn get-size [[_ _ [w _]]] w))
        get-pos     (if vertical?
                      (fn get-pos [[_ [_ y] _]] y)
                      (fn get-pos [[_ [x _] _]] x))
        place-point (if vertical?
                      (fn place-point [x y pos] [x pos])
                      (fn place-point [x y pos] [pos y]))
        initial-pos (if vertical? y x)]
    (retro/transform
     [this-gap 0 gap
      bounds nil (bounds-fn element)
      size 0 (get-size bounds)
      pos 0 (get-pos bounds)
      this-pos initial-pos (advance-pos this-pos' size' this-gap')
      element (place-by-anchor element anchor (place-point x y this-pos) bounds)]
     elements)))


(defn distribute [{{:keys [position direction anchor gap]} :attrs} elements bounds-fn]
  (let [direction (or direction :right)
        anchor (or anchor :center)
        vertical? (or (= direction :down) (= direction :up))]
    (if vertical?
      (when (not (#{:center :left :right} anchor))
        (throw (Exception. (str "distribute layout supports only :center :left :right anchors for direction " direction "\n elements: " elements))))
      (when (not (#{:center :top :bottom} anchor))
        (throw (Exception. (str "distribute layout supports only :center :top :bottom anchors for direction " direction "\n elements: " elements)))))
    (let [gap (or gap 0)
          position (or position [0 0])
          elements (if (seq? (first elements)) (first elements) elements) ;;so that you map over elements etc
          
          [x y] position
          bounds (map bounds-fn elements)
          step (+ gap (if vertical?
                        (apply max (map (fn [[_ _ [_ h]]] h) bounds))
                        (apply max (map (fn [[_ _ [w _]]] w) bounds))))
          place-point (if vertical?
                        (fn place-point [x y pos] [x pos])
                        (fn place-point [x y pos] [pos y]))

          first-offset (+ gap (/ step 2))
          positions (condp = direction
                      :down  (range (+ y first-offset) Integer/MAX_VALUE step)
                      :up    (range (- y first-offset) Integer/MIN_VALUE (- step))
                      :right (range (+ x first-offset) Integer/MAX_VALUE step)
                      :left  (range (- x first-offset) Integer/MIN_VALUE (- step)))]
      (map (fn [e pos bounds] (place-by-anchor e anchor (place-point x y pos) bounds))
           elements positions bounds))))

(def ^:private align-axes
  #{:top :bottom :left :right :v-center :h-center :center})

(defn- guide-from-element [node axis bounds-fn]
  (let [[_ [x y] [w h]] (bounds-fn node)]
    (condp = axis
      :top y
      :bottom (+ y h)
      :left x
      :right (+ x w)
      :v-center (+ y (/ h 2))
      :h-center (+ x (/ w 2)))))

(defn- align-center [{{:keys [relative-to axis]} :attrs} elements bounds-fn]
  (when (number? relative-to)
    (utils/exception ":relative-to cannot be a number when :axis is :center"))
  (let [bounds     (map bounds-fn elements)
        rel-bounds (if (= :first relative-to) (first bounds) (last bounds))
        pos        (bounds->anchor-point :center rel-bounds)]
   (map (fn [e b]
          (place-by-anchor e :center pos b)) elements bounds)))

(defn align [{{:keys [relative-to axis]} :attrs :as tag} elements bounds-fn]
  (assert (or (= :first relative-to)
              (= :last relative-to)
              (number? relative-to)) ":relative-to can either be a number or :first or :last")
  (assert (align-axes axis)
          (str ":axis has to be one of " (string/join ", " align-axes)))
  (if (= :center axis)
    (align-center tag elements bounds-fn)
    (let [guide     (if (number? relative-to)
                      relative-to
                      (guide-from-element (if (= :first relative-to)
                                            (first elements)
                                            (last elements)) axis bounds-fn))
          anchor    (condp = axis
                      :left     :top-left
                      :right    :top-right
                      :top      :top-left
                      :bottom   :bottom-left
                      :v-center :top
                      :h-center :left)
          v-guide?  (#{:right :left :v-center} axis)
          bounds    (map bounds-fn elements)
          positions (if v-guide?
                      (map (fn [[_ [_ y]]] y) bounds)
                      (map (fn [[_ [x _]]] x) bounds))]
      (map (fn [e pos bounds]
             (place-by-anchor e anchor (if v-guide?
                                         [guide pos]
                                         [pos guide]) bounds))
           elements positions bounds))))

(def layout-tags ;;TODO make this mutable so that it's extensible
  #{:layout :stack :distribute :align})

(def layout-selector
  [layout-tags])

(def has-selector [(en/attr? :select)])

(defn- get-selector-layouts [document]
  (en/select document has-selector))

(defn- layout-node->group-node [node]
  (-> node
      (assoc :tag :g)
      (update :attrs select-keys [:id :class :dali/path])))

(def remove-node (en/substitute []))

(defn- remove-selector-layouts [document]
  (-> [document]
      (en/transform has-selector remove-node)
      ;;(en/transform layout-selector layout-node->group-node)
      first))

(defn- patch-elements [document new-elements]
  (reduce (fn [doc e]
            (assoc-in-tree doc (-> e :attrs :dali/path) e))
          document new-elements))

(defmulti layout-nodes (fn [tag _ _] (:tag tag)))

(defmethod layout-nodes :stack
  [tag elements bound-fn]
  (stack tag elements bound-fn))

(defmethod layout-nodes :distribute
  [tag elements bound-fn]
  (distribute tag elements bound-fn))

(defmethod layout-nodes :align
  [tag elements bound-fn]
  (align tag elements bound-fn))

(defn composite-layout [layout-tag elements bounds-fn]
  (reduce (fn [elements layout-tag]
            (layout-nodes layout-tag elements bounds-fn))
          elements (->> layout-tag :attrs :layouts (map syntax/dali->ixml))))

(defmethod layout-nodes :layout
  [tag elements bound-fn]
  (composite-layout tag elements bound-fn))

(defn- apply-selector-layout [document layout-tag bounds-fn]
  (let [selector     (get-in layout-tag [:attrs :select])
        elements     (en/select document selector)
        new-elements (layout-nodes layout-tag elements bounds-fn)]
    (patch-elements document new-elements)))

(defn- nested-layout? [node]
  (and (-> node :tag layout-tags)
       (-> node :attrs :select not)))

(defn- selector-layout? [node]
  (and (-> node :tag layout-tags)
       (-> node :attrs :select)))

(defn apply-nested-layouts [doc bounds-fn]
  (utils/transform-zipper-backwards
   (-> doc utils/ixml-zipper utils/zipper-last) ;;perform depth first walk
   (fn [zipper]
     (let [node (zip/node zipper)]
       (if (nested-layout? node)
         (let [new-elements (layout-nodes node (:content node) bounds-fn)]
           (-> node
               layout-node->group-node
               (assoc :content (vec new-elements))))
         node)))))

;;enlive expects id and class to be strings, otherwise id or
;;class-based selectors fail with exceptions. This doesn't seem to be
;;a problem with other attributes.
(defn- fix-id-and-class-for-enlive [doc]
  (utils/transform-zipper
   (utils/ixml-zipper doc)
   (fn [zipper]
     (let [node (zip/node zipper)]
       (-> node
           (utils/safe-update-in [:attrs :id] name)
           (utils/safe-update-in
            [:attrs :class]
            (fn [c]
              (cond (keyword? c) (name c)
                    (sequential? c) (string/join " " (map name c))))))))))

(defn resolve-layout
  ([doc]
   (resolve-layout (batik/context) doc))
  ([ctx doc]
   (let [bounds-fn        #(batik/rehearse-bounds ctx %)
         selector-layouts (get-selector-layouts doc)
         doc              (-> doc
                              remove-selector-layouts
                              fix-id-and-class-for-enlive
                              index-tree
                              (apply-nested-layouts bounds-fn))
         doc              (reduce (fn [doc layout]
                                    (apply-selector-layout doc layout bounds-fn))
                                  doc selector-layouts)]
     (-> doc de-index-tree))))

(comment
  (do
    (def ctx (batik/context))

    (defn marker [pos] [:circle pos 3])
    
    (defn anchor-box [ctx pos anchor]
      [:g
       (marker pos)
       (let [box [:rect [-100 -100] [25 25]]]
         (place-by-anchor box anchor pos
                          (batik/rehearse-bounds ctx box)))])

    (defn anchor-circle [ctx pos anchor]
      [:g
       (marker pos)
       (let [c [:circle [-100 -100] 12.5]]
         (place-by-anchor c anchor pos
                          (batik/rehearse-bounds ctx c)))])

    (defn make-stack [ctx pos anchor direction]
      [:g
       (marker pos)
       (stack
        ctx
        {:position pos :gap 5 :anchor anchor :direction direction}
        [:rect :_ [10 100]]
        [:circle :_ 15]
        [:rect :_ [20 20]]
        [:rect :_ [10 30]]
        [:rect :_ [10 5]]
        [:rect :_ [10 5]])]))
  
  (time
   (s/spit-svg
    (s/dali->hiccup
     (resolve-layout
      [:page
       {:height 750 :width 600, :stroke {:paint :black :width 1} :fill :none}

       ;;test that top-left works
       [:rect [300 50] [100 100]]
       (place-top-left
        [:circle [-100 40] 50]
        [300 50]
        (batik/rehearse-bounds ctx [:circle [-100 40] 50]))

       ;;test that top-left works
       (marker [250 200])
       (place-top-left
        [:rect [-100 -100] [25 25]]
        [250 200]
        (batik/rehearse-bounds ctx [:rect [-100 -100] [50 50]]))

       ;;test all cases of place-by-anchor
       (anchor-box ctx [300 200] :top-left)
       (anchor-box ctx [350 200] :top)
       (anchor-box ctx [400 200] :top-right)
       (anchor-box ctx [300 250] :left)
       (anchor-box ctx [350 250] :center)
       (anchor-box ctx [400 250] :right)
       (anchor-box ctx [300 300] :bottom-left)
       (anchor-box ctx [350 300] :bottom)
       (anchor-box ctx [400 300] :bottom-right)

       (anchor-circle ctx [300 325] :top-left)
       (anchor-circle ctx [350 325] :top)
       (anchor-circle ctx [400 325] :top-right)
       (anchor-circle ctx [300 375] :left)
       (anchor-circle ctx [350 375] :center)
       (anchor-circle ctx [400 375] :right)
       (anchor-circle ctx [300 425] :bottom-left)
       (anchor-circle ctx [350 425] :bottom)
       (anchor-circle ctx [400 425] :bottom-right)

       (make-stack ctx [50 50] :top-left :down)
       (make-stack ctx [120 50] :top :down)
       (make-stack ctx [190 50] :top-right :down)

       (make-stack ctx [50 500] :bottom-left :up)
       (make-stack ctx [120 500] :bottom :up)
       (make-stack ctx [190 500] :bottom-right :up)

       (make-stack ctx [50 575] :left :right)

       (make-stack ctx [165 650] :right :left)

       [:stack
        {:position [300 500], :direction :right, :anchor :bottom-left, :gap 0.5}
        (map (fn [h] [:rect {:stroke :none, :fill :gray} :_ [10 h]])
             (take 10 (repeatedly #(rand 50))))]
       (marker [300 500])

       [:stack
        {:position [300 525], :direction :right, :gap 3}
        (take
         10
         (repeat
          (stack ctx {:gap 3}
                 [:rect :_ [5 5]]
                 [:circle :_ 5]
                 [:circle :_ 1.5])))]
       
       [:stack
        {:position [300 500], :direction :right, :anchor :bottom-left, :gap 0.5}
        (map (fn [h] [:rect {:stroke :none, :fill :gray} :_ [10 h]])
             (take 10 (repeatedly #(rand 50))))]
       
       (marker [300 550])
       (let [tt [:text {:x 30 :y 30 :stroke :none :fill :black :font-family "Georgia" :font-size 40} "This is it"]
             bb (batik/rehearse-bounds ctx tt)]
         (place-by-anchor tt :top-left [300 550] bb))     

       [:distribute
        {:position [220 650], :direction :right, :anchor :bottom, :gap 4}
        (map (fn [h label]
               [:stack
                {:direction :up, :gap 5}
                [:text {:x 30 :y 30 :stroke :none :fill :black :font-family "Verdana" :font-size 6} label #_(format "%.1f" h)]
                [:rect {:stroke :none, :fill :gray} :_ [5 h]]])
             (take 5 (repeatedly #(rand 50)))
             ["This is a long label"
              "Not long"
              "Ag"
              "Bg"
              "Another long one"])]
       (marker [220 650])

       [:line {:stroke :gray} [425 30] [425 70]]
       [:distribute
        {:position [425 50], :gap 2}
        [:rect :_ [10 10]]
        [:circle :_ 8]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]
      
       [:line {:stroke :gray} [425 75] [475 75]]
       [:distribute
        {:position [450 75] :direction :down}
        [:circle :_ 10]
        [:rect :_ [10 10]]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]

       [:line {:stroke :gray} [475 225] [525 225]]
       [:distribute
        {:position [500 225] :direction :up}
        [:circle :_ 10]
        [:rect :_ [10 10]]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]

       [:line {:stroke :gray} [575 225] [575 275]]
       [:distribute
        {:position [575 250], :direction :left}
        [:circle :_ 10]
        [:circle :_ 4]
        [:circle :_ 6]
        [:circle :_ 8]
        [:circle :_ 10]
        [:circle :_ 10]]]))
    
    "s:/temp/svg_stack1.svg"))
  )
