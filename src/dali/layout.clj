(ns dali.layout
  (:require [clojure
             [string :as string]
             [zip :as zip]]
            [dali :as d]
            [dali
             [batik :as batik]
             [geom :refer [v-]]
             [syntax :as syntax]
             [utils :as utils]
             [svg-context :as ctx]]
            [net.cgrand.enlive-html :as en]))

(defn- zip-up
  [loc]
  (if (= :end (loc 1))
    loc
    (let [p (zip/up loc)]
      (if p
        (recur p)
        loc))))

(defn- tree-path [path]
  (interleave (repeat :content) (rest path)))

(defn- get-in-tree [tree path]
  (get-in tree (tree-path path)))

(defn- assoc-in-tree [tree path value]
  (assoc-in tree (tree-path path) value))

(defn- zipper-point-to-path [zipper path]
  (let [right-times (fn [z i] (nth (iterate zip/right z) i))]
    (right-times
     (reduce (fn [z i]
               (let [z (right-times z i)]
                 (or (zip/down z) z))) zipper (butlast path))
     (last path))))

(defn- assoc-in-zipper [zipper path value]
  (let [z (zipper-point-to-path (zip-up zipper) path)]
    (zip/replace z value)))

(defn- update-in-tree [tree path fun & params]
  (apply update-in tree (tree-path path) fun params))

(defn- index-tree
  ([document]
   (index-tree document []))
  ([document path-prefix]
   (utils/transform-zipper
    (utils/ixml-zipper document)
    (fn [z]
      (let [node (zip/node z)]
        (if (string? node)
          node
          (let [parent-path (or (some-> z zip/up zip/node :attrs :dali/path) [])
                left-index  (or (some-> z zip/left zip/node :attrs :dali/path last) -1)
                this-path   (conj parent-path (inc left-index))]
            (assoc-in node [:attrs :dali/path] (into path-prefix this-path)))))))))

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

(defn- z-index [element]
  (or (some-> element :attrs :dali/z-index) 0))

(defn- apply-z-order [document]
  (utils/transform-zipper
   (utils/ixml-zipper document)
   (fn [z]
     (let [node (zip/node z)]
       (if (string? node)
         node
         (update node :content #(sort-by z-index %)))))))

(defn- group-for-composite-layout []
  (keyword (gensym "node-group-")))

(defn- composite->normal-layout [zipper]
  (let [node (zip/node zipper)]
    (if-not (= :dali/layout (-> node :tag))
      node
      (let [group-class (group-for-composite-layout)
            layouts     (-> node :attrs :layouts)]
        {:tag :g
         :content
         (concat (map #(syntax/add-class % group-class) (:content node))
                 (map (fn [[tag attrs]]
                        {:tag tag
                         :attrs
                         (assoc attrs :select [(utils/to-enlive-class-selector group-class)])})
                      layouts))}))))

(defn- transform-composite-layouts
  ([document]
   (utils/transform-zipper (utils/ixml-zipper document) composite->normal-layout)))

(defn- hoover-obsolete-nodes
  ([document]
   (loop [zipper (utils/ixml-zipper document)]
     (if (zip/end? zipper)
       (zip/root zipper)
       (let [node   (zip/node zipper)
             zipper (if (or (and (= :g (:tag node)) (empty? (:content node))) ;;empty groups
                            (= "dali-ghost" (some-> node :attrs :class)))
                      (zip/remove zipper)
                      zipper)]
         (recur (zip/next zipper)))))))

;;;;;;;;;;;;;;;; extensibility ;;;;;;;;;;;;;;;;

(defmulti layout-nodes (fn [doc tag nodes bounds-fn] (:tag tag)))

;;;;;;;;;;;;;;;; layout infrastructure ;;;;;;;;;;;;;;;;

(def remove-node (en/substitute []))

(defn- has-content? [tag]
  (some? (not-empty (:content tag))))

(defn selector-layout-selector []
  [(en/pred
    #(and (not (has-content? %))
          (d/layout-tag? (:tag %))))])

(defn- new-node? [element]
  (nil? (some-> element :attrs :dali/path)))

(defn- selected-node? [element]
  (= :selected (some-> element :attrs :dali/layout-type)))

(defn- nested-node? [element]
  (= :nested (some-> element :attrs :dali/layout-type)))

(defn- get-selector-layouts [document]
  (en/select document (selector-layout-selector)))

(defn- layout-node->group-node [node elements]
  (if (= 1 (count elements))
    (-> (first elements)
        (assoc-in [:attrs :dali/path] (-> node :attrs :dali/path)))
    (-> node
        (assoc :tag :g)
        (update :attrs select-keys [:id :class :dali/path :position])
        (assoc :content elements))))

(defn- remove-selector-layouts [document]
  (-> [document]
      (en/transform (selector-layout-selector) remove-node)
      first))

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
              (cond (keyword? c)    (name c)
                    (sequential? c) (string/join " " (map name c))
                    :else           c))))))))

