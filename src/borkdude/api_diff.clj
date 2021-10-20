(ns borkdude.api-diff
  (:require
   [clj-kondo.core :as clj-kondo]
   [clojure.string :as str]
   [clojure.tools.deps.alpha :as tda]
   [clojure.tools.deps.alpha.util.maven :as mvn]))

(defn path [lib v]
  (let [deps `{:deps {~lib {:mvn/version ~v}} :mvn/repos ~mvn/standard-repos}]
    (-> (tda/resolve-deps deps {})
        (get lib)
        :paths first)))

(defn index-by
  [f coll]
  (persistent! (reduce #(assoc! %1 (f %2) %2) (transient {}) coll)))

(defn group [vars]
  (->> vars
       (map #(select-keys % [:ns :name :fixed-arities :varargs-min-arity :deprecated]))
       (index-by (juxt :ns :name))))

(defn vars [lib exclude-meta]
  (if exclude-meta
    (let [{:keys [namespace-definitions var-definitions]}
          (-> (clj-kondo/run! {:lint   [lib]
                               :config {:output
                                        {:analysis {:var-definitions {:meta true}
                                                    :namespace-definitions {:meta true}}
                                         :format :edn}}})
              :analysis)
          ns-meta-excludes (reduce #(let [m (select-keys (:meta %2) exclude-meta)]
                                      (if (seq m)
                                        (assoc %1 (:name %2) m)
                                        %1))
                                   {}
                                   namespace-definitions)]
      (->> var-definitions
           (remove :private)
           (remove #(some-> (merge (:meta %) (get ns-meta-excludes (:ns %)))
                            (select-keys exclude-meta)
                            seq))))
    (->> (clj-kondo/run! {:lint   [lib]
                          :config {:output {:analysis true :format :edn}}})
         :analysis :var-definitions
         (remove :private))))

(defn var-symbol [[k v]]
  (str k "/" v))

(defn api-diff [{:keys [lib v1 v2
                        path1 path2
                        exclude-meta]}]

  (let [path1 (or path1 (path lib v1))
        path2 (or path2 (path lib v2))
        vars-1 (->> (vars path1 exclude-meta)
                    (sort-by (juxt :ns :row)))
        vars-2 (vars path2 exclude-meta)
        compare-group-1 (group vars-1)
        compare-group-2 (group vars-2)
        lookup-1 (index-by (juxt :ns :name) vars-1)]
    (doseq [[k var-1] compare-group-1]
      (if-let [var-2 (get compare-group-2 k)]
        (let [fixed-arities-v1 (:fixed-arities var-1)
              fixed-arities-v2 (:fixed-arities var-2)
              varargs-min-arity (:varargs-min-arity var-2)]
          (doseq [arity fixed-arities-v1]
            (when-not (or (contains? fixed-arities-v2 arity)
                          (and varargs-min-arity (>= arity varargs-min-arity)))
              (let [{:keys [:filename :row :col :private]} (get lookup-1 k)]
                (println (str filename ":" row ":" col ":") (str (if private "warning" "error") ":")
                         "Arity" arity "of" (var-symbol k) "was removed."))))
          (when (and (:deprecated var-2)
                     (not (:deprecated var-1)))
            (let [{:keys [:filename :row :col]} (get lookup-1 k)]
              (println (str filename ":" row ":" col ":") (str "warning" ":")
                       (var-symbol k) "was deprecated."))))
        (let [{:keys [:filename :row :col :private]} (get lookup-1 k)]
          (println (str filename ":" row ":" col ":") (str (if private "warning" "error") ":")
                   (var-symbol k) "was removed."))))))

(defn parse-opts [opts opts-def]
  (let [[cmds opts] (split-with #(not (str/starts-with? % ":")) opts)]
    (reduce
     (fn [opts [arg-name arg-val]]
       (let [k (keyword (subs arg-name 1))
             v (case (some-> opts-def k :type)
                 :symbol (symbol arg-val)
                 :keyword (keyword (if (str/starts-with? arg-val ":")
                                     (subs arg-val 1)
                                     arg-val))
                 arg-val)]
         (if (some-> opts-def k :multi)
           (update opts k concat [v])
           (assoc opts k v))))
     {:cmds cmds}
     (partition 2 opts))))

(defn -main [& args]
  (let [{:keys [lib v1 v2 path1 path2] :as opts}
        (parse-opts args {:exclude-meta {:multi true
                                         :type :keyword}
                          :lib {:type :symbol}})]
    (when-not (or (and lib v1 v2) (and path1 path2))
      (throw (ex-info "must specify either :lib, :v1 and :v2 OR :path1 and :path2" {})))
    (api-diff opts)))
