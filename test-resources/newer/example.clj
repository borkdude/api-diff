(ns example)

;; privates should be ignored
(defn- private-fn [x y])
(def ^:private private-defb 72)

;; this was public, should appear as removed
(defn- becomes-private [])

;; this was not marked with no-doc in older
(defn ^:no-doc becomes-nodoc [])

(def x 1)
;; (def y 2)
(def ^:deprecated z 3)

(defn ^:no-doc nodoc-changes [x y] (+ x y))
