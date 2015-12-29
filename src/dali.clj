(ns dali)

(def layout-tags
  (atom #{:dali/layout
          :dali/stack
          :dali/distribute
          :dali/align
          :dali/place
          :dali/matrix
          :dali/connect
          :dali/surround}))

(defn register-layout-tag [tag]
  (swap! layout-tags conj tag))

(defn layout-tag? [tag]
  (@layout-tags (keyword tag)))

(defn dali-tag? [tag]
  (let [tag (keyword tag)]
    (layout-tag? tag)))
