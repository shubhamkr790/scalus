package scalus
package uplc
package eval

import scala.io.Source.fromFile
import scala.language.implicitConversions

/** Tests for the Plutus Conformance Test Suite.
  *
  * @note
  *   This tests run only on JVM right now.
  */
class PlutusConformanceSpec extends BaseValidatorSpec:

    private type EvalFailure = "evaluation failure"
    private type ParseError = "parse error"
    private type Error = EvalFailure | ParseError
    private def parseExpected(code: String): Either[Error, Program] = {
        code match
            case "evaluation failure" => Left("evaluation failure")
            case "parse error"        => Left("parse error")
            case _ =>
                UplcParser().parseProgram(code) match
                    case Left(value) => fail(s"Unexpected parse error: $value")
                    case Right(program) =>
                        Right(program.copy(term = DeBruijn.deBruijnTerm(program.term)))

    }

    private def eval(code: String): Either[Error, Program] = {
        UplcParser().parseProgram(code) match
            case Right(program) =>
                try Right(program.copy(term = VM.evaluateProgram(program)))
                catch case e: Exception => Left("evaluation failure")
            case Left(e) =>
                Left("parse error")
    }

    private def check(name: String) =
        val path =
            s"../plutus-conformance/test-cases/uplc/evaluation"
        val code = fromFile(s"$path/$name.uplc").mkString
        val expected = fromFile(s"$path/$name.uplc.expected").mkString
        test(name) {
            // println(eval(code).pretty.render(80))
            (eval(code), parseExpected(expected)) match
                case (Right(actual), Right(expected)) =>
                    assert(actual alphaEq expected)
                case (Left(e1), Left(e2)) => assert(e1 == e2)
                case (a, b)               => fail(s"Expected $b but got $a")
        }

    // builtins
    // check("builtin/constant/bls12-381/G1/bad-syntax-1/bad-syntax-1")
    // check("builtin/constant/bls12-381/G1/bad-syntax-2/bad-syntax-2")
    // check("builtin/constant/bls12-381/G1/bad-zero-1/bad-zero-1")
    // check("builtin/constant/bls12-381/G1/bad-zero-2/bad-zero-2")
    // check("builtin/constant/bls12-381/G1/bad-zero-3/bad-zero-3")
    // check("builtin/constant/bls12-381/G1/off-curve/off-curve")
    // check("builtin/constant/bls12-381/G1/on-curve-bit3-clear/on-curve-bit3-clear")
    // check("builtin/constant/bls12-381/G1/on-curve-bit3-set/on-curve-bit3-set")
    // check("builtin/constant/bls12-381/G1/on-curve-serialised-not-compressed/on-curve-serialised-not-compressed")
    // check("builtin/constant/bls12-381/G1/out-of-group/out-of-group")
    // check("builtin/constant/bls12-381/G1/too-long/too-long")
    // check("builtin/constant/bls12-381/G1/too-short/too-short")
    // check("builtin/constant/bls12-381/G1/zero/zero")
    // check("builtin/constant/bls12-381/G2/bad-syntax-1/bad-syntax-1")
    // check("builtin/constant/bls12-381/G2/bad-syntax-2/bad-syntax-2")
    // check("builtin/constant/bls12-381/G2/bad-zero-1/bad-zero-1")
    // check("builtin/constant/bls12-381/G2/bad-zero-2/bad-zero-2")
    // check("builtin/constant/bls12-381/G2/bad-zero-3/bad-zero-3")
    // check("builtin/constant/bls12-381/G2/off-curve/off-curve")
    // check("builtin/constant/bls12-381/G2/on-curve-bit3-clear/on-curve-bit3-clear")
    // check("builtin/constant/bls12-381/G2/on-curve-bit3-set/on-curve-bit3-set")
    // check("builtin/constant/bls12-381/G2/on-curve-serialised-not-compressed/on-curve-serialised-not-compressed")
    // check("builtin/constant/bls12-381/G2/out-of-group/out-of-group")
    // check("builtin/constant/bls12-381/G2/too-long/too-long")
    // check("builtin/constant/bls12-381/G2/too-short/too-short")
    // check("builtin/constant/bls12-381/G2/zero/zero")
    check("builtin/constant/bool/False/False")
    check("builtin/constant/bool/True/True")
    check("builtin/constant/bytestring/bytestring1/bytestring1")
    check("builtin/constant/bytestring/bytestring2/bytestring2")
    check("builtin/constant/bytestring/bytestring3/bytestring3")
    check("builtin/constant/bytestring/bytestring4/bytestring4")
    check("builtin/constant/data/dataByteString/dataByteString")
    check("builtin/constant/data/dataConstr/dataConstr")
    check("builtin/constant/data/dataInteger/dataInteger")
    check("builtin/constant/data/dataList/dataList")
    check("builtin/constant/data/dataMap/dataMap")
    check("builtin/constant/data/dataMisByteString/dataMisByteString")
    check("builtin/constant/data/dataMisConstr/dataMisConstr")
    check("builtin/constant/data/dataMisInteger/dataMisInteger")
    check("builtin/constant/data/dataMisList/dataMisList")
    check("builtin/constant/data/dataMisMap/dataMisMap")
    check("builtin/constant/integer/integer1/integer1")
    check("builtin/constant/integer/integer10/integer10")
    check("builtin/constant/integer/integer2/integer2")
    check("builtin/constant/integer/integer3/integer3")
    check("builtin/constant/integer/integer4/integer4")
    check("builtin/constant/integer/integer5/integer5")
    check("builtin/constant/integer/integer6/integer6")
    check("builtin/constant/integer/integer7/integer7")
    check("builtin/constant/integer/integer8/integer8")
    check("builtin/constant/integer/integer9/integer9")
    check("builtin/constant/list/emptyList/emptyList")
    check("builtin/constant/list/simpleList/simpleList")
    check("builtin/constant/list/unitList/unitList")
    check("builtin/constant/pair/illTypedNestedPair/illTypedNestedPair")
    check("builtin/constant/pair/nestedPair/nestedPair")
    check("builtin/constant/pair/simplePair/simplePair")
    check("builtin/constant/string/string1/string1")
    check("builtin/constant/string/string2/string2")
    check("builtin/constant/string/string3/string3")
    check("builtin/constant/string/string4/string4")
    check("builtin/constant/string/string5/string5")
    check("builtin/constant/string/string6/string6")
    check("builtin/constant/unit/conUnit")
    check("builtin/interleaving/ite/ite")
    check("builtin/interleaving/iteAtIntegerArrowIntegerApplied1/iteAtIntegerArrowIntegerApplied1")
    check("builtin/interleaving/iteAtIntegerArrowIntegerApplied2/iteAtIntegerArrowIntegerApplied2")
    check(
      "builtin/interleaving/iteAtIntegerArrowIntegerAppliedApplied/iteAtIntegerArrowIntegerAppliedApplied"
    )
    check("builtin/interleaving/iteAtIntegerArrowIntegerWithCond/iteAtIntegerArrowIntegerWithCond")
    check("builtin/interleaving/iteForceAppForce/iteForceAppForce")
    check("builtin/interleaving/iteForced/iteForced")
    check("builtin/interleaving/iteForcedForced/iteForcedForced")
    check("builtin/interleaving/iteForcedWithIntegerAndString/iteForcedWithIntegerAndString")
    check("builtin/interleaving/iteStringInteger/iteStringInteger")
    check("builtin/interleaving/iteStringString/iteStringString")
    check("builtin/interleaving/iteUnforcedFullyApplied/iteUnforcedFullyApplied")
    check("builtin/interleaving/iteUnforcedWithCond/iteUnforcedWithCond")
    check("builtin/interleaving/iteWrongCondTypeFullyAppied/iteWrongCondTypeFullyAppied")
    check("builtin/interleaving/iteWrongCondTypePartiallyApplied/iteWrongCondTypePartiallyApplied")
    check("builtin/interleaving/multiplyIntegerForceError1/multiplyIntegerForceError1")
    check("builtin/interleaving/multiplyIntegerForceError2/multiplyIntegerForceError2")
    check("builtin/interleaving/multiplyIntegerForceError3/multiplyIntegerForceError3")
    check("builtin/semantics/addInteger/addInteger-uncurried/addInteger-uncurried")
    check("builtin/semantics/addInteger/addInteger1/addInteger1")
    check("builtin/semantics/addInteger/addInteger2/addInteger2")
    check("builtin/semantics/addInteger/addInteger3/addInteger3")
    check("builtin/semantics/addInteger/addInteger4/addInteger4")
    check("builtin/semantics/appendByteString/appendByteString1/appendByteString1")
    check("builtin/semantics/appendByteString/appendByteString2/appendByteString2")
    check("builtin/semantics/appendByteString/appendByteString3/appendByteString3")
    check("builtin/semantics/appendString/appendString")
    check("builtin/semantics/bData/bData")
    // check("builtin/semantics/blake2b_224/blake2b_224-empty/blake2b_224-empty")
    // check("builtin/semantics/blake2b_224/blake2b_224-length-200/blake2b_224-length-200")
    check("builtin/semantics/blake2b_256/blake2b_256-empty/blake2b_256-empty")
    check("builtin/semantics/blake2b_256/blake2b_256-length-200/blake2b_256-length-200")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G1/arith/add/add")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G1/arith/neg/neg")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G1/arith/scalarMul/scalarMul")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G1/uncompress/off-curve/off-curve")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G1/uncompress/out-of-group/out-of-group")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G2/arith/add/add")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G2/arith/neg/neg")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G2/arith/scalarMul/scalarMul")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G2/uncompress/off-curve/off-curve")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/G2/uncompress/out-of-group/out-of-group")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/pairing/balanced/balanced")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/pairing/left-additive/left-additive")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/pairing/left-multiplicative/left-multiplicative")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/pairing/right-additive/right-additive")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/pairing/right-multiplicative/right-multiplicative")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/pairing/swap-scalars/swap-scalars")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/signature/augmented/augmented")
    // check("builtin/semantics/bls12_381-cardano-crypto-tests/signature/large-dst/large-dst")
    // check("builtin/semantics/bls12_381_G1_add/add-associative/add-associative")
    // check("builtin/semantics/bls12_381_G1_add/add-commutative/add-commutative")
    // check("builtin/semantics/bls12_381_G1_add/add-zero/add-zero")
    // check("builtin/semantics/bls12_381_G1_add/add/add")
    // check("builtin/semantics/bls12_381_G1_compress/compress/compress")
    // check("builtin/semantics/bls12_381_G1_equal/equal-false/equal-false")
    // check("builtin/semantics/bls12_381_G1_equal/equal-true/equal-true")
    // check("builtin/semantics/bls12_381_G1_hashToGroup/hash-different-msg-same-dst/hash-different-msg-same-dst")
    // check("builtin/semantics/bls12_381_G1_hashToGroup/hash-dst-len-255/hash-dst-len-255")
    // check("builtin/semantics/bls12_381_G1_hashToGroup/hash-dst-len-256/hash-dst-len-256")
    // check("builtin/semantics/bls12_381_G1_hashToGroup/hash-empty-dst/hash-empty-dst")
    // check("builtin/semantics/bls12_381_G1_hashToGroup/hash-same-msg-different-dst/hash-same-msg-different-dst")
    // check("builtin/semantics/bls12_381_G1_hashToGroup/hash/hash")
    // check("builtin/semantics/bls12_381_G1_neg/add-neg/add-neg")
    // check("builtin/semantics/bls12_381_G1_neg/neg-zero/neg-zero")
    // check("builtin/semantics/bls12_381_G1_neg/neg/neg")
    // check("builtin/semantics/bls12_381_G1_scalarMul/addmul/addmul")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mul0/mul0")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mul1/mul1")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mul19+25/mul19+25")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mul44/mul44")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mul4x11/mul4x11")
    // check("builtin/semantics/bls12_381_G1_scalarMul/muladd/muladd")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mulneg1/mulneg1")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mulneg44/mulneg44")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mulperiodic1/mulperiodic1")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mulperiodic2/mulperiodic2")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mulperiodic3/mulperiodic3")
    // check("builtin/semantics/bls12_381_G1_scalarMul/mulperiodic4/mulperiodic4")
    // check("builtin/semantics/bls12_381_G1_uncompress/bad-zero-1/bad-zero-1")
    // check("builtin/semantics/bls12_381_G1_uncompress/bad-zero-2/bad-zero-2")
    // check("builtin/semantics/bls12_381_G1_uncompress/bad-zero-3/bad-zero-3")
    // check("builtin/semantics/bls12_381_G1_uncompress/off-curve/off-curve")
    // check("builtin/semantics/bls12_381_G1_uncompress/on-curve-bit3-clear/on-curve-bit3-clear")
    // check("builtin/semantics/bls12_381_G1_uncompress/on-curve-bit3-set/on-curve-bit3-set")
    // check("builtin/semantics/bls12_381_G1_uncompress/on-curve-serialised-not-compressed/on-curve-serialised-not-compressed")
    // check("builtin/semantics/bls12_381_G1_uncompress/out-of-group/out-of-group")
    // check("builtin/semantics/bls12_381_G1_uncompress/too-long/too-long")
    // check("builtin/semantics/bls12_381_G1_uncompress/too-short/too-short")
    // check("builtin/semantics/bls12_381_G1_uncompress/zero/zero")
    // check("builtin/semantics/bls12_381_G2_add/add-associative/add-associative")
    // check("builtin/semantics/bls12_381_G2_add/add-commutative/add-commutative")
    // check("builtin/semantics/bls12_381_G2_add/add-zero/add-zero")
    // check("builtin/semantics/bls12_381_G2_add/add/add")
    // check("builtin/semantics/bls12_381_G2_compress/compress/compress")
    // check("builtin/semantics/bls12_381_G2_equal/equal-false/equal-false")
    // check("builtin/semantics/bls12_381_G2_equal/equal-true/equal-true")
    // check("builtin/semantics/bls12_381_G2_hashToGroup/hash-different-msg-same-dst/hash-different-msg-same-dst")
    // check("builtin/semantics/bls12_381_G2_hashToGroup/hash-dst-len-255/hash-dst-len-255")
    // check("builtin/semantics/bls12_381_G2_hashToGroup/hash-dst-len-256/hash-dst-len-256")
    // check("builtin/semantics/bls12_381_G2_hashToGroup/hash-empty-dst/hash-empty-dst")
    // check("builtin/semantics/bls12_381_G2_hashToGroup/hash-same-msg-different-dst/hash-same-msg-different-dst")
    // check("builtin/semantics/bls12_381_G2_hashToGroup/hash/hash")
    // check("builtin/semantics/bls12_381_G2_neg/add-neg/add-neg")
    // check("builtin/semantics/bls12_381_G2_neg/neg-zero/neg-zero")
    // check("builtin/semantics/bls12_381_G2_neg/neg/neg")
    // check("builtin/semantics/bls12_381_G2_scalarMul/addmul/addmul")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mul0/mul0")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mul1/mul1")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mul19+25/mul19+25")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mul44/mul44")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mul4x11/mul4x11")
    // check("builtin/semantics/bls12_381_G2_scalarMul/muladd/muladd")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mulneg1/mulneg1")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mulneg44/mulneg44")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mulperiodic1/mulperiodic1")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mulperiodic2/mulperiodic2")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mulperiodic3/mulperiodic3")
    // check("builtin/semantics/bls12_381_G2_scalarMul/mulperiodic4/mulperiodic4")
    // check("builtin/semantics/bls12_381_G2_uncompress/bad-zero-1/bad-zero-1")
    // check("builtin/semantics/bls12_381_G2_uncompress/bad-zero-2/bad-zero-2")
    // check("builtin/semantics/bls12_381_G2_uncompress/bad-zero-3/bad-zero-3")
    // check("builtin/semantics/bls12_381_G2_uncompress/off-curve/off-curve")
    // check("builtin/semantics/bls12_381_G2_uncompress/on-curve-bit3-clear/on-curve-bit3-clear")
    // check("builtin/semantics/bls12_381_G2_uncompress/on-curve-bit3-set/on-curve-bit3-set")
    // check("builtin/semantics/bls12_381_G2_uncompress/on-curve-serialised-not-compressed/on-curve-serialised-not-compressed")
    // check("builtin/semantics/bls12_381_G2_uncompress/out-of-group/out-of-group")
    // check("builtin/semantics/bls12_381_G2_uncompress/too-long/too-long")
    // check("builtin/semantics/bls12_381_G2_uncompress/too-short/too-short")
    // check("builtin/semantics/bls12_381_G2_uncompress/zero/zero")
    // check("builtin/semantics/bls12_381_millerLoop/balanced/balanced")
    // check("builtin/semantics/bls12_381_millerLoop/equal-pairing/equal-pairing")
    // check("builtin/semantics/bls12_381_millerLoop/left-additive/left-additive")
    // check("builtin/semantics/bls12_381_millerLoop/random-pairing/random-pairing")
    // check("builtin/semantics/bls12_381_millerLoop/right-additive/right-additive")
    check("builtin/semantics/chooseDataByteString/chooseDataByteString")
    check("builtin/semantics/chooseDataConstr/chooseDataConstr")
    check("builtin/semantics/chooseDataInteger/chooseDataInteger")
    check("builtin/semantics/chooseDataList/chooseDataList")
    check("builtin/semantics/chooseDataMap/chooseDataMap")
    check("builtin/semantics/chooseList/chooseList1/chooseList1")
    check("builtin/semantics/chooseList/chooseList2/chooseList2")
    check("builtin/semantics/chooseUnit/chooseUnit")
    check("builtin/semantics/consByteString/consByteString1/consByteString1")
    check("builtin/semantics/consByteString/consByteString2/consByteString2")
    check("builtin/semantics/consByteString/consByteString3/consByteString3")
    check("builtin/semantics/constrData/constrData")
    check("builtin/semantics/decodeUtf8/decodeUtf8-invalid/decodeUtf8-invalid")
    check("builtin/semantics/decodeUtf8/decodeUtf8-ok/decodeUtf8-ok")
    check("builtin/semantics/divideInteger/divideInteger-neg-neg/divideInteger-neg-neg")
    check("builtin/semantics/divideInteger/divideInteger-neg-pos/divideInteger-neg-pos")
    check("builtin/semantics/divideInteger/divideInteger-pos-neg/divideInteger-pos-neg")
    check("builtin/semantics/divideInteger/divideInteger-pos-pos/divideInteger-pos-pos")
    check("builtin/semantics/divideInteger/divideInteger-zero/divideInteger-zero")
    check("builtin/semantics/divideInteger/divideInteger1/divideInteger1")
    check("builtin/semantics/encodeUtf8/encodeUtf8")
    check("builtin/semantics/equalsByteString/equalsByteString/equalsByteString")
    check("builtin/semantics/equalsByteString/equalsByteString1/equalsByteString1")
    check("builtin/semantics/equalsByteString/equalsByteString2/equalsByteString2")
    check("builtin/semantics/equalsData/equalsData")
    check("builtin/semantics/equalsInteger/equalsInteger1/equalsInteger1")
    check("builtin/semantics/equalsInteger/equalsInteger2/equalsInteger2")
    check("builtin/semantics/equalsInteger/equalsInteger3/equalsInteger3")
    check("builtin/semantics/equalsString/equalsString1/equalsString1")
    check("builtin/semantics/equalsString/equalsString2/equalsString2")
    check("builtin/semantics/fstPairOfPairAndList/fstPairOfPairAndList")
    check("builtin/semantics/headList/headList1/headList1")
    check("builtin/semantics/headList/headList2/headList2")
    check("builtin/semantics/headList/headList3/headList3")
    check("builtin/semantics/headList/headPartial/headPartial")
    check("builtin/semantics/iData/iData")
    check("builtin/semantics/ifThenElse/ifThenElse-1/ifThenElse-1")
    check("builtin/semantics/ifThenElse/ifThenElse-2/ifThenElse-2")
    check("builtin/semantics/ifThenElse/ifThenElse-3/ifThenElse-3")
    check("builtin/semantics/ifThenElse/ifThenElse-4/ifThenElse-4")
    check("builtin/semantics/ifThenElse/ifThenElse-no-force/ifThenElse-no-force")
    check("builtin/semantics/indexByteString/indexByteString1/indexByteString1")
    check("builtin/semantics/indexByteString/indexByteStringOOB/indexByteStringOOB")
    check("builtin/semantics/indexByteString/indexByteStringOverflow/indexByteStringOverflow")
    // check("builtin/semantics/keccak_256/keccak_256-empty/keccak_256-empty")
    // check("builtin/semantics/keccak_256/keccak_256-length-200/keccak_256-length-200")
    check("builtin/semantics/lengthOfByteString/lengthOfByteString")
    check("builtin/semantics/lessThanByteString/lessThanByteString0/lessThanByteString0")
    check("builtin/semantics/lessThanByteString/lessThanByteString1/lessThanByteString1")
    check("builtin/semantics/lessThanByteString/lessThanByteString2/lessThanByteString2")
    check("builtin/semantics/lessThanByteString/lessThanByteString3/lessThanByteString3")
    check("builtin/semantics/lessThanByteString/lessThanByteString4/lessThanByteString4")
    check("builtin/semantics/lessThanByteString/lessThanByteString5/lessThanByteString5")
    check(
      "builtin/semantics/lessThanEqualsByteString/lessThanEqualsByteString0/lessThanEqualsByteString0"
    )
    check(
      "builtin/semantics/lessThanEqualsByteString/lessThanEqualsByteString1/lessThanEqualsByteString1"
    )
    check(
      "builtin/semantics/lessThanEqualsByteString/lessThanEqualsByteString2/lessThanEqualsByteString2"
    )
    check(
      "builtin/semantics/lessThanEqualsByteString/lessThanEqualsByteString3/lessThanEqualsByteString3"
    )
    check("builtin/semantics/lessThanEqualsInteger/lessThanEqualsInteger1/lessThanEqualsInteger1")
    check("builtin/semantics/lessThanEqualsInteger/lessThanEqualsInteger2/lessThanEqualsInteger2")
    check("builtin/semantics/lessThanEqualsInteger/lessThanEqualsInteger3/lessThanEqualsInteger3")
    check("builtin/semantics/lessThanEqualsInteger/lessThanEqualsInteger4/lessThanEqualsInteger4")
    check("builtin/semantics/lessThanEqualsInteger/lessThanEqualsInteger5/lessThanEqualsInteger5")
    check("builtin/semantics/lessThanInteger/lessThanInteger1/lessThanInteger1")
    check("builtin/semantics/lessThanInteger/lessThanInteger2/lessThanInteger2")
    check("builtin/semantics/lessThanInteger/lessThanInteger3/lessThanInteger3")
    check("builtin/semantics/lessThanInteger/lessThanInteger4/lessThanInteger4")
    check("builtin/semantics/lessThanInteger/lessThanInteger5/lessThanInteger5")
    check("builtin/semantics/listData/listData")
    check("builtin/semantics/listOfList/listOfList")
    check("builtin/semantics/listOfPair/listOfPair")
    check("builtin/semantics/mapData/mapData")
    check("builtin/semantics/mkCons/divideInteger/divideInteger")
    check("builtin/semantics/mkCons/mkCons-fail/mkCons-fail")
    check("builtin/semantics/mkCons/mkCons1/mkCons1")
    check("builtin/semantics/mkCons/mkCons2/mkCons2")
    check("builtin/semantics/mkNilData/mkNilData")
    check("builtin/semantics/mkNilPairData/mkNilPairData")
    check("builtin/semantics/mkPairData/mkPairData")
    check("builtin/semantics/modInteger/modInteger-neg-neg/modInteger-neg-neg")
    check("builtin/semantics/modInteger/modInteger-neg-pos/modInteger-neg-pos")
    check("builtin/semantics/modInteger/modInteger-pos-neg/modInteger-pos-neg")
    check("builtin/semantics/modInteger/modInteger-pos-pos/modInteger-pos-pos")
    check("builtin/semantics/modInteger/modInteger-zero/modInteger-zero")
    check("builtin/semantics/modInteger/modInteger1/modInteger1")
    check("builtin/semantics/multiplyInteger/multiplyInteger1/multiplyInteger1")
    check("builtin/semantics/multiplyInteger/multiplyInteger2/multiplyInteger2")
    check("builtin/semantics/multiplyInteger/multiplyInteger3/multiplyInteger3")
    check("builtin/semantics/multiplyInteger/multiplyInteger4/multiplyInteger4")
    check("builtin/semantics/multiplyInteger/multiplyInteger5/multiplyInteger5")
    check("builtin/semantics/multiplyInteger/multiplyInteger6/multiplyInteger6")
    check("builtin/semantics/nullList/nullList")
    check("builtin/semantics/nullList2/nullList2")
    check("builtin/semantics/pairOfPairAndList/pairOfPairAndList")
    check("builtin/semantics/quotientInteger/quotientInteger-neg-neg/quotientInteger-neg-neg")
    check("builtin/semantics/quotientInteger/quotientInteger-neg-pos/quotientInteger-neg-pos")
    check("builtin/semantics/quotientInteger/quotientInteger-pos-neg/quotientInteger-pos-neg")
    check("builtin/semantics/quotientInteger/quotientInteger-pos-pos/quotientInteger-pos-pos")
    check("builtin/semantics/quotientInteger/quotientInteger-zero/quotientInteger-zero")
    check("builtin/semantics/quotientInteger/quotientInteger1/quotientInteger1")
    check("builtin/semantics/remainderInteger/remainderInteger-neg-neg/remainderInteger-neg-neg")
    check("builtin/semantics/remainderInteger/remainderInteger-neg-pos/remainderInteger-neg-pos")
    check("builtin/semantics/remainderInteger/remainderInteger-pos-neg/remainderInteger-pos-neg")
    check("builtin/semantics/remainderInteger/remainderInteger-pos-pos/remainderInteger-pos-pos")
    check("builtin/semantics/remainderInteger/remainderInteger-zero/remainderInteger-zero")
    check("builtin/semantics/remainderInteger/remainderInteger1/remainderInteger1")
    check("builtin/semantics/sha2_256/sha2_256-empty/sha2_256-empty")
    check("builtin/semantics/sha2_256/sha2_256-length-200/sha2_256-length-200")
    check("builtin/semantics/sha3_256/sha3_256-empty/sha3_256-empty")
    check("builtin/semantics/sha3_256/sha3_256-length-200/sha3_256-length-200")
    check("builtin/semantics/sliceByteString/sliceByteString1/sliceByteString1")
    check("builtin/semantics/sliceByteString/sliceByteString2/sliceByteString2")
    check("builtin/semantics/sliceByteString/sliceByteString3/sliceByteString3")
    check("builtin/semantics/sliceByteString/sliceByteString4/sliceByteString4")
    check("builtin/semantics/sliceByteString/sliceByteString5/sliceByteString5")
    check("builtin/semantics/sndPairOfPairAndList/sndPairOfPairAndList")
    check("builtin/semantics/subtractInteger-non-iter/subtractInteger-non-iter")
    check("builtin/semantics/subtractInteger/subtractInteger1/subtractInteger1")
    check("builtin/semantics/subtractInteger/subtractInteger2/subtractInteger2")
    check("builtin/semantics/subtractInteger/subtractInteger3/subtractInteger3")
    check("builtin/semantics/subtractInteger/subtractInteger4/subtractInteger4")
    check("builtin/semantics/tailList/tailList-partial/tailList-partial")
    check("builtin/semantics/tailList/tailList1/tailList1")
    check("builtin/semantics/trace/trace")
    check("builtin/semantics/unBData/unBData-fail/unBData-fail")
    check("builtin/semantics/unBData/unBData1/unBData1")
    check("builtin/semantics/unConstrData/unConstrData-fail/unConstrData-fail")
    check("builtin/semantics/unConstrData/unConstrData1/unConstrData1")
    check("builtin/semantics/unIData/unIData-fail/unIData-fail")
    check("builtin/semantics/unIData/unIData1/unIData1")
    check("builtin/semantics/unListData/unListData-fail/unListData-fail")
    check("builtin/semantics/unListData/unListData1/unListData1")
    check("builtin/semantics/unMapData/unMapData-fail/unMapData-fail")
    check("builtin/semantics/unMapData/unMapData1/unMapData1")
    check(
      "builtin/semantics/verifyEcdsaSecp256k1Signature/verifyEcdsaSecp256k1Signature-invalid-key/verifyEcdsaSecp256k1Signature-invalid-key"
    )
    check(
      "builtin/semantics/verifyEcdsaSecp256k1Signature/verifyEcdsaSecp256k1Signature-long-key/verifyEcdsaSecp256k1Signature-long-key"
    )
    check(
      "builtin/semantics/verifyEcdsaSecp256k1Signature/verifyEcdsaSecp256k1Signature-long-msg/verifyEcdsaSecp256k1Signature-long-msg"
    )
    check(
      "builtin/semantics/verifyEcdsaSecp256k1Signature/verifyEcdsaSecp256k1Signature-long-sig/verifyEcdsaSecp256k1Signature-long-sig"
    )
    check(
      "builtin/semantics/verifyEcdsaSecp256k1Signature/verifyEcdsaSecp256k1Signature-short-key/verifyEcdsaSecp256k1Signature-short-key"
    )
    check(
      "builtin/semantics/verifyEcdsaSecp256k1Signature/verifyEcdsaSecp256k1Signature-short-msg/verifyEcdsaSecp256k1Signature-short-msg"
    )
    check(
      "builtin/semantics/verifyEcdsaSecp256k1Signature/verifyEcdsaSecp256k1Signature-short-sig/verifyEcdsaSecp256k1Signature-short-sig"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature1/verifyEd25519Signature1"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature10/verifyEd25519Signature10"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature11/verifyEd25519Signature11"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature12/verifyEd25519Signature12"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature13/verifyEd25519Signature13"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature14/verifyEd25519Signature14"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature15/verifyEd25519Signature15"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature16/verifyEd25519Signature16"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature17/verifyEd25519Signature17"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature18/verifyEd25519Signature18"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature19/verifyEd25519Signature19"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature2/verifyEd25519Signature2"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature20/verifyEd25519Signature20"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature21/verifyEd25519Signature21"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature22/verifyEd25519Signature22"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature23/verifyEd25519Signature23"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature24/verifyEd25519Signature24"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature25/verifyEd25519Signature25"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature26/verifyEd25519Signature26"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature27/verifyEd25519Signature27"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature28/verifyEd25519Signature28"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature29/verifyEd25519Signature29"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature3/verifyEd25519Signature3"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature30/verifyEd25519Signature30"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature31/verifyEd25519Signature31"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature4/verifyEd25519Signature4"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature5/verifyEd25519Signature5"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature6/verifyEd25519Signature6"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature7/verifyEd25519Signature7"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature8/verifyEd25519Signature8"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519Signature9/verifyEd25519Signature9"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519SignatureLongKey/verifyEd25519SignatureLongKey"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519SignatureLongSig/verifyEd25519SignatureLongSig"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519SignatureShortKey/verifyEd25519SignatureShortKey"
    )
    check(
      "builtin/semantics/verifyEd25519Signature/verifyEd25519SignatureShortSig/verifyEd25519SignatureShortSig"
    )
    check(
      "builtin/semantics/verifySchnorrSecp256k1Signature/verifySchnorrSecp256k1Signature-long-key/verifySchnorrSecp256k1Signature-long-key"
    )
    check(
      "builtin/semantics/verifySchnorrSecp256k1Signature/verifySchnorrSecp256k1Signature-long-sig/verifySchnorrSecp256k1Signature-long-sig"
    )
    check(
      "builtin/semantics/verifySchnorrSecp256k1Signature/verifySchnorrSecp256k1Signature-short-key/verifySchnorrSecp256k1Signature-short-key"
    )
    check(
      "builtin/semantics/verifySchnorrSecp256k1Signature/verifySchnorrSecp256k1Signature-short-sig/verifySchnorrSecp256k1Signature-short-sig"
    )

    // constr/case
    check("term/app/app-1/app-1")
    check("term/app/app-2/app-2")
    check("term/app/app-3/app-3")
    check("term/app/app-4/app-4")
    check("term/app/app-5/app-5")
    check("term/app/app-6/app-6")
    check("term/app/app-7/app-7")
    check("term/app/app-8/app-8")
    check("term/app/app-9/app-9")
    check("term/argExpected/argExpected")
    check("term/case/case-1/case-1")
    check("term/case/case-2/case-2")
    check("term/case/case-3/case-3")
    check("term/case/case-4/case-4")
    check("term/case/case-5/case-5")
    check("term/case/case-6/case-6")
    check("term/case/case-7/case-7")
    check("term/case/case-8/case-8")
    check("term/case/case-9/case-9")
    check("term/closure/closure")
    check("term/constr/constr-1/constr-1")
    check("term/constr/constr-2/constr-2")
    check("term/constr/constr-3/constr-3")
    check("term/constr/constr-4/constr-4")
    check("term/constr/constr-5/constr-5")
    check("term/constr/constr-6/constr-6")
    check("term/delay/delay-error-1/delay-error-1")
    check("term/delay/delay-error-2/delay-error-2")
    check("term/delay/delay-lam/delay-lam")
    check("term/force/force-1/force-1")
    check("term/force/force-2/force-2")
    check("term/force/force-3/force-3")
    check("term/force/force-4/force-4")
    check("term/lam/lam-1/lam-1")
    check("term/lam/lam-2/lam-2")
    check("term/nonFunctionalApplication/nonFunctionalApplication")
    check("term/unlifting-sat/unlifting-sat")
    check("term/unlifting-unsat/unlifting-unsat")
    check("term/var/var")

    // Examples
    check("example/ApplyAdd1/ApplyAdd1")
    check("example/ApplyAdd2/ApplyAdd2")
    check("example/DivideByZero/DivideByZero")
    check("example/DivideByZeroDrop/DivideByZeroDrop")
    check("example/IfIntegers/IfIntegers")
    check("example/NatRoundTrip/NatRoundTrip")
    check("example/ScottListSum/ScottListSum")
    check("example/churchSucc/churchSucc")
    check("example/churchZero/churchZero")
    check("example/even2/even2")
    check("example/even3/even3")
    check("example/evenList/evenList")
    check("example/factorial/factorial")
    check("example/fibonacci/fibonacci")
    check("example/force-lam/force-lam")
    check("example/overapplication/overapplication")
    check("example/succInteger/succInteger")
