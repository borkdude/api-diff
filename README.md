# api-diff

This tool prints the diff of breaking changes between two Clojure library
versions.

## Installation

To invoke via `-M:api-diff` add this alias to your `deps.edn`:

``` clojure
{:aliases
 {:api-diff
  {:replace-deps
   {borkdude/api-diff {:git/url "https://github.com/borkdude/api-diff"
                       :git/sha "<latest-sha>"}}
   :main-opts ["-m" "borkdude.api-diff"]}}}
```

Or install as tool:

```
clj -Ttools install com.github.borkdude/api-diff '{:git/tag "<latest-tag>"}' :as api-diff
```

# Usage

Arguments for comparing two mvn libs:

- `:lib`: fully qualified symbol
- `:v1`: the older version as mvn lib
- `:v2:` the newer version as mvn lib

Arguments for comparing two directories:

- `:path1`: the file or directory with older
- `:path2`: the file or directory with newer

This tool currently only prints breaking changes: removed vars or removed
arities.  To see what was added in a newer version, just swap `:v1` and `:v2`
(for now):

```
clj -M:api-diff :lib clj-kondo/clj-kondo :v1 2021.09.25 :v2 2021.09.15
clj_kondo/core.clj:213:1: error: clj-kondo.core/config-hash was removed.
clj_kondo/core.clj:205:1: error: clj-kondo.core/resolve-config was removed.
clj_kondo/impl/analyzer.clj:1473:1: error: clj-kondo.impl.analyzer/analyze-ns-unmap was removed.
```

Comparing two jars locally:
```
clj -Mapi-diff api-diff :path1 ./my-jar-v1.2.jar :path2 ./my-jar-v1.3.jar
```

or via tool usage:

```
$ clj -Tapi-diff api-diff :lib clj-kondo/clj-kondo :v1 '"2021.09.25"' :v2 '"2021.09.15"'
```

## Limitations

**Static Analysis**

Some libraries generate their APIs at load time.
Because api-diff uses [static analysis](https://github.com/clj-kondo/clj-kondo/tree/master/analysis), it will not see APIs generated at load time.
One example of a library that generates its API at load time is [Amazonica](https://github.com/mcohen01/amazonica).

There is one exception.
Api-diff uses clj-kondo's static analyzer to gather API information.
Clj-kondo includes special support for [potemkin import-vars](https://github.com/clj-commons/potemkin#import-vars).
So, if a library uses potemkin import-vars to generate its API at load time, api-diff should be able to see it.

