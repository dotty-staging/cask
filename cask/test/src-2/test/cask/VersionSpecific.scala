package test.cask

import scala.language.experimental.macros

object VersionSpecific {

  // See src-3/VersionSpecific for a comment on why we can't use
  // `utest.compileError` directly in Scala 3.
  def compileError(expr: String): utest.CompileError = macro utest.asserts.Asserts.compileError

}
