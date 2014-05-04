(ns cljx.dali.batik
  (:require [clojure.java.io :as io])
  (:import [org.apache.batik.transcoder.image PNGTranscoder]
           [org.apache.batik.transcoder
            TranscoderInput TranscoderOutput]
           [org.apache.batik.dom.svg SAXSVGDocumentFactory]
           [org.apache.batik.bridge UserAgentAdapter BridgeContext GVTBuilder]
           [org.apache.batik.bridge.svg12 SVG12BridgeContext]))

(defprotocol BatikContext
  (gvt-node [this dom-node])
  (gvt-node-by-id [this id]))

(defrecord BatikContextRecord [bridge gvt dom]
  BatikContext
  (gvt-node [this dom-node]
    (.getGraphicsNode bridge dom-node))
  (gvt-node-by-id [this id]
    (gvt-node this (.getElementById dom id))))

(defn batik-context [dom & {:keys [dynamic?]}]
  (let [bridge (SVG12BridgeContext. (UserAgentAdapter.))]
    (.setDynamic bridge (or dynamic? true))
    (map->BatikContextRecord
     {:dom dom
      :bridge bridge
      :gvt (.build (GVTBuilder.) bridge dom)})))

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
  (let [ctx (batik-context (parse-svg "file:///s:/temp/svg.svg"))]
    (gvt-node-by-id ctx "thick")))

(comment
  (svg-to-png "file:///s:/temp/svg.svg" "s:/temp/out.png"))
