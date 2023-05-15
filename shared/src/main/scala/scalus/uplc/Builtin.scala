package scalus.uplc

import scalus.builtins.Builtins
import scalus.uplc.Cek.CekValue
import scalus.uplc.Cek.VCon
import scalus.uplc.Constant.given
import scalus.uplc.DefaultUni.Bool
import scalus.uplc.DefaultUni.Integer
import scalus.uplc.DefaultUni.asConstant
import scalus.uplc.DefaultUni.given
import scalus.utils.Utils

import scala.annotation.targetName
import scala.collection.immutable
import scala.collection.mutable.ArrayBuffer

enum TypeScheme:
  case Type(argType: DefaultUni)
  case Arrow(argType: TypeScheme, t: TypeScheme)
  case All(name: String, t: TypeScheme)
  case TVar(name: String)

  def ->:(t: TypeScheme): TypeScheme = Arrow(t, this)
  def ->:(t: DefaultUni): TypeScheme = Arrow(Type(t), this)

extension (x: DefaultUni)
  def ->:(t: DefaultUni): TypeScheme = TypeScheme.Arrow(TypeScheme.Type(t), TypeScheme.Type(x))

//  def ->:(t: TypeScheme): TypeScheme = TypeScheme.Arrow(t, TypeScheme.Type(x))

case class Runtime(
    typeScheme: TypeScheme,
    f: AnyRef
)

trait Meaning:
  def typeScheme: TypeScheme

