package scalus.prelude

import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scalus.Compiler.compile
import scalus.prelude.Maybe.*
import scalus.prelude.Prelude.{_, given}
import scalus.sir.SIR
import scalus.sir.SimpleSirToUplcLowering
import scalus.uplc.ArbitraryInstances
import scalus.uplc.Cek
import scalus.uplc.Term
import scalus.uplc.TermDSL.given

class MaybeSpec extends AnyFunSuite with ScalaCheckPropertyChecks with ArbitraryInstances {
  test("eq") {
    assert((Nothing: Maybe[String]) === Nothing)
    assert(Just(BigInt(1)) === Just(BigInt(1)))
    assert(Just(BigInt(1)) !== Just(BigInt(2)))
    assertEval(compile(new Just(true) === Nothing), false)
    assertEval(compile(new Just(true) === new Just(true)), true)
    assertEval(compile(new Just(true) !== new Just(true)), false)
  }

  private def assertEval(sir: SIR, expected: Term) = {
    val term = new SimpleSirToUplcLowering().lower(sir)
    assert(Cek.evalUPLC(term) == expected)
  }
}
