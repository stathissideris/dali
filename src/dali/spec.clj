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
      :transform-op (op :matrix (s/tuple number? number? number? number? number? number?))
      :transform-op (op :translate ::point)
      :transform-op (op :scale (s/or :factor ::point, :factor number?))
      :transform-op (op :rotate (s/or :angle number?, :center (s/cat
                                                               :angle number?
                                                               :x number?
                                                               :y number?)))
      :transform-op (op :skew-x number?)
      :transform-op (op :skew-y number?)))))

(s/def ::simple-attr-map
  (s/map-of (s/or :k keyword? :k string?) ::s/any))

(s/def ::attr-map
  (fn [{:keys [transform] :as m}]
    (and (s/valid? ::simple-attr-map (dissoc m :transform))
         (or (not transform)
             (s/valid? ::transform transform)))))

(s/def ::point (s/spec (s/cat :x number? :y number?)))

(s/def ::dimensions (s/spec (s/cat :w number? :h number?)))

(defn- boolean? [x] (instance? Boolean x))

(s/def ::path-arc-spec
  (s/cat :pos1 ::point
         :x-axis-rotation number?
         :large-arc-flag boolean?
         :sweep-flag boolean?
         :pos2 ::point))

(let [op (fn [long short params-spec]
           (s/cat :op #{long short}
                  :params params-spec))]
  (s/def ::path-operation
    (s/alt
     :path-op (op :move-to :M ::point)
     :path-op (op :move-by :m ::point)
     :path-op (op :line-to :L ::point)
     :path-op (op :line-by :l ::point)
     :path-op (op :horizontal-to :H number?)
     :path-op (op :horizontal-by :h number?)
     :path-op (op :vertical-to :V number?)
     :path-op (op :vertical-by :v number?)
     :path-op (op :cubic-to :C (s/cat :p1 ::point :p2 ::point :p3 ::point))
     :path-op (op :cubic-by :c (s/cat :p1 ::point :p2 ::point :p3 ::point))
     :path-op (op :symmetrical-to :S (s/cat :p1 ::point :p2 ::point))
     :path-op (op :symmetrical-by :s (s/cat :p1 ::point :p2 ::point))
     :path-op (op :quad-to :Q (s/cat :p1 ::point :p2 ::point))
     :path-op (op :quad-by :q (s/cat :p1 ::point :p2 ::point))
     :path-op (op :arc-to :A ::path-arc-spec)
     :path-op (op :arc-by :a ::path-arc-spec)
     :path-op (s/cat :op #{:close :Z :z}))))

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
              :tag ::path-tag
              :tag ::rect-tag
              :tag ::line-tag
              :tag ::polyline-tag
              :tag ::circle-tag
              :tag ::generic-tag))

(s/def ::document ::tag)

(defn conform [document]
  (s/conform ::document document))

(defn explain [document]
  (s/explain ::document document))
