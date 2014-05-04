(ns cljx.dali.batik
  (:require [clojure.java.io :as io])
  (:import [org.apache.batik.transcoder.image PNGTranscoder]
           [org.apache.batik.transcoder
            TranscoderInput TranscoderOutput]
           [org.apache.batik.dom.svg SAXSVGDocumentFactory]
           [org.apache.batik.bridge UserAgentAdapter BridgeContext GVTBuilder]
           [org.apache.batik.bridge.svg12 SVG12BridgeContext]))

(defn- parse-svg [uri]
  (let [factory (SAXSVGDocumentFactory. "org.apache.xerces.parsers.SAXParser")]
    (.createDocument factory uri)))

(defn svg-to-png [svg png]
  (with-open [out-stream (io/output-stream (io/file png))]
    (let [document (parse-svg svg)
          in (TranscoderInput. document)
          out (TranscoderOutput. out-stream)]
      (doto (PNGTranscoder.)
        (.transcode in out)))))

(defn gvt-tree [document & {:keys [dynamic?]}]
  (let [context (SVG12BridgeContext. (UserAgentAdapter.))]
    (.setDynamic context (or dynamic? true))
    {:context context
     :root (.build (GVTBuilder.) context document)}))

(defn to-gvt-node [ctx dom-node]
  (.getGraphicsNode ctx dom-node))

(defn to-rect [rect]
  [:rect
   [(.x rect)
    (.y rect)]
   [(.width rect)
    (.height rect)]])

(defn bounds [node]
  (to-rect (.getBounds node)))

(defn sensitive-bounds [node]
  (to-rect (.getSensitiveBounds node)))

(defn transformed-bounds [node]
  (to-rect (.getTransformedBounds node)))

(defn visual-bounds [node]
  (to-rect (.getTransformedSensitiveBounds node)))

(defmacro maybe [call]
  `(try ~call (catch Exception ~'e nil)))

(defn all-bounds [node]
  {:normal (maybe (to-rect (.getBounds node)))
   :geometry (maybe (to-rect (.getGeometryBounds node)))
   :primitive (maybe (to-rect (.getPrimitiveBounds node)))
   :sensitive (maybe (to-rect (.getSensitiveBounds node)))
   :transformed (maybe (transformed-bounds node))
   :transformed-geometry (maybe (to-rect (.getTransformedGeometryBounds node)))
   :transformed-primitive (maybe (to-rect (.getTransformedPrimitiveBounds node)))
   :visual (maybe (visual-bounds node))})

(comment
  (let [dom (parse-svg "file:///s:/temp/svg.svg")
        {:keys [root context]} (gvt-tree dom)
        thick (.getElementById dom "thick")]
    (to-gvt-node context thick)))

(comment
  (svg-to-png "file:///s:/temp/svg.svg" "s:/temp/out.png")
  )
