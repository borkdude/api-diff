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
                         :path2 "test-resources/newer.clj"}))]
    (is (str/includes? out " example/y was removed"))
    (is (not (str/includes? out " example/x was removed")))
    (is (str/includes? out " warning: example/z was deprecated"))))
