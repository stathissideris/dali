(ns dali.batik
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [dali
             [dom :as dom]
             [syntax :as s]])
  (:import [java.awt.geom AffineTransform PathIterator]
           java.io.ByteArrayInputStream
           java.nio.charset.StandardCharsets
           [org.apache.batik.bridge BridgeContext GVTBuilder UserAgentAdapter]
           [org.apache.batik.dom.svg SAXSVGDocumentFactory SVGDOMImplementation]
           [org.apache.batik.transcoder TranscoderInput TranscoderOutput]
           [org.apache.batik.gvt RootGraphicsNode CompositeGraphicsNode TextNode ShapeNode]
           org.apache.batik.transcoder.image.PNGTranscoder))

;;Batik - calculating bounds of cubic spline
;;http://stackoverflow.com/questions/10610355/batik-calculating-bounds-of-cubic-spline?rq=1

;;Wrong values of bounding box for text element using Batik
;;http://stackoverflow.com/questions/12166280/wrong-values-of-bounding-box-for-text-element-using-batik

(defprotocol BatikContext
  (gvt-node [this dom-node])
  (gvt-node-by-id [this id])
  (rehearse-bounds [this element]))

(defn- to-rect [rect]
  (when rect
    [:rect
     [(.getX rect)
      (.getY rect)]
     [(.getWidth rect)
      (.getHeight rect)]]))

(defmacro maybe [call]
  `(try ~call (catch Exception ~'e nil)))

(def id-transform (AffineTransform.))

(defn all-bounds [node]
  {:normal                (maybe (to-rect (.getBounds node)))
   :geometry              (maybe (to-rect (.getGeometryBounds node)))
   :primitive             (maybe (to-rect (.getPrimitiveBounds node)))
   :sensitive             (maybe (to-rect (.getSensitiveBounds node)))
   :transformed           (to-rect (.getTransformedBounds node id-transform))
   :transformed-geometry  (to-rect (.getTransformedGeometryBounds node id-transform))
   :transformed-primitive (to-rect (.getTransformedPrimitiveBounds node id-transform))
   :transformed-sensitive (to-rect (.getTransformedSensitiveBounds node id-transform))})

(defn- transformed-geometry-bounds [node]
  (when node
    (to-rect (.getTransformedGeometryBounds node id-transform))))

(defn- rehearse-bounds-impl [this dom element]
  (let [dom-element (->> element
                     s/ixml->xml
                     (dom/xml->dom-element dom))]
    (when-not (dom/add-to-svg! dom dom-element)
      (throw (ex-info "Failed to add element to DOM" {:element element})))
    (let [gvt  (gvt-node this dom-element)
          _    (when-not gvt
                 (throw (ex-info "Cannot find GVT node for element - try setting the page size manually if it isn't set already"
                                 {:element element})))
          bbox (transformed-geometry-bounds gvt)]
      (dom/remove-from-svg dom dom-element)
      bbox)))

(defrecord BatikContextRecord [bridge gvt dom]
  BatikContext
  (gvt-node [this dom-node]
    (.getGraphicsNode bridge dom-node))
  (gvt-node-by-id [this id]
    (gvt-node this (.getElementById dom id)))
  (rehearse-bounds [this element]
    (rehearse-bounds-impl this dom element)))

(defn context
  ([]
   (context nil))
  ([dom & {:keys [dynamic?]}]
   (let [dom (or dom
                 (-> (SVGDOMImplementation/getDOMImplementation)
                     (.createDocument SVGDOMImplementation/SVG_NAMESPACE_URI "svg" nil)))
         bridge (BridgeContext. (UserAgentAdapter.))]
     (.setDynamic bridge (or dynamic? true))
     (map->BatikContextRecord
      {:dom    dom
       :bridge bridge
       :gvt    (.build (GVTBuilder.) bridge dom)}))))

(defn parse-svg-uri [uri]
  (let [factory (SAXSVGDocumentFactory. "org.apache.xerces.parsers.SAXParser")]
    (.createDocument factory uri)))

(defn parse-svg-string [s]
  (let [factory (SAXSVGDocumentFactory. "org.apache.xerces.parsers.SAXParser")]
    (with-open [in (ByteArrayInputStream. (.getBytes s StandardCharsets/UTF_8))]
      (.createDocument factory "file:///fake.svg" in))))

(defn render-document-to-png [svg-document png]
  (with-open [out-stream (io/output-stream (io/file png))]
    (let [in (TranscoderInput. svg-document)
          out (TranscoderOutput. out-stream)]
      (doto (PNGTranscoder.)
        (.transcode in out)))))

(defn render-uri-to-png [uri png-filename]
  (render-document-to-png (parse-svg-uri uri) png-filename))

(defn bounds [node]
  (to-rect (.getBounds node)))

(defn sensitive-bounds [node]
  (to-rect (.getSensitiveBounds node)))

(def path-segment-types
  {PathIterator/SEG_MOVETO :M
   PathIterator/SEG_LINETO :L
   PathIterator/SEG_QUADTO :Q
   PathIterator/SEG_CUBICTO :C
   PathIterator/SEG_CLOSE :Z})

(defn- path-seq-step [path-iterator arr]
  (let [type (path-segment-types (.currentSegment path-iterator arr))]
    (.next path-iterator)
    (cons
     [type (into [] arr)]
     (when-not (.isDone path-iterator)
       (lazy-seq (path-seq-step path-iterator arr))))))

(defn- path-seq [path]
  (let [it (.getPathIterator path nil)
        arr (double-array 6)]
    (path-seq-step it arr)))

(defn outline [gvt-node] ;;TODO add type annotation
  (let [segments (-> gvt-node .getOutline path-seq)]
    (vec
     (concat
      [:path]
      (map (fn [[type params]]
             (condp = type
               :M (vec (take 2 params))
               :L 
               :Q
               :C
               :Z)) segments)))))


;;use this on the :gvt part of the record
(defprotocol BatikNode
  (info [this]))

(defn- class-name [x] (-> x .getClass .getName (string/replace "org.apache.batik.gvt." "")))

(extend-protocol BatikNode
  nil
  (info [this] nil)
  RootGraphicsNode
  (info [this]
    {:class (class-name this)
     :children (map info (.getChildren this))})
  CompositeGraphicsNode
  (info [this]
    {:class (class-name this)
     :children (map info (.getChildren this))})
  ShapeNode
  (info [this]
    {:class (class-name this)
     :shape (class-name (.getShape this))})
  TextNode
  (info [this]
    {:class (class-name this)
     :text (.getText this)})
  Object
  (info [this] (class-name this)))



(comment
  (let [ctx (batik-context (parse-svg-uri "file:///s:/temp/svg.svg"))
        node (gvt-node-by-id ctx "thick")]
    (-> node .getOutline (.getPathIterator nil))))

(comment
  (render-uri-to-png "file:///s:/temp/svg.svg" "s:/temp/out.png"))

(comment
  (do
    (require '[dali.syntax :as syntax])
    (-> [:page {:width 250 :height 250}
         [:circle {:stroke {:paint :black :width 3}
                   :fill :green} [125 125] 75]]
        syntax/dali->hiccup
        syntax/hiccup->svg-document-string
        parse-svg-string
        (render-document-to-png "s:/temp/out2.png"))))

(comment
  (do
    (def ctx (batik-context (parse-svg-uri "file:///s:/temp/svg.svg") :dynamic? true))
    (rehearse-bounds ctx [:circle [10 20] 5])))
