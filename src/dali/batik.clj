(ns dali.batik
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [dali
             [dom :as dom]
             [syntax :as s]]
            [dali.utils :as utils])
  (:import [java.awt RenderingHints Rectangle]
           [java.awt.geom AffineTransform PathIterator]
           [javax.imageio ImageIO]
           [java.io File]
           java.io.ByteArrayInputStream
           java.nio.charset.StandardCharsets
           [org.apache.batik.anim.dom SAXSVGDocumentFactory SVGDOMImplementation]
           [org.apache.batik.bridge BridgeContext GVTBuilder UserAgentAdapter]
           [org.apache.batik.bridge TextNode]
           [org.apache.batik.gvt RootGraphicsNode CompositeGraphicsNode ShapeNode]
           [org.apache.batik.gvt.renderer ConcreteImageRendererFactory]
           [org.apache.batik.transcoder TranscoderInput TranscoderOutput]
           org.apache.batik.transcoder.image.PNGTranscoder))

;;Batik - calculating bounds of cubic spline
;;http://stackoverflow.com/questions/10610355/batik-calculating-bounds-of-cubic-spline?rq=1

;;Wrong values of bounding box for text element using Batik
;;http://stackoverflow.com/questions/12166280/wrong-values-of-bounding-box-for-text-element-using-batik

(defprotocol BatikContext
  (gvt-node [this dom-node])
  (gvt-node-by-id [this id])
  (replace-node! [this dali-path new-node document])
  (append-node! [this new-node document])
  (get-bounds [this element]))

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

(defn- get-parents [node]
  (->> node
       (iterate #(.getParent %))
       (drop 1)
       (take-while some?)
       reverse))

(defn- transformed-bounds [gvt]
  (let [parents    (get-parents gvt)
        transforms (remove nil? (map #(when-let [t (.getTransform %)] (.clone t)) parents))
        tr         (reduce (fn [a b] (.concatenate a b) a) transforms)]
    (to-rect (.getTransformedGeometryBounds gvt tr))))

(defn- get-bounds-impl [this dom element]
  (let [dom-element (try (dom/get-node dom (-> element :attrs :dali/path))
                         (catch Exception e
                           (throw (ex-info "Could not get DOM element for dali node" {:dali-node element
                                                                                      :dom (dom/->xml dom)}))))
        _           (when-not dom-element
                      (throw (ex-info "DOM element for dali node is nil" {:dali-node element
                                                                          :dom (dom/->xml dom)})))
        gvt         (gvt-node this dom-element)
        _           (when-not gvt
                      (throw (ex-info "Cannot find GVT node for dali node"
                                      {:dali-node element
                                       :dom-element (when dom-element (dom/->xml dom-element))})))]
    (transformed-bounds gvt)))

(defn- dali->dom [dali-node document dom]
  (->> dali-node
       (s/ixml-fragment->xml-node document)
       (dom/xml->dom-element dom)))

(defrecord BatikContextRecord [bridge gvt dom]
  BatikContext
  (gvt-node [this dom-node]
    (.getGraphicsNode bridge dom-node))
  (gvt-node-by-id [this id]
    (gvt-node this (.getElementById dom id)))
  (replace-node! [this dali-path new-node document]
    (dom/replace-node! dom dali-path (dali->dom new-node document dom)))
  (append-node! [this new-node document]
    (dom/add-to-svg! dom (dali->dom new-node document dom)))
  (get-bounds [this element]
    (get-bounds-impl this dom element)))

(defn context
  ([doc]
   (context doc nil))
  ([doc dom & {:keys [dynamic?]}]
   (let [dom (or dom
                 (-> (SVGDOMImplementation/getDOMImplementation)
                     (.createDocument SVGDOMImplementation/SVG_NAMESPACE_URI "svg" nil)))
         bridge (BridgeContext. (UserAgentAdapter.))]
     (.setDynamic bridge (or dynamic? true))
     (->> doc
          s/ixml->xml
          :content
          (map #(dom/xml->dom-element dom %))
          (map #(dom/add-to-svg! dom %))
          doall)
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

(defn high-quality-png-transcoder []
  (proxy [PNGTranscoder] []
    (createRenderer []
      (let [add-hint (fn [hints k v] (.add hints (RenderingHints. k v)))
            renderer (proxy-super createRenderer)
            ;;hints    (.getRenderingHints renderer)
            hints (RenderingHints. RenderingHints/KEY_ALPHA_INTERPOLATION RenderingHints/VALUE_ALPHA_INTERPOLATION_QUALITY)
            ]
        (doto hints
          (add-hint RenderingHints/KEY_ALPHA_INTERPOLATION RenderingHints/VALUE_ALPHA_INTERPOLATION_QUALITY)
          (add-hint RenderingHints/KEY_INTERPOLATION       RenderingHints/VALUE_INTERPOLATION_BICUBIC)
          (add-hint RenderingHints/KEY_ANTIALIASING        RenderingHints/VALUE_ANTIALIAS_ON)
          (add-hint RenderingHints/KEY_COLOR_RENDERING     RenderingHints/VALUE_COLOR_RENDER_QUALITY)
          (add-hint RenderingHints/KEY_DITHERING           RenderingHints/VALUE_DITHER_DISABLE)
          (add-hint RenderingHints/KEY_RENDERING           RenderingHints/VALUE_RENDER_QUALITY)
          (add-hint RenderingHints/KEY_STROKE_CONTROL      RenderingHints/VALUE_STROKE_PURE)
          (add-hint RenderingHints/KEY_FRACTIONALMETRICS   RenderingHints/VALUE_FRACTIONALMETRICS_ON)
          (add-hint RenderingHints/KEY_TEXT_ANTIALIASING   RenderingHints/VALUE_TEXT_ANTIALIAS_OFF))
        (.setRenderingHints renderer hints)
        renderer))))

(defn render-document-to-png [svg-document png]
  (with-open [out-stream (io/output-stream (io/file png))]
    (let [in (TranscoderInput. svg-document)
          out (TranscoderOutput. out-stream)]
      (doto (high-quality-png-transcoder)
        (.transcode in out)))))

(comment ;;alternative implementation taken from dali, half broken
 (defn render-document-to-png [svg-document png]
   (let [renderer (-> (ConcreteImageRendererFactory.) .createStaticImageRenderer)
         builder  (GVTBuilder.)
         ctx      (BridgeContext. (UserAgentAdapter.))]
     (.setDynamicState ctx BridgeContext/STATIC)
     (let [root-node (.build builder ctx svg-document)
           w         (-> ctx .getDocumentSize .getWidth)
           h         (-> ctx .getDocumentSize .getHeight)]
       (.setTree renderer root-node)
       (doto renderer
         (.updateOffScreen w h)
         (.setTree root-node)
         (.repaint (Rectangle. 0 0 w h)))
       (ImageIO/write (.getOffScreen renderer) "png" (File. png))))))

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
    (get-bounds ctx [:circle [10 20] 5])))
