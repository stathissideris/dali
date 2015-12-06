(ns dali.utils
  (:require [clojure.zip :as zip]))

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

(defn function? [x]
  (or (fn? x)
      (instance? clojure.lang.MultiFn x)))

(defn num-or-fn? [x]
  (or (number? x)
      (function? x)))

(defn coll-of-nums-or-fns? [size v]
  (and (coll? v)
       (= size (count v))
       (every? num-or-fn? v)))

(defn exception [& msg]
  (Exception. (apply str msg)))

(defmacro assert-req [x]
  `(when (nil? ~x) (throw (dali.utils/exception "Required var '" ~(str x) "' is nil"))))

(defmacro prn-names [& args]
  `(do
     ~@(for [x args]
         `(do
            (print ~(str x " = "))
            (prn ~x)))))

;;TODO fix, does not play well with tools.namespace
;; #?(:clj
;;    (defn exception [& msg]
;;      (Exception. (apply str msg)))
;;    
;;    :cljs
;;    (defn exception [& msg]
;;      (js/Error. (apply str msg))))

(defn ixml-zipper [document]
  (zip/zipper #(not-empty (:content %))
              :content
              #(assoc %1 :content (vec %2))
              document))

(defn generic-zipper
  "Walks vectors, lists, maps, and maps' keys and values
  individually. Take care not to replace a keypair with a single
  value (will throw an exception)."
  [x]
  (zip/zipper
   (some-fn sequential? map?)
   seq
   (fn [node children]
     (cond (vector? node) (vec children)
           (seq? node) (seq children)
           (map? node) (into {} children)))
   x))

(defn transform-zipper [zipper replace-fn]
  (loop [zipper zipper]
    (if (zip/end? zipper)
      (zip/root zipper)
      (recur (zip/next (zip/replace
                        zipper
                        (replace-fn zipper)))))))

(defn zipper-last [zipper]
  (loop [zipper zipper]
    (if (zip/end? (zip/next zipper)) zipper (recur (zip/next zipper)))))

(defn transform-zipper-backwards
  ([z replace-fn]
   (loop [z z]
     (let [p (zip/prev (zip/replace z (replace-fn z)))]
       (if (nil? p) ;;we've hit top, not previous node
         (zip/node z)
         (recur p))))))

(defn dump-zipper
  ([z]
   (dump-zipper z zip/next))
  ([z next-fn]
   (loop [z z]
     (when (and (not (nil? z)) (not (zip/end? z)))
       (prn (zip/node z))
       (recur (next-fn z))))))

(defn safe-update-in [m [k & ks] f & args]
  (if-not (apply get-in m [(cons k ks)])
    m (apply update-in m (cons k ks) f args)))

(defn to-enlive-class-selector [x]
  (->> x name (str ".") keyword))

(defn to-enlive-id-selector [x]
  (->> x name (str "#") keyword))

(defn to-iri-id [x]
  (->> x name (str "#")))

(defn keyword-ns [k]
  (->> k str (re-find #":(.+?)/") second))

(defn keyword-name [k]
  (-> k str (subs 1)))

(defn keyword-concat [& args]
  (->> args (map name) (apply str) keyword))
