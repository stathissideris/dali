(ns dali.style
  (:use [dali.utils]
        [dali.math]
        [dali.core]
        [clojure.walk]))

(derive ::color ::fill)
(derive ::linear-gradient ::fill)
(derive ::radial-gradient ::fill)
(derive ::image-texture ::fill)
(derive ::shape-texture ::fill)

(derive ::linear-gradient ::gradient)
(derive ::radial-gradient ::gradient)

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

;;;;;; fill ;;;;;;;
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

;;;;;; gradients ;;;;;;

(defn linear-gradient
  "Construct a linear gradient with multiple stops. The first two
  parameters define the start and end points of the gradient. The rest
  of the parameters define the stops of the gradient as pairs of
  numbers (between 0 and 1) and colors. Optionally, as a last
  parameter you can pass a keyword defining the cycle method of the
  gradient (:no-cycle, :repeat or :reflect, with :no-cycle being the
  default).

  Example:

  (linear-gradient [10 10]
                   [100 10]
                   0.1 (color :white)
                   0.2 (color :green)
                   0.5 (color :red)
		           0.9 (color :black))"

  [start end & stops]
  {:pre [(>= (count stops) 4)]}
  (let [cycle-method (if (keyword? (last stops)) (last stops) :no-cycle)
        stops (if (keyword? (last stops)) (butlast stops) stops)]
    (when (some #(not (within % [0 1])) (map first (partition 2 stops)))
      (throw (Exception. (str "Not all stops within 0 and 1: " stops))))
    {:type :linear-gradient
     :start start
     :end end
     :stops stops
     :cycle-method cycle-method}))

(defn radial-gradient
  [center radius & stops]
  (let [f (first stops)
        focus (if (or (coll? f) (function? f)) f nil)
        stops (if focus (rest stops) stops)
        cycle-method (if (keyword? (last stops)) (last stops) :no-cycle)
        stops (if (keyword? (last stops)) (butlast stops) stops)]
    (when (some #(not (within % [0 1])) (map first (partition 2 stops)))
      (throw (Exception. (str "Not all stops within 0 and 1: " stops))))
    {:type :radial-gradient
     :center center
     :radius radius
     :focus-point focus
     :stops stops
     :cycle-method cycle-method}))

(declare #^{:dynamic true} backend)
(declare #^{:dynamic true} this)

(defn eval-dynamic-style [the-backend shape style]
  (binding [*ns* (the-ns 'dali.style)
            backend the-backend
            this shape]
    (postwalk (fn eval-dynamic-style-fn [x]
                (if (dynamic-value? x)
                  (let [code (:code x)]                    
                    (eval `(do ~@code)))
                  x)) style)))

;;;;;; fills ;;;;;;

(defn image-texture
  ([img-info]
     (if (nil? (:dimensions img-info))
       (image-texture img-info nil)
       (image-texture
        img-info
        (rectangle [0 0] (:dimensions img-info)))))
  ([img-info anchor]
     {:type :image-texture
      :image img-info
      :dimensions (:dimensions img-info)
      :anchor anchor}))


(def FONT-STYLES #{:normal :italic :oblique})
(def FONT-WEIGHTS #{:normal :bold :bolder :lighter :100 :200 :300
                    :400 :500 :600 :700 :800 :900})

(defn font
  [& spec]
  {:pre [(every? #{:family :size :style :weight} (take-nth 2 spec))]}
  (let [m (apply hash-map spec)]
    (if-let [style (:style m)]
      (when-not (FONT-STYLES style)
        (throw (Exception. (str "Font style " style " not recognised")))))
    (if-let [weight (:weight m)]
      (when-not (FONT-WEIGHTS weight)
        (throw (Exception. (str "Font weight " weight " not recognised")))))
    m))
