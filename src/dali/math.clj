(ns dali.math)

(defn abs [x]
  (java.lang.Math/abs x))

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

(defn radians->degrees [x]
  (java.lang.Math/toDegrees x))

(defn degrees->radians [x]
  (java.lang.Math/toRadians x))

(defn sin
  "The sine of angle x (in degrees)"
  [x]
  (java.lang.Math/sin (degrees->radians x)))

(defn cos
  "The cosine of angle x (in degrees)"
  [x]
  (java.lang.Math/cos (degrees->radians x)))

(defn polar-angle
  [x y]
  (radians->degrees (java.lang.Math/atan2 x y)))

(defn sqrt
  [x]
  (java.lang.Math/sqrt x))

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
