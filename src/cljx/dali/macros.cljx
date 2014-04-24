(ns dali.macros)

;used by core
(defmacro defshape [shape & geometry-components]
  `(defn ~shape
     ([~@geometry-components]
        {:type ~(keyword (str shape))
         :geometry ~(zipmap (map (fn [x] (keyword (str x)))
                                 geometry-components)
                            geometry-components)})
     ([~'attr-map ~@geometry-components]
        (merge
         (~'parse-attr-map ~'attr-map)
         (~shape ~@geometry-components)))))

(defmacro dynamic [& body]
  {:type :dynamic-value
   :code `'~body})

;used by backend
(defmacro delegate-op-to-backend [op & shape-types]
  `(do ~@(map
          (fn [t]
            `(defmethod ~op ~t
               [~'backend ~'shape]
               (~(symbol (str op "-" (name t))) ~'backend ~'shape)))
          shape-types)))

;used by backend.browser-svg
(defmacro xml
  "Alias for hiccup.core/html"
  [& args] `(html ~@args))

;used by backend.browser-svg
(defmacro named-map
  "Converts (named-map x x) to {:x x, :y y}"
  [& args]
  (let [ks (map (comp keyword name) args)]
    (apply hash-map (interleave ks args))))
