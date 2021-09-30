# api-diff

## Installation

```
clj -Ttools install com.github.borkdude/api-diff '{:git/tag "v0.0.1"}' :as api-diff
```

# Usage

```
clj -Tapi-diff api-diff :lib clj-kondo/clj-kondo :v2 '"2021.09.15"' :v1 '"2021.09.25"'
```