(defn- set-dali-path [xml-node path]
  (assoc-in xml-node [:attrs :dali/path] path))

(defn- inc-path [path]
  (update path (dec (count path)) inc))

(defn- patch-elements [zipper new-elements]
  (let [original-path (-> zipper zip/node :attrs :dali/path)
        z
        (reduce (fn [z e]
                  (assoc-in-zipper z (-> e :attrs :dali/path) e))
                zipper (map fix-id-and-class-for-enlive new-elements))]
    (zipper-point-to-path (zip-up z) original-path))) ;;fix them so enlive can find them

(defn- layout-selector [node]
  (let [s (get-in node [:attrs :select])]
    (if (keyword? s)
      [(utils/to-enlive-id-selector s)]
      s)))

(defn- get-nodes-to-layout [layout-node document]
  (let [assoc-type (fn [node t] (assoc-in node [:attrs :dali/layout-type] t))]
    (concat (if-let [selector (layout-selector layout-node)]
              (let [selected (en/select document selector)]
                (when (empty? selected)
                  (throw (ex-info "select clause of layout node did not match any nodes"
                                  {:layout-node layout-node
                                   :document document})))
                (map #(assoc-type % :selected) selected)))
            (map #(assoc-type % :nested)
                 (:content layout-node)))))

(defn- apply-position [zipper ctx group-node]
  (if-not group-node
    zipper
    (if-let [p (some-> group-node :attrs :position)]
      (if (empty? (:content group-node))
        (throw (ex-info "Selector layouts cannot have a :position attribute"
                        {:node group-node}))
        (let [doc (zip/root zipper)
              [_ current-pos] (ctx/get-relative-bounds ctx doc (-> group-node :attrs :dali/path))]
          (patch-elements
           zipper
           [(-> group-node
                (syntax/add-transform [:translate (v- p current-pos)])
                (update :attrs dissoc :position))])))
      zipper)))

(defn- apply-layout [layout-node zipper ctx]
  (let [current-doc     (zip/root zipper)
        nodes-to-layout (get-nodes-to-layout layout-node current-doc)
        output-nodes    (layout-nodes current-doc layout-node nodes-to-layout
                                      #(ctx/get-bounds ctx current-doc (-> % :attrs :dali/path)))
        new-nodes       (filter new-node? output-nodes)
        nested-nodes    (filter nested-node? output-nodes)
        selected-nodes  (filter selected-node? output-nodes)
        group-node      (layout-node->group-node layout-node (concat new-nodes nested-nodes))]
    (-> zipper
        (patch-elements (concat [group-node] selected-nodes))
        (apply-position ctx group-node))))

(defn- apply-layouts [document ctx]
  (let [layout?           (fn [node] (d/layout-tag? (-> node :tag)))]
    (utils/transform-zipper-eval-order
     (utils/ixml-zipper document)
     (fn walker [zipper]
       (let [node (zip/node zipper)]
         (if (layout? node) (apply-layout node zipper ctx) zipper))))))

(defn- has-page-dimensions? [doc]
  (and (-> doc :attrs :width)
       (-> doc :attrs :height)))

(defn- infer-page-dimensions
  [doc ctx]
  (let [[_ [x y] [w h]] (ctx/get-bounds ctx doc [0])]
    (-> doc
        (assoc-in [:attrs :width] (+ x w 10))
        (assoc-in [:attrs :height] (+ y h 10)))))

(defn resolve-layout
  ([doc]
   (when (nil? doc) (throw (utils/exception "Cannot resolve layout of nil document")))
   ;;do this early because batik/context uses ixml->xml which needs enlive-friendly IDs
   (let [doc (-> doc
                 transform-composite-layouts
                 fix-id-and-class-for-enlive)]
     (resolve-layout (batik/context) doc)))
  ([ctx doc]
   (let [doc (-> doc
                 index-tree
                 (apply-layouts ctx))
         doc (apply-z-order doc)
         doc (if (has-page-dimensions? doc)
               doc
               (infer-page-dimensions doc ctx))]
     (-> doc de-index-tree hoover-obsolete-nodes))))
