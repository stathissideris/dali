(ns user)

(defn load-dev []
  (require 'dev)
  (in-ns 'dev))

(def dev load-dev)

