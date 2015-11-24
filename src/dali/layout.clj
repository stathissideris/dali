(ns dali.layout
  (:require [clojure
             [string :as string]
             [zip :as zip]]
            [dali
             [batik :as batik]
             [syntax :as syntax]
             [utils :as utils]]
            [dali.layout
             [align :as align]
             [connect :as connect]
             [distribute :as distribute]
             [stack :as stack]]
            [net.cgrand.enlive-html :as en]))

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


;;;;;;;;;;;;;;;; extensibility ;;;;;;;;;;;;;;;;

(def layout-tags ;;TODO make this mutable so that it's extensible
  #{:layout :stack :distribute :align :connect})

(defmulti layout-nodes (fn [_ tag _ _] (:tag tag)))

(defmethod layout-nodes :stack
  [document tag elements bound-fn]
  (stack/stack document tag elements bound-fn))

(defmethod layout-nodes :distribute
  [document tag elements bound-fn]
  (distribute/distribute document tag elements bound-fn))

(defmethod layout-nodes :align
  [document tag elements bound-fn]
  (align/align document tag elements bound-fn))

(defmethod layout-nodes :connect
  [document tag elements bound-fn]
  (connect/connect document tag elements bound-fn))

(defn- composite-layout [document layout-tag elements bounds-fn]
  (reduce (fn [elements layout-tag]
            (layout-nodes document layout-tag elements bounds-fn))
          elements (->> layout-tag :attrs :layouts (map syntax/dali->ixml))))

(defmethod layout-nodes :layout
  [document tag elements bound-fn]
  (composite-layout document tag elements bound-fn))


;;;;;;;;;;;;;;;; layout infrastructure ;;;;;;;;;;;;;;;;

(def layout-selector
  [layout-tags])

(def remove-node (en/substitute []))

(defn- has-content? [tag]
  (some? (not-empty (:content tag))))

(def selector-layout-selector
  [(en/pred
    #(and (not (has-content? %))
          (layout-tags (:tag %))))])

(defn- nested-layout? [node]
  (and (-> node :tag layout-tags)
       (has-content? node)))

(defn- append? [element]
  (= :append (some-> element :attrs :dali/path)))

(defn- get-selector-layouts [document]
  (en/select document selector-layout-selector))

(defn- layout-node->group-node [node]
  (-> node
      (assoc :tag :g)
      (update :attrs select-keys [:id :class :dali/path])))

(defn- remove-selector-layouts [document]
  (-> [document]
      (en/transform selector-layout-selector remove-node)
      first))

(defn- patch-elements [document new-elements]
  (reduce (fn [doc e]
            (if (append? e)
              (update doc :content conj e)
              (assoc-in-tree doc (-> e :attrs :dali/path) e)))
          document new-elements))

(defn- apply-selector-layout [document layout-tag bounds-fn]
  (let [selector     (get-in layout-tag [:attrs :select])
        elements     (en/select document selector)
        new-elements (layout-nodes document layout-tag elements bounds-fn)]
    (patch-elements document new-elements)))

(defn- apply-nested-layouts [document bounds-fn]
  (utils/transform-zipper-backwards
   (-> document utils/ixml-zipper utils/zipper-last) ;;perform depth first walk
   (fn [zipper]
     (let [node (zip/node zipper)]
       (if (nested-layout? node)
         (let [new-elements (layout-nodes document node (:content node) bounds-fn)]
           (-> node
               layout-node->group-node
               (assoc :content (vec (filter (complement append?) new-elements)))))
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

(defn- has-page-dimensions? [doc]
  (and (-> doc :attrs :width)
       (-> doc :attrs :height)))

(defn- page-dimensions-100
  "See https://www.w3.org/Graphics/SVG/WG/wiki/Intrinsic_Sizing"
  [doc bounds-fn]
  (-> doc
      (assoc-in [:attrs :width] "100%")
      (assoc-in [:attrs :height] "100%")))

(defn resolve-layout
  ([doc]
   (when (nil? doc) (throw (utils/exception "Cannot resolve layout of nil document")))
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
                                  doc selector-layouts)
         doc              (if (has-page-dimensions? doc)
                            doc
                            (page-dimensions-100 doc bounds-fn))]
     (-> doc de-index-tree))))
