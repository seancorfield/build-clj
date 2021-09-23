;; copyright (c) 2021 sean corfield, all rights reserved.

(ns org.corfield.build
  "Common build utilities.

  The following high-level defaults are provided:

  :target    \"target\",
  :basis     (create-basis {:project \"deps.edn\"},
  :class-dir (str target \"/classes\"),
  :jar-file  (format \"%s/%s-%s.jar\" target lib version),
  :uber-file (format \"%s/%s-%s.jar\" target lib version)
             or, if :version is not provided:
             (format \"%s/%s-standalone.jar\" target lib)

  You are expected to provide :lib and :version as needed.

  The following build task functions are provided, with the
  specified required and optional hash map options:

  clean     -- opt :target,
  deploy    -- req :lib, :version
               opt :target, :class-dir, :jar-file
               (see docstring for additional options)
  install   -- req :lib, :version
               opt :target, :class-dir, :basis, :jar-file
               (see docstring for additional options)
  jar       -- req :lib, :version
               opt :target, :class-dir, :basis, :scm, :src-dirs,
                   :resource-dirs, :tag, :jar-file
               (see docstring for additional options)
  uber      -- req :lib or :uber-file
               opt :target, :class-dir, :basis, :scm, :src-dirs,
                   :resource-dirs, :tag, :version
               (see docstring for additional options)
  run-task  -- [opts aliases]
               opt :java-opts -- defaults to :jvm-opts from aliases
                   :jvm-opts  -- added to :java-opts
                   :main      -- defaults to clojure.main
                   :main-args -- defaults to :main-opts from aliases
                   :main-opts -- added to :main-args
  run-tests -- opt :aliases (plus run-task options)
               invokes (run-task opts (into [:test] aliases))

  All of the above return the opts hash map they were passed
  (unlike some of the functions in clojure.tools.build.api).

  The following low-level defaults are also provided to make
  it easier to call the task functions here:

  :ns-compile if :main is provided and :sort is not, this
              defaults to the :main namespace (class),

  :scm        if :tag is provided, that is used here, else
              if :version is provided, that is used for :tag
              here with \"v\" prefixed,

  :src-dirs   [\"src\"]

  :src+dirs   this is a synthetic option that is used for the
              file/directory copying that is part of `jar` and
              `uber` and it is computed as :src-dirs plus
              :resource-dirs, essentially, with the former
              defaulted as noted above and the latter defaulted
              to [\"resources\"] just for the copying but otherwise
              has no default (for `tools.build/write-pom`)."
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [clojure.tools.deps.alpha :as t]
            [deps-deploy.deps-deploy :as dd]
            [org.corfield.log4j2-conflict-handler
             :refer [log4j2-conflict-handler]]))

(defn- default-target
  [target]
  (or target "target"))

(defn- default-basis
  [basis]
  (or basis (b/create-basis {})))

(defn- default-class-dir
  [class-dir target]
  (or class-dir (str (default-target target) "/classes")))

(defn- default-jar-file
  [target lib version]
  (format "%s/%s-%s.jar" (default-target target) (name (or lib 'application)) version))

(defn clean
  "Remove the target folder."
  [{:keys [target] :as opts}]
  (println "\nCleaning target...")
  (b/delete {:path (default-target target)})
  opts)

(defn- jar-opts
  "Provide sane defaults for jar/uber tasks.

  :lib is required, :version is optional for uber, everything
  else is optional."
  [{:keys [basis class-dir conflict-handlers jar-file lib
           main ns-compile resource-dirs scm sort src-dirs tag
           target uber-file version]
    :as   opts}]
  (let [scm-default   (cond tag     {:tag tag}
                            version {:tag (str "v" version)})
        src-default   (or src-dirs ["src"])
        version       (or version "standalone")
        xxx-file      (default-jar-file target lib version)]
    (assoc opts
           :basis      (default-basis basis)
           :class-dir  (default-class-dir class-dir target)
           :conflict-handlers
           (merge log4j2-conflict-handler conflict-handlers)
           :jar-file   (or    jar-file    xxx-file)
           :ns-compile (or    ns-compile  (when (and main (not sort))
                                            [main]))
           :scm        (merge scm-default scm)
           :src-dirs   src-default
           :src+dirs   (into  src-default (or resource-dirs ["resources"]))
           :uber-file  (or    uber-file   xxx-file))))

(defn jar
  "Build the library JAR file.

  Requires: :lib, :version

  Accepts any options that are accepted by:
  * tools.build/write-pom
  * tools.build/jar

  Writes pom.xml into META-INF in the :class-dir, then
  copies :src-dirs + :resource-dirs into :class-dir, then
  builds :jar-file into :target (directory)."
  {:arglists '([{:keys [lib version
                        basis class-dir jar-file main manifest repos
                        resource-dirs scm src-dirs src-pom tag target]}])}
  [{:keys [lib version] :as opts}]
  (assert (and lib version) "lib and version are required for jar")
  (let [{:keys [class-dir jar-file src+dirs] :as opts}
        (jar-opts opts)]
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "Copying" (str (str/join ", " src+dirs) "..."))
    (b/copy-dir {:src-dirs   src+dirs
                 :target-dir class-dir})
    (println "Building jar" (str jar-file "..."))
    (b/jar opts))
  opts)

(defn uber
  "Build the application uber JAR file.

  Requires: :lib or :uber-file

  Accepts any options that are accepted by:
  * `tools.build/write-pom`
  * `tools.build/compile-clj`
  * `tools.build/uber`

  The uber JAR filename is derived from :lib
  and :version if provided, else from :uber-file.

  If :version is provided, writes pom.xml into
  META-INF in the :class-dir, then

  Compiles :src-dirs into :class-dir, then
  copies :src-dirs and :resource-dirs into :class-dir, then
  builds :uber-file into :target (directory)."
  {:argslists '([{:keys [lib uber-file
                         basis class-dir compile-opts filter-nses
                         ns-compile repos resource-dirs scm sort
                         src-dirs src-pom tag target version]}])}
  [{:keys [lib uber-file] :as opts}]
  (assert (or lib uber-file) ":lib or :uber-file is required for uber")
  (let [{:keys [class-dir lib ns-compile sort src-dirs src+dirs uber-file version]
         :as   opts}
        (jar-opts opts)]
    (if (and lib version)
      (do
        (println "\nWriting pom.xml...")
        (b/write-pom opts))
      (println "\nSkipping pom.xml because :lib and/or :version were omitted..."))
    (println "Copying" (str (str/join ", " src+dirs) "..."))
    (b/copy-dir {:src-dirs   src+dirs
                 :target-dir class-dir})
    (if (or ns-compile sort)
      (do
        (println "Compiling" (str (str/join ", " (or ns-compile src-dirs)) "..."))
        (b/compile-clj opts))
      (println "Skipping compilation because :main, :ns-compile, and :sort were omitted..."))
    (println "Building uberjar" (str uber-file "..."))
    (b/uber opts))
  opts)

(defn install
  "Install the JAR to the local Maven repo cache.

  Requires: :lib, :version

  Accepts any options that are accepted by:
  * `tools.build/install`"
  {:arglists '([{:keys [lib version
                        basis class-dir classifier jar-file target]}])}
  [{:keys [lib version basis class-dir classifier jar-file target] :as opts}]
  (assert (and lib version) ":lib and :version are required for install")
  (let [target (default-target target)]
    (b/install {:basis      (default-basis basis)
                :lib        lib
                :classifier classifier
                :version    version
                :jar-file   (or jar-file (default-jar-file target lib version))
                :class-dir  (default-class-dir class-dir target)})
    opts))

