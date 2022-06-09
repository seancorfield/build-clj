# Change Log

* v0.8.2 0ffdb4c -- 2022-06-09
  * Update `tools.build` to v0.8.2 for `compile-clj` enhancement and dependency updates.

* v0.8.0 9bd8b8a -- 2022-03-06
  * Fix [#18](https://github.com/seancorfield/build-clj/issues/18) by using `resolve-path` from `tools.build`.
  * This project is now tracking `tools.build` version numbers (v0.8.0) _[I'll figure out a version schema for any releases I make between `tools.build` releases if that actually happens!]_

* v0.7.0 5d2cb60 -- 2022-02-24
  * Update `tools.build` to v0.8.0 for various enhancements and bug fixes.

* v0.6.7 22c2d09 -- 2022-01-05
  * Update `build-uber-log4j2-handler` to v0.1.5 and `tools.build` to v0.7.5 for various bug fixes.

* v0.6.6 171d5f1 -- 2022-01-04
  * Update `build-uber-log4j2-handler` to v0.1.4 for updated log4j2 dependency.

* v0.6.5 972031a -- 2021-12-23
  * Update `tools.build` to v0.7.4 for various enhancements and bug fixes.

* v0.6.4 c21cfde -- 2021-12-22
  * Update `tools.build` to v0.7.3 for various enhancements and bug fixes.
  * Update `build-uber-log4j2-handler` to v0.1.3 for updated log4j2 dependency.

* v0.6.3 9b8e09b -- 2021-12-14
  * Update `build-uber-log4j2-handler` to v0.1.2 for updated log4j2 dependency.

* v0.6.2 97c275a -- 2021-12-13
  * Update `tools.build` to v0.7.2 0361dde for various enhancements and bug fixes.

* v0.6.1 6e962ef -- 2021-12-10
  * Update `build-uber-log4j2-handler` to v0.1.1 for updated log4j2 dependency.

* v0.6.0 2451bea -- 2021-12-01
  * Provide a "slim" entry point that does not include `deps-deploy`.

* v0.5.5 0527130 -- 2021-11-26
  * Update `tools.build` to v0.6.8 d79ae84 for `git-process` and various bug fixes.

* v0.5.4 bc9c0cc -- 2021-11-13
  * Update `tools.build` to v0.6.5 a0c3ff6 for the improved `compile-clj` and git commands.

* v0.5.3 dbf7321 -- 2021-11-08
  * Update `tools.build` to v0.6.3 4a1b53a for updated git deps handling.

* v0.5.2 8f75b81 -- 2021-10-11
  * Update `tools.build` to v0.6.1 515b334 (for non-replacement on some non-text files).

* v0.5.1 dc121d6 -- 2021-10-07
  * Support Polylith and other monorepo projects better when building library JARs by adding `:transitive` option to `jar` task.

* v0.5.0 2ceb95a -- 2021-09-27
  * Address #10 by exposing the four `default-*` functions used to compute `target`, `class-dir`, `basis`, and `jar-file` (`uber-file`).

* v0.4.0 54e39ae -- 2021-09-22
  * Address #6 by providing `install` task based on `tools.build` (and deprecating `:installer :local` for `deploy` task).
  * Update `tools.build` to v0.5.1 21da7d4.

* v0.3.1 996ddfa -- 2021-09-17
  * Update `deps-deploy` to 0.2.0 for latest deps and new Maven settings support.

* v0.3.0 fb11811 -- 2021-09-16
  * `:lib` is now optional for `uber` if you pass in `:uber-file`.
  * Update `tools.build` to v0.5.0 7d77952 (default basis is now "repro" without user `deps.edn`).

* v0.2.2 5a12a1a -- 2021-09-15
  * Address #3 by adding an `uber` task which includes the log4j2 plugins cache conflict handler.
  * Added support for many more options that `tools.build` tasks accept.
  * Overhaul/expansion of docstrings.
  * Fix `:src-dirs` defaulting.
  * Remove spurious `println` from `uber` task.
  * Update `tools.build` to v0.4.0 801a22f.

* v0.1.3 26b884c -- 2021-09-07
  * Update `tools.build` to v0.2.2 3049217.

* v0.1.2 0719a09 -- 2021-08-31
  * Update `tools.build` to v0.2.0 7cbb94b.

* v0.1.1 d096192 -- 2021-08-30
  * Address #1 by adding `:scm` option; `:tag` will still override `:scm {:tag ...}`.

* v0.1.0 fe2d586 -- 2021-08-27
  * Initial release
