(ns dali)

(def layout-tags
  (atom #{:layout :stack :distribute :align :connect :matrix}))

(defn register-layout-tag [tag]
  (swap! layout-tags conj tag))

(defn dali-tag? [tag]
  (@layout-tags (keyword tag)))

(defn layout-tag? [tag]
  (@layout-tags (keyword tag)))
