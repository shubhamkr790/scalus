package scalus.builtin

import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Shrink
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scalus.Compiler.compile
import scalus.*
import scalus.builtin.Builtins.*
import scalus.builtin.Data.*
import scalus.builtin.FromDataInstances.given
import scalus.uplc.*
import scalus.uplc.eval.Cek

import scala.annotation.nowarn

enum Adt:
    case A
    case B(b: Boolean)
    case C(a: Adt, b: Adt)

@Compile
object Adt:
    given FromData[Adt] = FromData.deriveEnum[Adt] {
        case 0 => _ => Adt.A
        case 1 => FromData.deriveConstructor[Adt.B]
        case 2 => FromData.deriveConstructor[Adt.C]
    }

@Compile
object ToDataAdt:
    given ToData[Adt] = (a: Adt) =>
        import ToDataInstances.given
        a match
            case Adt.A => constrData(0, mkNilData())
            case Adt.B(bs) =>
                constrData(1, mkCons(bs.toData, mkNilData()))
            case Adt.C(a, b) =>
                constrData(
                  2,
                  mkCons(a.toData, mkCons(b.toData, mkNilData()))
                )

case class BigRecord(
    a: Boolean,
    b: BigInt,
    bs: ByteString,
    s: String,
    d: Data,
    ls: scalus.prelude.List[BigInt],
    m: scalus.prelude.AssocMap[BigInt, scalus.prelude.Maybe[String]]
)

@Compile
object BigRecord extends ArbitraryInstances:
    given FromData[BigRecord] = FromData.deriveCaseClass[BigRecord]

@Compile
object ToDataBigRecord:
    import ToDataInstances.given
    /* given ToData[BigRecord] = (r: BigRecord) =>
    r match
      case BigRecord(a, b, bs, s, d, ls, m) =>
        mkConstr(
          0,
          scalus.builtin.List(a.toData, b.toData, bs.toData, s.toData, d, ls.toData, m.toData)
        ) */
    given ToData[BigRecord] = ToData.deriveCaseClass[BigRecord](0)

class FromDataDerivationSpec
    extends AnyFunSuite
    with ScalaCheckPropertyChecks
    with ArbitraryInstances {

    given Arbitrary[BigRecord] = Arbitrary(
      for
          a <- Arbitrary.arbitrary[Boolean]
          b <- Arbitrary.arbitrary[BigInt]
          bs <- Arbitrary.arbitrary[ByteString]
          s <- Arbitrary.arbitrary[String]
          d <- Arbitrary.arbitrary[Data]
          ls <- Arbitrary.arbitrary[scalus.prelude.List[BigInt]]
          m <- Arbitrary.arbitrary[scalus.prelude.AssocMap[BigInt, scalus.prelude.Maybe[String]]]
      yield BigRecord(a, b, bs, s, d, ls, m)
    )

    def sizedAdt(sz: Int): Gen[Adt] =
        if sz <= 0 then
            Gen.oneOf(
              Gen.const(Adt.A),
              for bs <- Arbitrary.arbitrary[Boolean]
              yield Adt.B(bs)
            )
        else
            for
                a <- sizedAdt(sz / 3)
                b <- sizedAdt(sz / 3)
            yield Adt.C(a, b)
    given Arbitrary[Adt] = Arbitrary(Gen.sized(sizedAdt))

    @nowarn("cat=deprecation")
    given Shrink[BigRecord] = Shrink { r =>
        val BigRecord(a, b, bs, s, d, ls, m) = r
        val aShrunk = Shrink.shrink(a).map(BigRecord(_, b, bs, s, d, ls, m))
        val bShrunk = Shrink.shrink(b).map(BigRecord(a, _, bs, s, d, ls, m))
        val bsShrunk = Shrink.shrink(bs).map(BigRecord(a, b, _, s, d, ls, m))
        val sShrunk = Shrink.shrink(s).map(BigRecord(a, b, bs, _, d, ls, m))
        val dShrunk = Shrink.shrink(d).map(BigRecord(a, b, bs, s, _, ls, m))
        val lsShrunk = Shrink.shrink(ls).map(BigRecord(a, b, bs, s, d, _, m))
        val mShrunk = Shrink.shrink(m).map(BigRecord(a, b, bs, s, d, ls, _))
        aShrunk ++ bShrunk ++ bsShrunk ++ sShrunk ++ dShrunk ++ lsShrunk ++ mShrunk
    }

    test("derived FromData roundtrip works using Plutus uplc") {
        import ToDataBigRecord.given
        import scalus.uplc.TermDSL.{*, given}
        val sir = compile { (d: Data) => fromData[BigRecord](d).toData }
        // println(sir.pretty.render(100))
        val term = sir.toUplc()
        // println(term.pretty.render(100))
        forAll { (r: BigRecord) =>
            val d = r.toData
            assert(fromData[BigRecord](d) == r)
            val out = UplcCli.evalFlat(Program((1, 0, 0), term $ d))
            out match
                case UplcEvalResult.Success(term, _) =>
                    assert(term == Term.Const(Constant.Data(d)))
                case UplcEvalResult.UplcFailure(errorCode, error) => fail(error)
                case UplcEvalResult.TermParsingError(error)       => fail(error)

            assert(Cek.evalUPLC(term $ d) == Term.Const(Constant.Data(d)))
        }
    }

    test("FromData.deriveEnum") {
        import ToDataAdt.given
        import scalus.uplc.TermDSL.{*, given}
        val sir = compile { (d: Data) => fromData[Adt](d).toData }
        val term = sir.toUplc()
        // println(sir.pretty.render(100))
        forAll { (r: Adt) =>
            val d = r.toData
            assert(fromData[Adt](d) == r)
            val out = UplcCli.evalFlat(Program((1, 0, 0), term $ d))
            out match
                case UplcEvalResult.Success(term, _) =>
                    assert(term == Term.Const(Constant.Data(d)))
                case UplcEvalResult.UplcFailure(errorCode, error) => fail(error)
                case UplcEvalResult.TermParsingError(error)       => fail(error)

            assert(Cek.evalUPLC(term $ d) == Term.Const(Constant.Data(d)))
        }
    }
}
