(ns dali.math)

#?(:cljs
   (def PI (.-PI js/Math)))

#?(:clj
   (defn abs [x]
     (java.lang.Math/abs x))

   :cljs
   (defn abs [x]
     (.abs js/Math x)))
  
(defn within
  "Tests whether x is within the range of a and b. a can be less or
  more than b. The optional error parameter extends both sides of the
  range by error."
  ([x [a b]]
     (or (and (>= x a) (<= x b))
         (and (>= x b) (<= x a))))
  ([x [a b] error]
     (or (and (>= x (- a error)) (<= x (+ b error)))
         (and (>= x (- b error)) (<= x (+ a error))))))

#?(:clj
   (defn radians->degrees [x]
     (java.lang.Math/toDegrees x))

   :cljs
   (defn radians->degrees [x]
     (/ (* x 180) PI)))

#?(:clj
   (defn degrees->radians [x]
     (java.lang.Math/toRadians x))
   
   :cljs
   (defn degrees->radians [x]
     (* (/ x 180) PI)))

#?(:clj
   (defn sin
     "The sine of angle x (in degrees)"
     [x]
     (java.lang.Math/sin (degrees->radians x)))

   :cljs
   (defn sin
     "The sine of angle x (in degrees)"
     [x]
     (.sin js/Math (degrees->radians x))))

#?(:clj
   (defn cos
     "The cosine of angle x (in degrees)"
     [x]
     (java.lang.Math/cos (degrees->radians x)))

   :cljs
   (defn cos
     "The cosine of angle x (in degrees)"
     [x]
     (.cos js/Math (degrees->radians x))))

#?(:clj
   (defn atan2 [x y]
     (java.lang.Math/atan2 x y))
   
   :cljs
   (defn atan2 [x y]
     (.atan2 js/Math x y)))

#?(:clj
   (defn sqrt
     [x]
     (java.lang.Math/sqrt x))

   :cljs
   (defn sqrt
     [x]
     (.sqrt js/Math x)))

(defn polar-angle
  [[x y]]
  (radians->degrees (atan2 y x)))

(defn cartesian-to-polar
  [[x y]]
  [(sqrt (+ (* x x) (* y y))) (polar-angle [x y])])

(defn polar-to-cartesian
  [[r theta]]
  [(* r (cos theta))
   (* r (sin theta))])

(defn minus
  "Makes all the numbers of a seq negative and returns it as a vector"
  [x]
  (vec (map - x)))

(defn determinant-2x2
  "The determinant of a 2x2 matrix."
  [[[a b] [c d]]]
  (- (* a d)
     (* b c)))

(defn solve-2x2-system
  "Solves a 2x2 system of equations. cx is the coeffient of x, cy is
  the coefficient of y, and the con variables are the constant
  values."
  [[cx1 cy1 con1]
   [cx2 cy2 con2]]
  (let [denom (determinant-2x2 [[cx1 cy1]
                            [cx2 cy2]])]
    (when (not (zero? denom))
      {:x (/ (determinant-2x2 [[con1 cy1]
                               [con2 cy2]])
             denom)
       :y (/ (determinant-2x2 [[cx1 con1]
                               [cx2 con2]])
             denom)})))

(defn linear-equation-at-x
  "Calculates the y value according to a linear equation based on the
  passed x value. The equation information is passed as a map
  with :slope and :offset keys."
  [{:keys [slope offset]} x]
  (-> x
      (* slope)
      (+ offset)))
