(ns dali.utils)

(defn match-non-match
  "Given a predicate and 2 values, it returns a vector with the
  matching value first, and the non-matching value second. Returns nil
  if none of the values match the predicate."
  [pred x1 x2]
  (cond (pred x1) [x1 x2]
        (pred x2) [x2 x1]
        :else nil))
