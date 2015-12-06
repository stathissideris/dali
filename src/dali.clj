(ns dali)

(def layout-tags
  (atom #{:dali/layout :dali/stack :dali/distribute :dali/align :dali/connect :dali/matrix}))

(defn register-layout-tag [tag]
  (swap! layout-tags conj tag))

(defn dali-tag? [tag]
  (@layout-tags (keyword tag)))

(defn layout-tag? [tag]
  (@layout-tags (keyword tag)))
