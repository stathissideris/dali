(ns dali.geom
  (:require [dali.math :as math]))

(defn v+
  "Add two vectors."
  [[x1 y1] [x2 y2]]
  [(+ x1 x2) (+ y1 y2)])

(defn v-
  "Subtract the second vector from the first or negate a
  vector (single arity)."
  ([[x y]]
   [(- x) (- y)])
  ([[x1 y1] [x2 y2]]
   [(- x1 x2) (- y1 y2)]))

(defn v-scale
  "Scale vector."
  [[x y] a]
  [(* x a) (* y a)])

(defn v-half
  "Scale vector to half its magnitude."
  [v]
  (v-scale v 0.5))

(defn distance-squared
  [[x1 y1] [x2 y2]]
  (+ (* (- x1 x2) (- x1 x2))
     (* (- y1 y2) (- y1 y2))))

(defn distance
  [[x1 y1] [x2 y2]]
  (math/sqrt
   (+ (* (- x1 x2) (- x1 x2))
      (* (- y1 y2) (- y1 y2)))))

(defn slope
  [[x1 y1] [x2 y2]]
  (/ (- y2 y1) (- x2 x1)))

(defn angle
  [[x1 y1] [x2 y2]]
  (math/polar-angle [(- x2 x1) (- y2 y1)]))

(defn ->svg-angle
  [x]
  (- x))

(defn ->standard-angle
  [x]
  (- x))
