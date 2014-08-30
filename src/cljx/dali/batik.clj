(ns dali.batik
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [dali.syntax :as s]
            [dali.dom :as dom])
  (:import [java.nio.charset StandardCharsets]
           [java.io ByteArrayInputStream]
           [java.awt.geom PathIterator]
           [org.apache.batik.transcoder.image PNGTranscoder]
           [org.apache.batik.transcoder
            TranscoderInput TranscoderOutput]
           [org.apache.batik.dom.svg SAXSVGDocumentFactory SVGDOMImplementation]
           [org.apache.batik.bridge UserAgentAdapter BridgeContext GVTBuilder DocumentLoader]))

;;Batik - calculating bounds of cubic spline
;;http://stackoverflow.com/questions/10610355/batik-calculating-bounds-of-cubic-spline?rq=1

;;Wrong values of bounding box for text element using Batik
;;http://stackoverflow.com/questions/12166280/wrong-values-of-bounding-box-for-text-element-using-batik

(defprotocol BatikContext
  (gvt-node [this dom-node])
  (gvt-node-by-id [this id])
  (rehearse-bounds [this element]))

(defn- to-rect [rect]
  [:rect
   [(.getX rect)
    (.getY rect)]
   [(.getWidth rect)
    (.getHeight rect)]])

(def ^:private rehearse-bounds-impl
  (memoize
   (fn [this dom element]
     (let [element (->> element
                        s/dali->hiccup
                        (dom/hiccup->element dom))]
       (dom/add-to-svg dom element)
       (let [bbox (to-rect (-> element .getBBox))]
         (dom/remove-from-svg dom element)
         bbox)))))

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
        {:dom dom
         :bridge bridge
         :gvt (.build (GVTBuilder.) bridge dom)}))))

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

(defmacro maybe [call]
  `(try ~call (catch Exception ~'e nil)))

(defn all-bounds [node]
  {:normal (maybe (to-rect (.getBounds node)))
   :geometry (maybe (to-rect (.getGeometryBounds node)))
   :primitive (maybe (to-rect (.getPrimitiveBounds node)))
   :sensitive (maybe (to-rect (.getSensitiveBounds node)))
   :transformed (maybe (to-rect (.getTransformedBounds node)))
   :transformed-geometry (maybe (to-rect (.getTransformedGeometryBounds node)))
   :transformed-primitive (maybe (to-rect (.getTransformedPrimitiveBounds node)))
   :transformed-sensitive (maybe (to-rect (.getTransformedSensitiveBounds node)))})

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
