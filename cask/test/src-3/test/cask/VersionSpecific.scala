package test.cask

import utest._

object VersionSpecific {

  // This is a workaround for https://github.com/lampepfl/dotty/issues/11630.
  // The essence of the issue is that between Dotty 3.0.0-M3 and 3.0.0-RC1 the
  // inliner and typer phases were decoupled. Since utest defines
  // `compilerError` as `transparent` in order to propagate error position
  // information, it no can no longer catch compilation errors resulting from
  // inlines. However, since cask heavily uses macros, we need the old behavior
  // to check for error cases. Hence, to work around the issue, we forward to
  // utest, but this time without the `transparent` modifier.
  inline def compileError(inline expr: String): utest.CompileError = utest.compileError(expr)

}
