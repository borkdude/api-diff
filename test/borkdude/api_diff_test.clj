(ns borkdude.api-diff-test
  (:require [borkdude.api-diff :as api-diff]
            [clojure.string :as str]
            [clojure.test :as t :refer [deftest testing is]]))

(defn- osify [lines]
  (mapv (fn [l]
          (if (str/starts-with? l "test-resources")
            (let [[p r] (str/split l #" " 2)]
              (str
               (str/replace p "/" (System/getProperty "file.separator"))
               " "
               r))
            l))
        lines))

(deftest diff-test
  (testing "libs"
    (let [actual-lines (-> (api-diff/-main ":lib" "clj-kondo/clj-kondo"
                                           ":v1" "2021.09.25"
                                           ":v2" "2021.09.15")
                           with-out-str
                           str/split-lines)]
      (is (= (osify ["clj_kondo/core.clj:205:1: error: clj-kondo.core/resolve-config was removed."
                     "clj_kondo/core.clj:213:1: error: clj-kondo.core/config-hash was removed."
                     "clj_kondo/impl/analyzer.clj:1473:1: error: clj-kondo.impl.analyzer/analyze-ns-unmap was removed."])
             actual-lines))))
  (testing "paths"
    (let [actual-lines (-> (api-diff/-main ":path1" "test-resources/older"
                                           ":path2" "test-resources/newer")
                           with-out-str
                           str/split-lines)]
      (is (= (osify ["test-resources/older/example.clj:6:1: error: example/becomes-private has become private."
                     "test-resources/older/example.clj:11:1: error: example/y was removed."
                     "test-resources/older/example.clj:12:1: warning: example/z was deprecated."
                     "test-resources/older/example.clj:14:1: error: example/nodoc-changes arity 1 was removed."
                     "test-resources/older/example.clj:16:1: error: example/skip-wiki was removed."
                     "test-resources/older/example.clj:18:1: error: example/arity-change-and-becomes-skip-wiki arity 2 was removed."
                     "test-resources/older/other.clj:3:1: error: other/other-x was removed."])
             actual-lines)))
    (testing "files"
      (let [actual-lines (-> (api-diff/-main ":path1" "test-resources/older/example.clj"
                                             ":path2" "test-resources/newer/example.clj")
                             with-out-str
                             str/split-lines)]
        (is (= (osify ["test-resources/older/example.clj:6:1: error: example/becomes-private has become private."
                       "test-resources/older/example.clj:11:1: error: example/y was removed."
                       "test-resources/older/example.clj:12:1: warning: example/z was deprecated."
                       "test-resources/older/example.clj:14:1: error: example/nodoc-changes arity 1 was removed."
                       "test-resources/older/example.clj:16:1: error: example/skip-wiki was removed."
                       "test-resources/older/example.clj:18:1: error: example/arity-change-and-becomes-skip-wiki arity 2 was removed."])
               actual-lines))))
    (testing "exclude-meta single"
      (let [actual-lines (-> (api-diff/-main ":path1" "test-resources/older"
                                             ":path2" "test-resources/newer"
                                             ":exclude-meta" ":no-doc")
                             with-out-str
                             str/split-lines)]
        (is (= (osify ["test-resources/older/example.clj:6:1: error: example/becomes-private has become private."
                       "test-resources/older/example.clj:8:1: warning: example/becomes-nodoc now has meta [:no-doc]."
                       "test-resources/older/example.clj:11:1: error: example/y was removed."
                       "test-resources/older/example.clj:12:1: warning: example/z was deprecated."
                       "test-resources/older/example.clj:16:1: error: example/skip-wiki was removed."
                       "test-resources/older/example.clj:18:1: error: example/arity-change-and-becomes-skip-wiki arity 2 was removed."])
               actual-lines))))
    (testing "exclude-meta multiple"
      (let [actual-lines (-> (api-diff/-main ":path1" "test-resources/older"
                                             ":path2" "test-resources/newer"
                                             ":exclude-meta" ":no-doc"
                                             ":exclude-meta" ":skip-wiki")
                             with-out-str
                             str/split-lines)]
        (is (= (osify ["test-resources/older/example.clj:6:1: error: example/becomes-private has become private."
                       "test-resources/older/example.clj:8:1: warning: example/becomes-nodoc now has meta [:no-doc]."
                       "test-resources/older/example.clj:11:1: error: example/y was removed."
                       "test-resources/older/example.clj:12:1: warning: example/z was deprecated."
                       "test-resources/older/example.clj:18:1: error: example/arity-change-and-becomes-skip-wiki arity 2 was removed."
                       "test-resources/older/example.clj:18:1: warning: example/arity-change-and-becomes-skip-wiki now has meta [:skip-wiki]."])
               actual-lines))))))
;; => #'borkdude.api-diff-test/diff-test
