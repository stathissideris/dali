(ns dali.layout.place
  (:require [dali.layout :as layout]
            [dali.geom :as geom :refer [v+]]
            [dali.layout.utils :refer [place-by-anchor bounds->anchor-point]]
            [dali.utils :as utils]
            [net.cgrand.enlive-html :as en]))

(defmethod layout/layout-nodes :dali/place
  [doc {{:keys [offset relative-to anchor]} :attrs} elements bounds-fn]
  (let [[other-id other-anchor] (if (keyword? relative-to)
                                  [relative-to :center]
                                  relative-to)
        other-element           (first (en/select doc [(utils/to-enlive-id-selector other-id)]))
        _                       (when-not other-element
                                  (throw (ex-info "Cannot find relative-to element"
                                                  {:element-id other-id :document doc})))
        this-element            (first elements)
        anchor                  (or anchor :center)
        offset                  (or offset [0 0])]
    [(place-by-anchor this-element
                      anchor
                      (geom/v+ offset (bounds->anchor-point other-anchor (bounds-fn other-element)))
                      (bounds-fn this-element))]))
