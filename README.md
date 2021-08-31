# build-clj

Common build tasks abstracted into a library.

Having implemented `build.clj` in several of my open source projects
I found there was a lot of repetition across them, so I factored out
the common functionality into this library.

Since it depends on `tools.build` and
[Erik Assum's `deps-deploy`](https://github.com/slipset/deps-deploy),
your `:build` alias can just be:

```clojure
  :build {:deps {io.github.seancorfield/build-clj
                 {:git/tag "v0.1.0" :git/sha "fe2d586"}}
          :ns-default build}
```

Your `build.clj` can start off as follows:

```clojure
(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'myname/mylib)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "1.0.%s" (b/git-count-revs nil)))
```

The following common build tasks are provided, all taking an options
hash map as the single argument _and returning that hash map unchanged_
so you can reliably thread the build tasks.
_[Several functions in `clojure.tools.build.api` return `nil` instead]_

* `clean`     -- clean the target directory,
* `deploy`    -- deploy to Clojars,
* `jar`       -- build the (library) JAR and `pom.xml` files,
* `run-tests` -- run the project's tests.

For `deploy` and `jar`, you must provide at least `:lib` and `:version`.
Everything else has "sane" defaults, but can be overridden.

You might typically have the following tasks in your `build.clj`:

```clojure
(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/run-tests)
      (bb/clean)
      (bb/jar)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))
```

In addition, there is a `run-task` function that takes an options hash
map and a vector of aliases. This runs an arbitrary Clojure main function,
determined by those aliases, in a subprocess. `run-tests` uses this by
adding a `:test` alias and in the absence of any `:main-opts` behind those
aliases, assumes it should run `cognitect.test-runner`'s `-main` function.

If you want a `run-tests` task in your `build.clj`, independent of the `ci`
task shown above, the following can be added:

```clojure
(defn run-tests "Run the tests." [opts]
  (-> opts (bb/run-tests)))
```

`run-task` picks up `:jvm-opts` and `:main-opts` from the specified aliases
and uses them as the `:java-args` and `:main-args` respectively in a call to
`clojure.tools.build.api/java-command` to build the `java` command to run.
By default, it runs `clojure.main`'s `-main` function with the specified
`:main-args`.

For example, if your `deps.edn` contains the following alias:

```clojure
  :eastwood {:extra-deps {jonase/eastwood {:mvn/version "0.5.1"}}
             :main-opts ["-m" "eastwood.lint" "{:source-paths,[\"src\"]}"]}
```

Then you can define an `eastwood` task in your `build.clj` file:

```clojure
(defn eastwood "Run Eastwood." [opts]
  (-> opts (bb/run-task [:eastwood])))
```

Or you could just make it part of your `ci` pipeline without adding that function:

```clojure
(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/run-task [:eastwood])
      (bb/run-tests)
      (bb/clean)
      (bb/jar)))
```

## Defaults

The following defaults are provided:

* `:target`    -- `"target"`,
* `:basis`     -- `(create-basis {:project "deps.edn"}`,
* `:class-dir` -- `(str target "/classes")`,
* `:jar-file`  -- `(format \"%s/%s-%s.jar\" target lib version)`.

For the functions defined in `org.corfield.build`, you can override
the defaults as follows:

* `clean`
  * `:target`,
* `deploy`
  * Requires: `:lib` and `:version`,
  * `:target`, `:class-dir`, `:jar-file`,
* `jar`
  * Requires: `:lib` and `:version`,
  * `:target`, `:class-dir`, `:basis`, `:scm`, `:src-dirs`, `:tag` (defaults to `(str "v" version)`), `:jar-file`,
* `run-tests`
  * `:aliases` -- for any additional aliases beyond `:test` which is always added,
  * Also accepts any options that `run-task` accepts.

As noted above, `run-task` takes an options hash map and a vector of aliases.
The following options can be provided to `run-task` to override the default
behavior:

* `:java-opts` -- used _instead of_ `:jvm-opts` from the aliases,
* `:jvm-opts`  -- used _in addition to_ the `:java-opts` vector or _in addition to_ `:jvm-opts` from the aliases,
* `:main`      -- used _instead of_ `'clojure.main` when building the `java` command to run,
* `:main-args` -- used _instead of_ `:main-opts` from the aliases,
* `:main-opts` -- used _in addition to_ the `:main-args` vector or _in addition to_ `:main-opts` from the aliases.

> Note: if `:main-args` is not provided and there are no `:main-opts` in the aliases provided, the default will be `["-m" "cognitect.test-runner"]` to ensure that `run-tests` works by default without needing `:main-opts` in the `:test` alias (since it is common to want to start a REPL with `clj -A:test`).

## Projects Using `build-clj`

You can see how `build-clj` is used to reduce boilerplate in the
`build.clj` file of the following projects:

* [`depstar`](https://github.com/seancorfield/depstar/blob/develop/build.clj)
* [`expectations`](https://github.com/clojure-expectations/clojure-test/blob/develop/build.clj)
* [`honeysql`](https://github.com/seancorfield/honeysql/blob/develop/build.clj)
* [`next.jdbc`](https://github.com/seancorfield/next-jdbc/blob/develop/build.clj)

# License

Copyright © 2021 Sean Corfield

Distributed under the Apache Software License version 2.0.
