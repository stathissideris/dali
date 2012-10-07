(ns dali.utils)

(defn match-non-match
  "Given a predicate and 2 values, it returns a vector with the
  matching value first, and the non-matching value second. Returns nil
  if none of the values match the predicate."
  [pred x1 x2]
  (cond (pred x1) [x1 x2]
        (pred x2) [x2 x1]
        :else nil))

(defn deep-merge
  "Merges map b into map a recursively. If both values for a certain
  key are maps, they are deeply merged themselves. If not, the value
  from map b is used unless it's nil."
  [a b]
  (cond
    (nil? a) b
    (nil? b) a
    :else
    (let [all-keys (into #{} (concat (keys a) (keys b)))]
      (into
       {}
       (map
        (fn [key]
          (let [va (a key ::none), vb (b key ::none)]
            (cond (and (map? va) (map? vb)) [key (deep-merge va vb)]
                  (= ::none vb) [key va]
                  (= ::none va) [key vb]
                  :else [key vb]))) all-keys)))))
