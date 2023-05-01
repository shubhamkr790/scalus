package scalus.flat

import io.bullet.borer.Cbor
import org.scalacheck.{Arbitrary, Gen, Shrink}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scalus.builtins
import scalus.uplc.*
import scalus.utils.Utils

import scala.util.Random

class FlatSpec extends AnyFunSuite with ScalaCheckPropertyChecks with ArbitraryInstances:
  test("Flat bits") {
    val enc = new EncoderState(3)
    enc.bits(7, 64)
//    println(enc)
    enc.bits(5, 22)
//    println(enc)
    enc.bits(6, 47)
    enc.nextWord()
    val dec = new DecoderState(enc.buffer)
    assert(dec.bits8(7) == 64)
    assert(dec.bits8(5) == 22)
    assert(dec.bits8(6) == 47)
  }

  test("encode/decode random bytes") {
    val gen = Gen.nonEmptyListOf(Arbitrary.arbitrary[Byte])
    implicit def noShrink[T]: Shrink[T] = Shrink.shrinkAny

    forAll(gen) { bs =>
      val bitlens =
        bs.map(b =>
          if b == 0 then 1
          else if b < 0 then 8
          else Integer.numberOfTrailingZeros(Integer.highestOneBit(b)) + 1
        )
      val enc = new EncoderState(bs.length + 1)
      val valuesWithBitSizes = bs.zip(bitlens)
//      println(valuesWithBitSizes)
      valuesWithBitSizes.foreach { case (vl, sz) => enc.bits(sz, vl) }
      enc.nextWord()
//      println(enc)
      val dec = new DecoderState(enc.buffer)
      valuesWithBitSizes.foreach { case (vl, sz) =>
        val decodedValue = dec.bits8(sz)
        assert(decodedValue == vl)
      }
    }
  }

  test("encode/decode Array[Byte]") {
    val fl = summon[Flat[Array[Byte]]]

    {
      val arr = Array.empty[Byte]
      assert(fl.bitSize(arr) == 16)
      val enc = EncoderState(2)
      fl.encode(arr, enc)
      assert(Utils.bytesToHex(enc.result) == "0100")
      val dec = DecoderState(enc.result)
      assert(fl.decode(dec).length == 0)
    }

    {
      val arr = Array[Byte](11, 22, 33)
      assert(fl.bitSize(arr) == 6 * 8)
      val enc = EncoderState(6)
      fl.encode(arr, enc)
      assert(Utils.bytesToHex(enc.result) == "01030B162100")
      val dec = DecoderState(enc.result)
      assert(Utils.bytesToHex(fl.decode(dec)) == "0B1621")
    }

    def check(n: Int) =
      val arr = new Array[Byte](n)
      Random.nextBytes(arr)
      val enc = EncoderState(fl.bitSize(arr) / 8 + 1)
      fl.encode(arr, enc)
      val result = enc.result
      val dec = DecoderState(result)
      val decoded = fl.decode(dec)
      assert(decoded.sameElements(arr))

    check(1)
    check(255)
    check(256)
    check(510)
    check(Random.between(1, 3 * 255))

    enum Val:
      case Bit(b: Boolean)
      case ByteArray(arr: Array[Byte])

    given Arbitrary[Val] = Arbitrary(
      Gen.oneOf(
        Gen.oneOf(true, false).map(Val.Bit.apply),
        Gen.containerOf[Array, Byte](Arbitrary.arbitrary[Byte]).map(Val.ByteArray.apply)
      )
    )
    forAll { (v: List[Val]) =>
      val size = v.map {
        case Val.Bit(_)         => 1
        case Val.ByteArray(arr) => summon[Flat[Array[Byte]]].bitSize(arr)
      }.sum
      val enc = new EncoderState(size / 8 + 1)
      v.foreach {
        case Val.Bit(b)         => summon[Flat[Boolean]].encode(b, enc)
        case Val.ByteArray(arr) => summon[Flat[Array[Byte]]].encode(arr, enc)
      }
      enc.nextWord()
      val result = enc.result
      val dec = DecoderState(result)
      v.foreach(_ match {
        case Val.Bit(b)         => assert(summon[Flat[Boolean]].decode(dec) == b)
        case Val.ByteArray(arr) => assert(summon[Flat[Array[Byte]]].decode(dec).sameElements(arr))
      })
    }
  }

  test("Zagzig/zigZag") {
    forAll { (n: BigInt) =>
      assert(zagZig(zigZag(n)) == n)
    }
  }

  test("encode/decode BigInt") {
    val fl = summon[Flat[BigInt]]
    assert(fl.bitSize(BigInt(0)) == 8)
    assert(fl.bitSize(BigInt(1)) == 8)
    assert(fl.bitSize(BigInt(-1)) == 8)
    assert(fl.bitSize(BigInt(2) << 120) == 144)

    def check(n: BigInt, encodedHex: String) =
      val enc = EncoderState(fl.bitSize(n) / 8 + 1)
      fl.encode(n, enc)
      assert(Utils.bytesToHex(enc.result) == encodedHex)
      val dec = DecoderState(enc.result)
      assert(fl.decode(dec) == n)

    check(BigInt(0), "00")
    check(BigInt(1), "02")
    check(BigInt(-1), "01")
    check(BigInt(64), "8001")
    check(BigInt(-80), "9F01")

    forAll { (n: BigInt) =>
      val enc = EncoderState(fl.bitSize(n) / 8 + 1)
      fl.encode(n, enc)
      val result = enc.result
      val dec = DecoderState(result)
      val decoded = fl.decode(dec)
      assert(decoded == n)
    }
  }

  test("encode/decode String") {
    val fl = summon[Flat[String]]
    assert(fl.bitSize("") == 16)
    assert(fl.bitSize("aaa") == 6 * 8)

    def check(n: String, encodedHex: String) =
      val enc = EncoderState(fl.bitSize(n) / 8 + 1)
      fl.encode(n, enc)
      assert(Utils.bytesToHex(enc.result) == encodedHex)
      val dec = DecoderState(enc.result)
      assert(fl.decode(dec) == n)

    check("", "0100")
    check("a", "01016100")
    check("Ї", "0102D08700")

    forAll { (n: String) =>
      val enc = EncoderState(fl.bitSize(n) / 8 + 1)
      fl.encode(n, enc)
      val result = enc.result
      val dec = DecoderState(result)
      val decoded = fl.decode(dec)
      assert(decoded == n)
    }
  }

  test("encode/decode DefaulnUni") {
    import scalus.uplc.DefaultFun.*
    import scalus.uplc.FlatInstantces.given
    val fl = summon[Flat[DefaultFun]]
    assert(fl.bitSize(AddInteger) == 7)
    forAll(Gen.oneOf(DefaultFun.values)) { (f: DefaultFun) =>
      val enc = EncoderState(1)
      fl.encode(f, enc)
      enc.nextWord()
      val result = enc.result
      val dec = DecoderState(result)
      val decoded = fl.decode(dec)
      assert(decoded == f)
    }
  }

  test("encode/decode Constant") {
    import scalus.uplc.Constant.*
    import scalus.uplc.Data.*
    import scalus.uplc.FlatInstantces.given
    val fl = summon[Flat[Constant]]
    assert(fl.bitSize(Constant.Unit) == 6)
    assert(fl.bitSize(Constant.Bool(true)) == 7)
    assert(fl.bitSize(Constant.Integer(1)) == 14)
    assert(fl.bitSize(Constant.ByteString(builtins.ByteString.empty)) == 22)
    assert(fl.bitSize(Constant.ByteString(builtins.ByteString(11, 22, 33))) == 54)
    assert(fl.bitSize(Constant.String("Ї")) == 46)
    assert(fl.bitSize(Constant.Data(1.toData)) == 38)
    forAll { (c: Constant) =>
      val enc = EncoderState(fl.bitSize(c) / 8 + 10)
      fl.encode(c, enc)
      enc.nextWord()
      val result = enc.result
      val dec = DecoderState(result)
      val decoded = fl.decode(dec)
      assert(decoded == c)
    }
  }

  test("encode/decode Term") {
    import scalus.uplc.Data.toData
    import scalus.uplc.FlatInstantces.given
    val fl = summon[Flat[Term]]
    assert(fl.bitSize(Term.Error("any string")) == 4)
    assert(fl.bitSize(Term.Var(NamedDeBruijn("any name", 12))) == 12)
    // 4 bits for Const tag + 1 bit Cons of type tags list + 4 bits for Unit tag + 1 bit for Nil
    assert(fl.bitSize(Term.Const(Constant.Unit)) == 10)
    // 10 bits as above + 1 bit for Bool
    assert(fl.bitSize(Term.Const(Constant.Bool(true))) == 11)
    // 10 bits as above + 8 bit for small integer
    assert(fl.bitSize(Term.Const(Constant.Integer(1))) == 18)
    // 10 bits as above + 16 bits for 0 length byte array (as in Flat implementation)
    assert(fl.bitSize(Term.Const(Constant.ByteString(builtins.ByteString.empty))) == 26)
    assert(fl.bitSize(Term.Const(Constant.ByteString(builtins.ByteString(11, 22, 33)))) == 58)
    assert(fl.bitSize(Term.Const(Constant.String("Ї"))) == 50)
    assert(fl.bitSize(Term.Const(Constant.Data(1.toData))) == 42)
    forAll { (t: Term) =>
      // 1. Serialize initial Term
      val enc = EncoderState(fl.bitSize(t) / 8 + 2)
      fl.encode(t, enc)
      enc.nextWord()
      // 2. Deserialize Term with only indices in names
      val result = enc.result
      val dec = DecoderState(result)
      val decoded = fl.decode(dec)
      // 3. Serialize Term with only indices in names
      val enc2 = EncoderState(fl.bitSize(decoded) / 8 + 2)
      fl.encode(decoded, enc2)
      enc2.nextWord()
      val result2 = enc.result
      // 4. Deserialize Term with only indices in names again
      val dec2 = DecoderState(result2)
      val decoded2 = fl.decode(dec2)
      // 5. Compare
      assert(decoded == decoded2)
    }
  }
