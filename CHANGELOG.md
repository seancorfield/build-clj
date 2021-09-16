# Change Log

* v0.3.0 -- 2021-09-16
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
