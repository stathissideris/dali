(ns dali.style
  (:use [dali.utils]
        [clojure.walk]))

(defn color
  ([r g b]
     {:type :color
      :colorspace :rgb
      :r r, :g g, :b b})
  ([r g b a]
     {:type :color
      :colorspace :rgba
      :r r, :g g, :b b, :a a}))

(defn color? [x]
  (= :color (:type x)))

;;;;;; stroke ;;;;;;;
;;
;; stroke is a map containing the following keys:
;;
;;  :width        a number
;;  :cap          one of :round :butt :square
;;  :join         one of :round :miter :bevel
;;  :miter-limit  a number
;;  :dash         a vector of numbers
;;  :dash-phase   a number
;;

(let [stroke-directives #{:width :cap :join :miter-limit :dash :dash-phase}
      cap-options #{:round :butt :square}
      join-options #{:round :miter :bevel}]
  (defn validate-stroke-spec [spec]
    (and (every? stroke-directives (map first spec))
         (every? (fn [[dir v]]
                   (condp = dir
                     :width (num-or-fn? v)
                     :cap (or (function? v) (cap-options v))
                     :join (or (function? v) (join-options v))
                     :miter-limit (num-or-fn? v)
                     :dash (or (function? v) (and (coll? v) (every? num-or-fn? v)))
                     :dash-phase (num-or-fn? v))) spec)))
  
  (defn stroke-spec
    "This function validates the stroke spec and returns it. If the
    stroke is not valid, it throws an Exception. You can write a
    stroke as a map literal in order to avoid the cost of validation."
    [& spec]
    (let [spec-map (apply hash-map spec)]
      (if-not (validate-stroke-spec spec-map)
        (throw (Exception. (str "Stroke spec " spec " is not valid.")))
        spec-map))))

;;;;;; stroke ;;;;;;;
;;
;; fill is a map containing the following keys:
;;
;; :color <A COLOR>
;;

;;TODO expand this
(let [fill-directives #{:color}]
  (defn validate-fill-spec [spec]
    (and (every? fill-directives (map first spec))
         (every? (fn [[dir v]]
                   (condp = dir
                     :color (or (function? v) (color? v)))) spec)))
  
  (defn fill-spec
    "This function validates the fill spec and returns it. If the fill
    is not valid, it throws an Exception. You can write a fill as a
    map literal in order to avoid the cost of validation."
    [& spec]
    (let [spec-map (apply hash-map spec)]
      (if-not (validate-fill-spec spec-map)
        (throw (Exception. (str "Fill spec " spec " is not valid.")))
        spec-map))))

(defn eval-dynamic-style [shape style]
  (postwalk (fn [x] (if (function? x) (x shape) x)) style))
