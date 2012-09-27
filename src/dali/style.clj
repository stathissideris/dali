(ns dali.style)

(defn color
  ([r g b]
     {:type :color
      :colorspace :rgb
      :r r, :g g, :b b})
  ([r g b a]
     {:type :color
      :colorspace :rgba
      :r r, :g g, :b b, :a a}))
