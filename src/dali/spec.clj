(ns dali.spec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))

(defn blankable [spec]
  (s/or :defined spec
        ::blank #{:_}))

(let [op (fn [op params-spec]
           (s/cat :op #{op} :params params-spec))]

  (s/def ::transform
    (s/+
     (s/alt
      :matrix-op (op :matrix (s/tuple number? number? number? number? number? number?))
      :translate-op (op :translate ::point)
      :scale-op (op :scale (s/or :factor ::point, :factor number?))
      :rotate-op (op :rotate (s/or :angle number?, :center (s/cat
                                                               :angle number?
                                                               :x number?
                                                               :y number?)))
      :skew-x-op (op :skew-x number?)
      :skew-y-op (op :skew-y number?)))))

(s/def ::attr-map
  (s/and (s/keys :opt-un [::transform])
         (s/map-of (s/or :k keyword? :k string?) ::s/any)))

(s/def ::point (s/spec (s/cat :x number? :y number?)))

(s/def ::dimensions (s/spec (s/cat :w number? :h number?)))

(defn- boolean? [x] (instance? Boolean x))
(s/def ::boolean (s/with-gen boolean? #(gen/boolean)))

(s/def ::path-arc-spec
  (s/cat :pos1 ::point
         :x-axis-rotation number?
         :large-arc-flag ::boolean
         :sweep-flag ::boolean
         :pos2 ::point))

(let [op (fn [long short params-spec]
           (s/cat :op (s/and #{long short}
                             (s/conformer (constantly long))) ;;coerce to long form
                  :params params-spec))]
  (s/def ::path-operation
    (s/alt
     :move-to-op (op :move-to :M ::point)
     :move-by-op (op :move-by :m ::point)
     :line-to-op (op :line-to :L ::point)
     :line-by-op (op :line-by :l ::point)
     :horizontal-to-op (op :horizontal-to :H number?)
     :horizontal-by-op (op :horizontal-by :h number?)
     :vertical-to-op (op :vertical-to :V number?)
     :vertical-by-op (op :vertical-by :v number?)
     :cubic-to-op (op :cubic-to :C (s/cat :p1 ::point :p2 ::point :p3 ::point))
     :cubic-by-op (op :cubic-by :c (s/cat :p1 ::point :p2 ::point :p3 ::point))
     :symmetrical-to-op (op :symmetrical-to :S (s/cat :p1 ::point :p2 ::point))
     :symmerical-by-op (op :symmetrical-by :s (s/cat :p1 ::point :p2 ::point))
     :quad-to-op (op :quad-to :Q (s/cat :p1 ::point :p2 ::point))
     :quad-by-op (op :quad-by :q (s/cat :p1 ::point :p2 ::point))
     :arc-to-op (op :arc-to :A ::path-arc-spec)
     :arc-by-op (op :arc-by :a ::path-arc-spec)
     :close-op (s/cat :op #{:close :Z :z}))))

(s/def ::path-tag
  (s/cat :tag #{:path}
         :attrs (s/? ::attr-map)
         :operations (s/* ::path-operation)))

(s/def ::line-tag
  (s/cat :tag #{:line}
         :attrs (s/? ::attr-map)
         :coords
         (s/? ;;both or none
          (s/cat
           :start ::point
           :end ::point))))

(s/def ::polyline-tag
  (s/cat :tag #{:polyline}
         :attrs (s/? ::attr-map)
         :points (s/* ::point)))

(s/def ::polygon-tag
  (s/cat :tag #{:polygon}
         :attrs (s/? ::attr-map)
         :points (s/* ::point)))

(s/def ::circle-tag
  (s/cat :tag #{:circle}
         :attrs (s/? ::attr-map)
         :coords
         (s/? ;;both or none
          (s/cat
           :center (blankable ::point)
           :radius number?))))

(s/def ::rect-tag
  (s/cat :tag #{:rect}
         :attrs (s/? ::attr-map)
         :position (blankable ::point)
         :dimensions ::dimensions
         :rounding (s/? (s/or :uniform number?
                              :distinct ::dimensions)))) ;;TODO not sure about :distinct

(s/def ::generic-tag
  (s/cat :tag keyword?
         :attrs (s/? ::attr-map)
         :content (s/* (s/or :tag ::tag, :text string?))))

(s/def ::tag (s/or
              :path-tag ::path-tag
              :rect-tag ::rect-tag
              :line-tag ::line-tag
              :polyline-tag ::polyline-tag
              :circle-tag ::circle-tag
              :generic-tag ::generic-tag))

(s/def ::document
  (s/cat :tag #{:dali/page}
         :attrs (s/? ::attr-map)
         :content (s/* (s/or :tag ::tag, :text string?))))

(defn conform [document]
  (s/conform ::document document))

(defn explain [document]
  (s/explain ::document document))