(defn deploy
  "Deploy the JAR to Clojars.

  Requires: :lib, :version

  Accepts any options that are accepted by:
  * `deps-deploy/deploy`

  If :artifact is provided, it will be used for the deploy,
  else :jar-file will be used (making it easy to thread
  options through `jar` and `deploy`, specifying just :jar-file
  or relying on the default value computed for :jar-file)."
  {:arglists '([{:keys [lib version
                        artifact class-dir installer jar-file pom-file target]}])}
  [{:keys [lib version class-dir installer jar-file target] :as opts}]
  (assert (and lib version) ":lib and :version are required for deploy")
  (when (and installer (not= :remote installer))
    (println installer ":installer is deprecated -- use install task for local deployment"))
  (let [target    (default-target target)
        class-dir (default-class-dir class-dir target)
        jar-file  (or jar-file (default-jar-file target lib version))]
    (dd/deploy (merge {:installer :remote :artifact jar-file
                       :pom-file (b/pom-path {:lib lib :class-dir class-dir})}
                      opts)))
  opts)

(defn run-task
  "Run a task based on aliases.

  If :main-args is not provided and no :main-opts are found
  in the aliases, default to the Cognitect Labs' test-runner."
  [{:keys [java-opts jvm-opts main main-args main-opts] :as opts} aliases]
  (let [task     (str/join ", " (map name aliases))
        _        (println "\nRunning task for:" task)
        basis    (b/create-basis {:aliases aliases})
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
      (throw (ex-info (str "Task failed for: " task) {}))))
  opts)

(defn run-tests
  "Run tests.

  Always adds :test to the aliases."
  [{:keys [aliases] :as opts}]
  (-> opts (run-task (into [:test] aliases))))
