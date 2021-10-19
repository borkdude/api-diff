(ns example)

;; privates should be ignored
(defn- private-fn [x y])
(def ^:private private-defb 72)

;; this was public, should appear as removed
(defn- becomes-private [])

(def x 1)
;; (def y 2)
(def ^:deprecated z 3)
