# build-clj

Common [`tools.build`](https://github.com/clojure/tools.build) tasks abstracted into a library, building on the examples in the [official `tools.build` guide](https://clojure.org/guides/tools_build).

Having implemented `build.clj` (using `tools.build`) in several of my open source projects
I found there was a lot of repetition across them, so I factored out
the common functionality into this library.

Since it depends on both `tools.build` and
[Erik Assum's `deps-deploy`](https://github.com/slipset/deps-deploy),
your `:build` alias can just be:

```clojure
  :build {:deps {io.github.seancorfield/build-clj
                 {:git/tag "v0.4.0" :git/sha "54e39ae"}}
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

## Tasks Provided

The following common build tasks are provided, all taking an options
hash map as the single argument _and returning that hash map unchanged_
so you can reliably thread the build tasks.
_[Several functions in `clojure.tools.build.api` return `nil` instead]_

* `clean`     -- clean the target directory (wraps `delete` from `tools.build`),
* `deploy`    -- deploy to Clojars (wraps `deploy` from `deps-deploy`),
* `install`   -- install the JAR locally (wraps `create-basis` and `install` from `tools.build`),
* `jar`       -- build the (library) JAR and `pom.xml` files (wraps `create-basis`, `write-pom`, `copy-dir`, and `jar` from `tools.build`),
* `uber`      -- build the (application) uber JAR, with optional `pom.xml` file creation and/or AOT compilation (wraps `create-basis`, `write-pom`, `copy-dir`, `compile-clj`, and `uber` from `tools.build`),
* `run-tests` -- run the project's tests (wraps `create-basis`, `java-command`, and `process` from `tools.build`, to run the `:main-opts` in your `:test` alias).
* `help`      -- prints help for your build, identified by its namespace symbol (typically `'build`). Returns nil.

For `deploy`, `install`, and `jar`, you must provide at least `:lib` and `:version`.
For `uber`, you must provide at least `:lib` or `:uber-file` for the name of the JAR file.
Everything else has "sane" defaults, but can be overridden.

## Typical `build.clj` with `build-clj`

You might typically have the following tasks in your `build.clj`:

```clojure
(defn ci "Run the CI pipeline of tests (and build the JAR)." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/run-tests)
      (bb/clean)
      (bb/jar)))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))

(defn help "Display this help message." [opts]
  (bb/help 'build))
```

Or if you are working with an application, you might have:

```clojure
(defn ci "Run the CI pipeline of tests (and build the uberjar)." [opts]
  (-> opts
      (assoc :lib lib :main main)
      (bb/run-tests)
      (bb/clean)
      (bb/uber)))

(defn help "Display this help message." [opts]
  (bb/help 'build))
```

> Note: this `uber` task in `build-clj` supplies the [log4j2 conflict handler](https://github.com/seancorfield/build-uber-log4j2-handler) to the underlying `uber` task of `tools.build` so that you don't have to worry about the plugins cache files being merged.

## Running Tests

If you want a `run-tests` task in your `build.clj`, independent of the `ci`
task shown above, the following can be added:

```clojure
(defn run-tests "Run the tests." [opts]
  (-> opts (bb/run-tests)))
```

By default, the `run-tests` task will run whatever is in your `:test` alias
but if there is no `:main-opts`, it assumes Cognitect's `test-runner`:

```clojure
  :test
  {:extra-paths ["test"]
   :extra-deps {org.clojure/test.check {:mvn/version "1.1.0"}
                io.github.cognitect-labs/test-runner
                {:git/tag "v0.5.0" :git/sha "48c3c67"}}
   :exec-fn cognitect.test-runner.api/test}
```

The above alias allows for tests to be run directly via:

```bash
clojure -X:test
```

The `run-tests` task above would run the tests as if the `:test` alias
also contained:

```clojure
   :main-opts ["-m" "cognitect.test-runner"]
```

If you want to use a different test runner with `build-clj`, just provide
different dependencies and supply `:main-opts`:

```clojure
  ;; a :test alias that specifies the kaocha runner:
  :test
  {:extra-paths ["test"]
   :extra-deps {lambdaisland/kaocha {:mvn/version "1.0.887"}}
   :main-opts ["-m" "kaocha.runner"]}
```

With this `:test` alias, the `run-tests` task above would run your tests using Kaocha.

## Running Additional Programs

In addition, there is a `run-task` function that takes an options hash
map and a vector of aliases. This runs an arbitrary Clojure main function,
determined by those aliases, in a subprocess. `run-tests` uses this by
adding a `:test` alias and in the absence of any `:main-opts` behind those
aliases, assumes it should run `cognitect.test-runner`'s `-main` function.

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
* `:basis`     -- `(b/create-basis {})` -- this is a reproducible basis, i.e., it ignores the user `deps.edn` file -- if you want your user `deps.edn` included, you will need to explicitly pass `:basis (b/create-basis {:user :standard})` into tasks,
* `:class-dir` -- `(str target "/classes")`,
* `:jar-file`  -- `(format \"%s/%s-%s.jar\" target lib version)`,
* `:uber-file` -- `(format \"%s/%s-%s.jar\" target lib version)` if `:version` is provided, else `(format \"%s/%s-standalone.jar\" target lib)`.

For the functions defined in `org.corfield.build`, you can override
the high-level defaults as follows:

* `clean`
  * `:target`,
* `deploy`
  * Requires: `:lib` and `:version`,
  * `:target`, `:class-dir`, `:jar-file`,
* `install`
  * Requires: `:lib` and `:version`,
  * `:target`, `:class-dir`, `:basis`, `:jar-file`,
* `jar`
  * Requires: `:lib` and `:version`,
  * `:target`, `:class-dir`, `:basis`, `:resource-dirs`, `:scm`, `:src-dirs`, `:tag` (defaults to `(str "v" version)`), `:jar-file`,
* `uber`
  * Requires: `:lib` or `:uber-file`,
  * `:target`, `:class-dir`, `:basis`, `:compile-opts`, `:main`, `:ns-compile`, `:resource-dirs`, `:scm`, `:src-dirs`, `:tag` (defaults to `(str "v" version)` if `:version` provided), `:version`
* `run-tests`
  * `:aliases` -- for any additional aliases beyond `:test` which is always added,
  * Also accepts any options that `run-task` accepts.

See the docstrings of those task functions for more detail on which options
they can also accept and which additional defaults they offer.

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

* [`expectations`](https://github.com/clojure-expectations/clojure-test/blob/develop/build.clj)
* [`honeysql`](https://github.com/seancorfield/honeysql/blob/develop/build.clj)
* [`next.jdbc`](https://github.com/seancorfield/next-jdbc/blob/develop/build.clj)

# License

Copyright © 2021 Sean Corfield

Distributed under the Apache Software License version 2.0.
