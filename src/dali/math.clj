(ns dali.math)

(defn abs [x]
  (java.lang.Math/abs x))

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
