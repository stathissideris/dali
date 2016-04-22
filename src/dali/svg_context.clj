(ns dali.svg-context)

(defprotocol SVGContext
  (get-bounds [this doc path])
  (get-relative-bounds [this doc path]))
