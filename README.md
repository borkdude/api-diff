# api-diff

This is the `clj -T` version of this
[gist](https://gist.github.com/borkdude/2b963db1582654ec28bfd40b4dc35748). It
prints the diff of breaking changes between library versions.

## Installation

As tool:

```
clj -Ttools install com.github.borkdude/api-diff '{:git/tag "<latest-tag>"}' :as api-diff
```

To invoke via `-main` in `:api-diff` alias:

``` clojure
{:aliases
 {:api-diff
  {:replace-deps
   {borkdude/api-diff {:git/url "https://github.com/borkdude/api-diff"
                       :git/sha "<latest-sha>"}}
   :main-opts ["-m" "borkdude.api-diff"]}}}
```

# Usage

Arguments for comparing two mvn libs:

- `:lib`: fully qualified symbol
- `:v1`: the older version as mvn lib
- `:v2:` the newer version as mvn lib

Arguments for comparing two directories:

- `:path1`: the file or directory with older
- `:path2`: the file or directory with newer

This tool currently only prints breaking changes: removed vars or removed arities.
To see what was added in a newer version, just swap `:v1` and `:v2` (for now).

```
$ clj -Tapi-diff api-diff :lib clj-kondo/clj-kondo :v1 '"2021.09.25"' :v2 '"2021.09.15"'
clj_kondo/core.clj:213:1: error: clj-kondo.core/config-hash was removed.
clj_kondo/core.clj:205:1: error: clj-kondo.core/resolve-config was removed.
clj_kondo/impl/analyzer.clj:1473:1: error: clj-kondo.impl.analyzer/analyze-ns-unmap was removed.
```

Or via the `:api-diff` alias:

```
clj -M:api-diff :lib clj-kondo/clj-kondo :v1 2021.09.25 :v2 2021.09.15
```
