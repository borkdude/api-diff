(ns example)

;; privates should be ignored
(defn- private-fn [])
(def ^:private private-defa 3)
(defn becomes-private [])

(defn becomes-nodoc [])

(def x 1)
(def y 2)
(def z 3)

(defn ^:no-doc nodoc-changes [x] x)

(defn ^:skip-wiki skip-wiki [])

(defn arity-change-and-becomes-skip-wiki [x y])
