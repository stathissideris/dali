(ns dali.geom)

(defn translate-point [[x y] [dx dy]]
  [(+ x dx) (+ y dy)])
