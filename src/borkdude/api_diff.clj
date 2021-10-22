(ns borkdude.api-diff
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.deps.alpha :as tda]
   [clojure.tools.deps.alpha.util.maven :as mvn]
   [flatland.ordered.map :as ordered-map]))

(defn path [lib v]
  (let [deps `{:deps {~lib {:mvn/version ~v}} :mvn/repos ~mvn/standard-repos}]
    (-> (tda/resolve-deps deps {})
        (get lib)
        :paths first)))

(defn index-by
  [f coll]
  (persistent! (reduce #(assoc! %1 (f %2) %2) (transient (ordered-map/ordered-map)) coll)))

(defn group [vars]
  (->> vars
       (map #(select-keys % [:ns :name :fixed-arities :varargs-min-arity :deprecated :private ::excluded]))
       (index-by (juxt :ns :name))))

(defn vars [lib exclude-meta]
  (if exclude-meta
    (let [{:keys [namespace-definitions var-definitions]}
          (-> (clj-kondo/run! {:lint   [lib]
                               :config {:output
                                        {:analysis {:var-definitions       {:meta true}
                                                    :namespace-definitions {:meta true}}
                                         :format   :edn}}})
              :analysis)
          ns-meta-excludes (reduce #(let [m (select-keys (:meta %2) exclude-meta)]
                                      (if (seq m)
                                        (assoc %1 (:name %2) m)
                                        %1))
                                   {}
                                   namespace-definitions)]
      (->> var-definitions
           (map #(if-let [excluded-by (some-> (merge (:meta %) (get ns-meta-excludes (:ns %)))
                                              (select-keys exclude-meta)
                                              seq)]
                   (assoc % ::excluded (format "meta %s" (vec (keys excluded-by))))
                   %))))
    (->> (clj-kondo/run! {:lint   [lib]
                          :config {:output {:analysis true :format :edn}}})
         :analysis :var-definitions)))

(defn log-diff [level v  msg]
  (let [{:keys [filename row col ns name]} v]
    (println (str filename ":" row ":" col ": " level ": " ns "/" name " " msg))))

(defn- force-os-path-syntax
  "see https://github.com/clj-kondo/clj-kondo/issues/1438
  can turf if/when this issue is fixed"
  [path]
  (some-> path io/file str))

(defn api-diff [{:keys [lib v1 v2
                        path1 path2
                        exclude-meta]}]
  (let [path1 (or (force-os-path-syntax path1) (path lib v1))
        path2 (or (force-os-path-syntax path2) (path lib v2))
        vars-1 (->> (vars path1 exclude-meta)
                    (sort-by (juxt :ns :row)))
        vars-2 (vars path2 exclude-meta)
        compare-group-1 (group vars-1)
        compare-group-2 (group vars-2)
        lookup-1 (index-by (juxt :ns :name) vars-1)]
    (doseq [[k var-1] compare-group-1]
      (when (and (not (:private var-1)) (not (::excluded var-1)))
        (if-let [var-2 (get compare-group-2 k)]
          (do
            (when (:private var-2)
              (log-diff "error" (get lookup-1 k) "has become private."))
            (let [fixed-arities-v1  (:fixed-arities var-1)
                  fixed-arities-v2  (:fixed-arities var-2)
                  varargs-min-arity (:varargs-min-arity var-2)]
              (doseq [arity fixed-arities-v1]
                (when-not (or (contains? fixed-arities-v2 arity)
                              (and varargs-min-arity (>= arity varargs-min-arity)))
                  (log-diff "error" (get lookup-1 k) (format "arity %s was removed." arity)))))
            (when (and (:deprecated var-2) (not (:deprecated var-1)))
              (log-diff "warning" (get lookup-1 k) "was deprecated."))
            (when (::excluded var-2)
              (log-diff "warning" (get lookup-1 k) (format "now has %s." (::excluded var-2)))))
          (log-diff "error" (get lookup-1 k) "was removed."))))))

(defn- to-keyword [s]
  (keyword
    (if (str/starts-with? s ":")
      (subs s 1)
      s)))

(defn parse-opts [opts opts-def]
  (let [[cmds opts] (split-with #(not (str/starts-with? % ":")) opts)]
    (reduce
     (fn [opts [arg-name arg-val]]
       (let [k (keyword (subs arg-name 1))
             od (k opts-def)
             v ((or (:parse-fn od) identity) arg-val)]
         (if-let [c (:collect-fn od)]
           (update opts k c v)
           (assoc opts k v))))
     {:cmds cmds}
     (partition 2 opts))))

(defn -main [& args]
  (let [{:keys [lib v1 v2 path1 path2] :as opts}
        (parse-opts args {:exclude-meta {:parse-fn to-keyword
                                         :collect-fn (fnil conj #{})}
                          :lib {:parse-fn symbol}})]
    (when-not (or (and lib v1 v2) (and path1 path2))
      (throw (ex-info "must specify either :lib, :v1 and :v2 OR :path1 and :path2" {})))
    (api-diff opts)))