object Meaning:
  def mkMeaning(t: TypeScheme, f: AnyRef) = Runtime(t, f)
  import TypeScheme.*

  val AddInteger =
    mkMeaning(
      Integer ->: Integer ->: Integer,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.addInteger(aa, bb)))
    )
  val SubtractInteger =
    mkMeaning(
      Integer ->: Integer ->: Integer,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.subtractInteger(aa, bb)))
    )
  val MultiplyInteger =
    mkMeaning(
      Integer ->: Integer ->: Integer,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.multiplyInteger(aa, bb)))
    )
  val DivideInteger =
    mkMeaning(
      Integer ->: Integer ->: Integer,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.divideInteger(aa, bb)))
    )
  val QuotientInteger =
    mkMeaning(
      Integer ->: Integer ->: Integer,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.quotientInteger(aa, bb)))
    )
  val RemainderInteger =
    mkMeaning(
      Integer ->: Integer ->: Integer,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.remainderInteger(aa, bb)))
    )
  val ModInteger =
    mkMeaning(
      Integer ->: Integer ->: Integer,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.modInteger(aa, bb)))
    )
  val EqualsInteger =
    mkMeaning(
      Integer ->: Integer ->: Bool,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.equalsInteger(aa, bb)))
    )
  val LessThanEqualsInteger =
    mkMeaning(
      Integer ->: Integer ->: Bool,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.lessThanEqualsInteger(aa, bb)))
    )
  val LessThanInteger =
    mkMeaning(
      Integer ->: Integer ->: Bool,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.lessThanInteger(aa, bb)))
    )

  val AppendByteString =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.ByteString ->: DefaultUni.ByteString,
      (a: CekValue) =>
        val aa = a.asByteString
        (b: CekValue) =>
          val bb = b.asByteString
          () => Cek.VCon(asConstant(Builtins.appendByteString(aa, bb)))
    )

  val ConsByteString =
    mkMeaning(
      DefaultUni.Integer ->: DefaultUni.ByteString ->: DefaultUni.ByteString,
      (a: CekValue) =>
        val aa = a.asInteger
        (b: CekValue) =>
          val bb = b.asByteString
          () => Cek.VCon(asConstant(Builtins.consByteString(aa, bb)))
    )

  val SliceByteString =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.Integer ->: DefaultUni.Integer ->: DefaultUni.ByteString,
      (a: CekValue) =>
        val bs = a.asByteString
        (b: CekValue) =>
          val start = b.asInteger
          (c: CekValue) =>
            val end = c.asInteger
            () => Cek.VCon(asConstant(Builtins.sliceByteString(bs, start, end)))
    )

  val IndexByteString =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.Integer ->: DefaultUni.Integer,
      (a: CekValue) =>
        val aa = a.asByteString
        (b: CekValue) =>
          val bb = b.asInteger
          () => Cek.VCon(asConstant(Builtins.indexByteString(aa, bb)))
    )

  val LengthOfByteString =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.Integer,
      (a: CekValue) =>
        val aa = a.asByteString
        () => Cek.VCon(asConstant(Builtins.lengthOfByteString(aa)))
    )

  val EqualsByteString =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.ByteString ->: Bool,
      (a: CekValue) =>
        val aa = a.asByteString
        (b: CekValue) =>
          val bb = b.asByteString
          () => Cek.VCon(asConstant(aa == bb))
    )

  val LessThanByteString =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.ByteString ->: Bool,
      (a: CekValue) =>
        val aa = a.asByteString
        (b: CekValue) =>
          val bb = b.asByteString
          () => Cek.VCon(asConstant(Builtins.lessThanByteString(aa, bb)))
    )

  val LessThanEqualsByteString =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.ByteString ->: Bool,
      (a: CekValue) =>
        val aa = a.asByteString
        (b: CekValue) =>
          val bb = b.asByteString
          () => Cek.VCon(asConstant(Builtins.lessThanEqualsByteString(aa, bb)))
    )

  val Sha2_256 =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.ByteString,
      (a: CekValue) =>
        val aa = a.asByteString
        () => Cek.VCon(asConstant(Builtins.sha2_256(aa)))
    )

  val Sha3_256 =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.ByteString,
      (a: CekValue) =>
        val aa = a.asByteString
        () => Cek.VCon(asConstant(Builtins.sha3_256(aa)))
    )

  val Blake2b_256 =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.ByteString,
      (a: CekValue) =>
        val aa = a.asByteString
        () => Cek.VCon(asConstant(Builtins.blake2b_256(aa)))
    )

  val VerifyEd25519Signature = {
    val tpe =
      DefaultUni.ByteString ->: DefaultUni.ByteString ->: DefaultUni.ByteString ->: DefaultUni.Bool
    mkMeaning(
      tpe,
      (a: CekValue) =>
        val pk = a.asByteString
        (b: CekValue) =>
          val msg = b.asByteString
          (c: CekValue) =>
            val sig = c.asByteString
            () => Cek.VCon(asConstant(Builtins.verifyEd25519Signature(pk, msg, sig)))
    )
  }

  val VerifyEcdsaSecp256k1Signature = {
    val tpe =
      DefaultUni.ByteString ->: DefaultUni.ByteString ->: DefaultUni.ByteString ->: DefaultUni.Bool
    mkMeaning(
      tpe,
      (a: CekValue) =>
        val pk = a.asByteString
        (b: CekValue) =>
          val msg = b.asByteString
          (c: CekValue) =>
            val sig = c.asByteString
            () => Cek.VCon(asConstant(Builtins.verifyEcdsaSecp256k1Signature(pk, msg, sig)))
    )
  }

  val VerifySchnorrSecp256k1Signature = {
    val tpe =
      DefaultUni.ByteString ->: DefaultUni.ByteString ->: DefaultUni.ByteString ->: DefaultUni.Bool
    mkMeaning(
      tpe,
      (a: CekValue) =>
        val pk = a.asByteString
        (b: CekValue) =>
          val msg = b.asByteString
          (c: CekValue) =>
            val sig = c.asByteString
            () => Cek.VCon(asConstant(Builtins.verifySchnorrSecp256k1Signature(pk, msg, sig)))
    )
  }

  val AppendString =
    mkMeaning(
      DefaultUni.String ->: DefaultUni.String ->: DefaultUni.String,
      (a: CekValue) =>
        val aa = a.asString
        (b: CekValue) =>
          val bb = b.asString
          () => Cek.VCon(asConstant(Builtins.appendString(aa, bb)))
    )

  val EqualsString =
    mkMeaning(
      DefaultUni.String ->: DefaultUni.String ->: Bool,
      (a: CekValue) =>
        val aa = a.asString
        (b: CekValue) =>
          val bb = b.asString
          () => Cek.VCon(asConstant(Builtins.equalsString(aa, bb)))
    )

  val EncodeUtf8 = {
    val tpe = DefaultUni.String ->: DefaultUni.ByteString
    mkMeaning(
      tpe,
      (a: CekValue) =>
        val aa = a.asString
        () => Cek.VCon(asConstant(Builtins.encodeUtf8(aa)))
    )
  }

  val DecodeUtf8 =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.String,
      (a: CekValue) =>
        val aa = a.asByteString
        () => Cek.VCon(asConstant(Builtins.decodeUtf8(aa)))
    )

  val IfThenElse =
    mkMeaning(
      All("a", Bool ->: TVar("a") ->: TVar("a") ->: TVar("a")),
      (b: CekValue) =>
        val bb = b.asBool
        (t: CekValue) => (f: CekValue) => () => Builtins.ifThenElse(bb, t, f)
    )

  val ChooseUnit =
    mkMeaning(
      All("a", DefaultUni.Unit ->: TVar("a") ->: TVar("a")),
      (unit: CekValue) =>
        unit match
          case Cek.VCon(Constant.Unit) => (a: CekValue) => () => a
          case _                       => throw new Error("impossible")
    )

  val Trace =
    mkMeaning(
      All("a", DefaultUni.String ->: TVar("a") ->: TVar("a")),
      (a: CekValue) =>
        val aa = a.asString
        (b: CekValue) => () => scalus.builtins.Builtins.trace(aa)(b)
    )

  // [ forall a, forall b, pair(a, b) ] -> a
  val FstPair =
    mkMeaning(
      // FIXME wrong type
      All("a", All("b", DefaultUni.Pair(Integer, Bool) ->: TVar("a"))),
      (a: CekValue) =>
        val (fst, _) = a.asPair
        () => Cek.VCon(fst)
    )

  // [ forall a, forall b, pair(a, b) ] -> b
  val SndPair =
    mkMeaning(
      // FIXME wrong type
      All("a", All("b", DefaultUni.Pair(Integer, Bool) ->: Bool)),
      (a: CekValue) =>
        val (_, snd) = a.asPair
        () => Cek.VCon(snd)
    )

  // [ forall a, forall b, list(a), b, b ] -> b
  val ChooseList =
    mkMeaning(
      // FIXME wrong type
      All("a", All("b", DefaultUni.List(Bool) ->: TVar("b") ->: TVar("b") ->: TVar("b"))),
      (a: CekValue) =>
        val ls = a.asList
        (b: CekValue) => (c: CekValue) => () => if ls.isEmpty then b else c
    )

  val MkCons =
    mkMeaning(
      All("a", Integer ->: DefaultUni.List(Integer) ->: DefaultUni.List(Integer)),
      (a: CekValue) =>
        (b: CekValue) =>
          (a, b) match
            case (VCon(aCon), VCon(Constant.List(tp, l))) => // fixme chek type
              () => Cek.VCon(Constant.List(tp, aCon :: l))
            case _ => throw new RuntimeException(s"Expected list, got $this")
    )

  // [ forall a, list(a) ] -> a
  val HeadList =
    mkMeaning(
      // FIXME wrong type
      All("a", Bool ->: Bool),
      (a: CekValue) =>
        val ls = a.asList
        () => Cek.VCon(ls.head)
    )

  // [ forall a, list(a) ] -> list(a)
  val TailList =
    mkMeaning(
      // FIXME wrong type
      All("a", Bool ->: Bool),
      (a: CekValue) =>
        a match
          case VCon(Constant.List(tpe, ls)) =>
            () => Cek.VCon(Constant.List(tpe, ls.tail))
          case _ => throw new Exception(s"TailList: not a list, but $a")
    )

  // [ forall a, list(a) ] -> bool
  val NullList =
    mkMeaning(
      // FIXME wrong type
      All("a", Bool ->: Bool),
      (a: CekValue) =>
        val ls = a.asList
        () => Cek.VCon(asConstant(ls.isEmpty))
    )

  val ChooseData =
    mkMeaning(
      All(
        "a",
        DefaultUni.Data ->: TVar("a") ->: TVar("a") ->: TVar("a") ->: TVar("a") ->: TVar(
          "a"
        ) ->: TVar("a")
      ),
      (a: CekValue) =>
        val aa = a.asData
        (b: CekValue) =>
          (c: CekValue) =>
            (d: CekValue) =>
              (e: CekValue) => (f: CekValue) => () => Builtins.chooseData(aa, b, c, d, e, f)
    )

  val ConstrData =
    mkMeaning(
      Integer ->: DefaultUni.List(DefaultUni.Data) ->: DefaultUni.Data,
      (a: CekValue) =>
        val i = a.asInteger
        (b: CekValue) =>
          val args = b match {
            case VCon(Constant.List(DefaultUni.Data, l)) =>
              l.map {
                case Constant.Data(d) => d
                case _                => throw new Exception(s"ConstrData: not a data, but $b")
              }
            case _ => throw new RuntimeException(s"Expected list, got $this")
          }
          () =>
            Cek.VCon(
              Constant.Data(Data.Constr(i.longValue, args))
            )
    )

  val MapData =
    mkMeaning(
      DefaultUni.List(DefaultUni.Pair(DefaultUni.Data, DefaultUni.Data)) ->: DefaultUni.Data,
      (a: CekValue) =>
        val aa = a.asList
        () =>
          Cek.VCon(
            Constant.Data(Data.Map(aa.map {
              case Constant.Pair(Constant.Data(a), Constant.Data(b)) => (a, b)
              case _ => throw new RuntimeException(s"MapData: not a pair, but $a")
            }))
          )
    )

  val ListData =
    mkMeaning(
      DefaultUni.List(DefaultUni.Data) ->: DefaultUni.Data,
      (a: CekValue) =>
        val aa = a.asList
        val datas = aa.map {
          case Constant.Data(value) => value
          case _                    => throw new RuntimeException(s"ListData: not a data, but $a")
        }
        () => Cek.VCon(Constant.Data(Data.List(datas)))
    )

  val IData =
    mkMeaning(
      Integer ->: DefaultUni.Data,
      (a: CekValue) =>
        val aa = a.asInteger
        () => Cek.VCon(Constant.Data(Data.I(aa)))
    )

  val BData =
    mkMeaning(
      DefaultUni.ByteString ->: DefaultUni.Data,
      (a: CekValue) =>
        val aa = a.asByteString
        () => Cek.VCon(Constant.Data(Data.B(aa)))
    )

  /*
    unConstrData : [ data ] -> pair(integer, list(data))
   */
  val UnConstrData =
    mkMeaning(
      DefaultUni.Data ->: DefaultUni.Pair(Integer, DefaultUni.List(DefaultUni.Data)),
      (a: CekValue) =>
        a match
          case VCon(Constant.Data(Data.Constr(i, ls))) =>
            () =>
              Cek.VCon(
                Constant.Pair(asConstant(i), asConstant(ls))
              )
          case _ => throw new Exception(s"unConstrData: not a constructor, but $a")
    )

  /*  unMapData : [ data ] -> list(pair(data, data))
   */
  val UnMapData =
    mkMeaning(
      DefaultUni.Data ->: DefaultUni.List(DefaultUni.Pair(DefaultUni.Data, DefaultUni.Data)),
      (a: CekValue) =>
        a match
          case VCon(Constant.Data(Data.Map(values))) =>
            () =>
              Cek.VCon(
                Constant.List(
                  DefaultUni.Pair(DefaultUni.Data, DefaultUni.Data),
                  values.map { case (k, v) =>
                    Constant.Pair(asConstant(k), asConstant(v))
                  }
                )
              )
          case _ => throw new Exception(s"unMapData: not a map, but $a")
    )
  /*  unListData : [ data ] -> list(data)
   */
  val UnListData =
    mkMeaning(
      DefaultUni.Data ->: DefaultUni.List(DefaultUni.Data),
      (a: CekValue) =>
        a match
          case VCon(Constant.Data(Data.List(values))) =>
            () => Cek.VCon(Constant.List(DefaultUni.Data, values.map(asConstant)))
          case _ => throw new Exception(s"unListData: not a list, but $a")
    )

  /*  unIData : [ data ] -> integer
   */
  val UnIData =
    mkMeaning(
      DefaultUni.Data ->: DefaultUni.Integer,
      (a: CekValue) =>
        a match
          case VCon(Constant.Data(Data.I(i))) =>
            () => Cek.VCon(asConstant(i))
          case _ => throw new Exception(s"unIData: not an integer, but $a")
    )

  /*  unBData : [ data ] -> bytestring
   */
  val UnBData =
    mkMeaning(
      DefaultUni.Data ->: DefaultUni.ByteString,
      (a: CekValue) =>
        a match
          case VCon(Constant.Data(Data.B(b))) =>
            () => Cek.VCon(asConstant(b))
          case _ => throw new Exception(s"unBData: not a bytestring, but $a")
    )

  val EqualsData =
    mkMeaning(
      DefaultUni.Data ->: DefaultUni.Data ->: DefaultUni.Bool,
      (a: CekValue) =>
        val aa = a.asData
        (b: CekValue) =>
          val bb = b.asData
          () => Cek.VCon(Constant.Bool(Builtins.equalsData(aa, bb)))
    )

  val SerialiseData = mkMeaning(
    DefaultUni.Data ->: DefaultUni.ByteString,
    (a: CekValue) =>
      val aa = a.asData
      () => Cek.VCon(Constant.ByteString(Builtins.serialiseData(aa)))
  )

  val MkPairData =
    mkMeaning(
      DefaultUni.Data ->: DefaultUni.Data ->: DefaultUni.Pair(DefaultUni.Data, DefaultUni.Data),
      (a: CekValue) =>
        val aa = a.asData
        (b: CekValue) =>
          val bb = b.asData
          () => Cek.VCon(Constant.Pair(asConstant(aa), asConstant(bb)))
    )

  val MkNilData =
    mkMeaning(
      Type(DefaultUni.List(DefaultUni.Data)),
      () => Cek.VCon(Constant.List(DefaultUni.Data, Nil))
    )

  val MkNilPairData = mkMeaning(
    Type(DefaultUni.Pair(DefaultUni.Data, DefaultUni.Data)),
    () => Cek.VCon(Constant.List(DefaultUni.Pair(DefaultUni.Data, DefaultUni.Data), Nil))
  )

  val BuiltinMeanings: immutable.Map[DefaultFun, Runtime] = immutable.Map.apply(
    (DefaultFun.AddInteger, Meaning.AddInteger),
    (DefaultFun.SubtractInteger, Meaning.SubtractInteger),
    (DefaultFun.MultiplyInteger, Meaning.MultiplyInteger),
    (DefaultFun.DivideInteger, Meaning.DivideInteger),
    (DefaultFun.QuotientInteger, Meaning.QuotientInteger),
    (DefaultFun.RemainderInteger, Meaning.RemainderInteger),
    (DefaultFun.ModInteger, Meaning.ModInteger),
    (DefaultFun.EqualsInteger, Meaning.EqualsInteger),
    (DefaultFun.LessThanEqualsInteger, Meaning.LessThanEqualsInteger),
    (DefaultFun.LessThanInteger, Meaning.LessThanInteger),
    (DefaultFun.AppendByteString, Meaning.AppendByteString),
    (DefaultFun.ConsByteString, Meaning.ConsByteString),
    (DefaultFun.SliceByteString, Meaning.SliceByteString),
    (DefaultFun.LengthOfByteString, Meaning.LengthOfByteString),
    (DefaultFun.IndexByteString, Meaning.IndexByteString),
    (DefaultFun.EqualsByteString, Meaning.EqualsByteString),
    (DefaultFun.LessThanByteString, Meaning.LessThanByteString),
    (DefaultFun.LessThanEqualsByteString, Meaning.LessThanEqualsByteString),
    (DefaultFun.Sha2_256, Meaning.Sha2_256),
    (DefaultFun.Sha3_256, Meaning.Sha3_256),
    (DefaultFun.Blake2b_256, Meaning.Blake2b_256),
    (DefaultFun.VerifyEd25519Signature, Meaning.VerifyEd25519Signature),
    (DefaultFun.VerifyEcdsaSecp256k1Signature, Meaning.VerifyEcdsaSecp256k1Signature),
    (DefaultFun.VerifySchnorrSecp256k1Signature, Meaning.VerifySchnorrSecp256k1Signature),
    (DefaultFun.AppendString, Meaning.AppendString),
    (DefaultFun.EqualsString, Meaning.EqualsString),
    (DefaultFun.EncodeUtf8, Meaning.EncodeUtf8),
    (DefaultFun.DecodeUtf8, Meaning.DecodeUtf8),
    (DefaultFun.IfThenElse, Meaning.IfThenElse),
    (DefaultFun.ChooseUnit, Meaning.ChooseUnit),
    (DefaultFun.Trace, Meaning.Trace),
    (DefaultFun.FstPair, Meaning.FstPair),
    (DefaultFun.SndPair, Meaning.SndPair),
    (DefaultFun.ChooseList, Meaning.ChooseList),
    (DefaultFun.MkCons, Meaning.MkCons),
    (DefaultFun.HeadList, Meaning.HeadList),
    (DefaultFun.TailList, Meaning.TailList),
    (DefaultFun.NullList, Meaning.NullList),
    (DefaultFun.ChooseData, Meaning.ChooseData),
    (DefaultFun.ConstrData, Meaning.ConstrData),
    (DefaultFun.MapData, Meaning.MapData),
    (DefaultFun.ListData, Meaning.ListData),
    (DefaultFun.IData, Meaning.IData),
    (DefaultFun.BData, Meaning.BData),
    (DefaultFun.UnConstrData, Meaning.UnConstrData),
    (DefaultFun.UnMapData, Meaning.UnMapData),
    (DefaultFun.UnListData, Meaning.UnListData),
    (DefaultFun.UnIData, Meaning.UnIData),
    (DefaultFun.UnBData, Meaning.UnBData),
    (DefaultFun.EqualsData, Meaning.EqualsData),
    (DefaultFun.SerialiseData, Meaning.SerialiseData),
    (DefaultFun.MkPairData, Meaning.MkPairData),
    (DefaultFun.MkNilData, Meaning.MkNilData),
    (DefaultFun.MkNilPairData, Meaning.MkNilPairData)
  )
