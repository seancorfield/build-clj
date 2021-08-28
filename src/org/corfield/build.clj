;; copyright (c) 2021 sean corfield, all rights reserved.

(ns org.corfield.build
  "Common build utilities.

  The following defaults are provided:
  :target    \"target\",
  :basis     (create-basis {:project \"deps.edn\"},
  :class-dir (str target \"/classes\"),
  :jar-file  (format \"%s/%s-%s.jar\" target lib version)

  You are expected to provide :lib and :version as needed.

  The following build functions are provided, with the
  specified required and optional hash map options:

  clean     -- opt :target,
  deploy    -- req :lib, :version
               opt :target, :class-dir, :jar-file
  jar       -- req :lib, :version
               opt :target, :class-dir, :basis, :src-dirs, :tag, :jar-file, :scm-url
  run-task  -- [opts aliases]
               opt :java-opts -- defaults to :jvm-opts from aliases
                   :jvm-opts  -- added to :java-opts
                   :main      -- defaults to clojure.main
                   :main-args -- defaults to :main-opts from aliases
                   :main-opts --
  run-tests -- opt :aliases (plus run-task options)
               invokes (run-task opts (into [:test] aliases))

  All of the above return the opts hash map they were passed
  (unlike some of the functions in clojure.tools.build.api)."
  (:require [clojure.tools.build.api :as b]
            [clojure.tools.deps.alpha :as t]
            [deps-deploy.deps-deploy :as dd]))

(def ^:private default-target "target")
(def ^:private default-basis (b/create-basis {:project "deps.edn"}))
(defn- default-class-dir [target] (str target "/classes"))
(defn- default-jar-file [target lib version]
  (format "%s/%s-%s.jar" target (name lib) version))

(defn clean
  "Remove the target folder."
  [{:keys [target] :as opts}]
  (println "\nCleaning target...")
  (b/delete {:path (or target default-target)})
  opts)

(defn jar
  "Build the library JAR file.

  Requires: lib, version"
  [{:keys [target class-dir lib version basis src-dirs tag jar-file scm-url] :as opts}]
  (assert (and lib version) "lib and version are required for jar")
  (let [target    (or target default-target)
        class-dir (or class-dir (default-class-dir target))
        basis     (or basis default-basis)
        src-dirs  (or src-dirs ["src"])
        tag       (or tag (str "v" version))
        jar-file  (or jar-file (default-jar-file target lib version))]
    (println "\nWriting pom.xml...")
    (b/write-pom {:class-dir class-dir
                  :lib       lib
                  :version   version
                  :scm       (cond-> {:tag tag}
                               scm-url (assoc :url scm-url))
                  :basis     basis
                  :src-dirs  src-dirs})
    (println "Copying src...")
    (b/copy-dir {:src-dirs   src-dirs
                 :target-dir class-dir})
    (println (str "Building jar " jar-file "..."))
    (b/jar {:class-dir class-dir
            :jar-file  jar-file}))
  opts)

(defn deploy
  "Deploy the JAR to Clojars.

  Requires: lib, version"
  [{:keys [target class-dir lib version jar-file] :as opts}]
  (assert (and lib version) "lib and version are required for deploy")
  (let [target    (or target default-target)
        class-dir (or class-dir (default-class-dir target))
        jar-file  (or jar-file (default-jar-file target lib version))]
    (dd/deploy (merge {:installer :remote :artifact jar-file
                       :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
                      opts)))
  opts)

(defn run-task
  "Run a task based on aliases.

  If :main-args is not provided and not :main-opts are found
  in the aliases, default to the Cognitect Labs' test-runner."
  [{:keys [java-opts jvm-opts main main-args main-opts] :as opts} aliases]
  (println "\nRunning task for:" aliases)
  (let [basis    (b/create-basis {:aliases aliases})
        combined (t/combine-aliases basis aliases)
        cmds     (b/java-command
                  {:basis     basis
                   :java-opts (into (or java-opts (:jvm-opts combined))
                                    jvm-opts)
                   :main      (or 'clojure.main main)
                   :main-args (into (or main-args
                                        (:main-opts combined)
                                        ["-m" "cognitect.test-runner"])
                                    main-opts)})
        {:keys [exit]} (b/process cmds)]
    (when-not (zero? exit)
      (throw (ex-info (str "Task failed for: " aliases) {}))))
  opts)

(defn run-tests
  "Run tests.

  Always adds :test to the aliases."
  [{:keys [aliases] :as opts}]
  (-> opts (run-task (into [:test] aliases))))
