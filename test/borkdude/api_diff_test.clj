(ns borkdude.api-diff-test
  (:require [borkdude.api-diff :as api-diff :refer [api-diff]]
            [clojure.string :as str]
            [clojure.test :as t :refer [deftest is]]))

(deftest diff-test
  (let [out (with-out-str
             (api-diff {:lib 'clj-kondo/clj-kondo
                        :v1 "2021.09.25"
                        :v2 "2021.09.15"}))]
    (is (str/includes? out "clj-kondo.core/config-hash was removed")))
  (let [out (with-out-str
              (api-diff {:path1 "test-resources/older.clj"
                         :path2 "test-resources/newer.clj"}))
        actual-lines (str/split-lines out)]
    (is (= ["test-resources/older.clj:6:1: error: example/becomes-private was removed."
            "test-resources/older.clj:9:1: error: example/y was removed."
            "test-resources/older.clj:10:1: warning: example/z was deprecated."]
           actual-lines))))
