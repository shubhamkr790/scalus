package scalus.sir

import scalus.*
import scalus.builtin.*
import scalus.prelude.*
import scalus.ledger.api.v3.*
import org.scalatest.funsuite.AnyFunSuite
import scalus.ledger.api.v2.OutputDatum





class SIRTypeSerializationSpec extends AnyFunSuite {

    def encodeDecodeSIR(sir: SIR): SIR = {
       import scalus.flat.*
       val bitSize = ToExprHSSIRFlat.bitSize(sir)
       val encoded = EncoderState(bitSize / 8 + 1)
       ToExprHSSIRFlat.encode(sir, encoded)
       val decoded = ToExprHSSIRFlat.decode(DecoderState(encoded.buffer))
       decoded
    }

    def encodeDecodeSIRType(sirType: SIRType): SIRType = {
        import scalus.flat.*
        val bitSize = ToExprHSSIRTypeFlat.bitSize(sirType)
        val encoded = EncoderState (bitSize / 8 + 1)
        ToExprHSSIRTypeFlat.encode (sirType, encoded)
        val decoded = ToExprHSSIRTypeFlat.decode (DecoderState (encoded.buffer) )
        decoded
    }


    test("check order of parameters in TxnInfo") {
        val sir = Compiler.compile {
            // TODO:  resolve apply for case-classes.
            val txIdPrev = new TxId(ByteString.fromString("0x12345678"))
            val outRef = new TxOutRef(txIdPrev, 0)
            val credential1 = new Credential.PubKeyCredential(new PubKeyHash(ByteString.fromString("pkh1")))
            val addressIn = new Address(credential = credential1, stakingCredential = Maybe.Nothing)
            val valueIn = Value(ByteString.empty, ByteString.empty, 4)
            val outRefResolved = new TxOut(address=addressIn, value=valueIn, datum = OutputDatum.NoOutputDatum, referenceScript=Maybe.Nothing)
            val inputs = scalus.prelude.List.single(new TxInInfo(outRef, outRefResolved))
            val credential2 = new Credential.PubKeyCredential(new PubKeyHash(ByteString.fromString("5")))
            val addressOut = new Address(credential = credential2, stakingCredential = Maybe.Nothing)
            val valueOut = Value(ByteString.empty, ByteString.empty, 2)
            val outputs = scalus.prelude.List.single(new TxOut(addressOut, valueOut, OutputDatum.NoOutputDatum, Maybe.Nothing))
            val txId = new TxId(ByteString.fromString("0x123456789"))
            val fee = BigInt(2)
            val mint = Value.zero
            def txInfo = new TxInfo(
                          inputs=inputs,
                          referenceInputs = scalus.prelude.List.empty,
                          outputs = outputs,
                          fee = fee,
                          mint = mint,
                          certificates = scalus.prelude.List.empty,
                          withdrawals = AssocMap.empty,
                          validRange = Interval.always,
                          signatories = scalus.prelude.List.empty,
                          redeemers = AssocMap.empty,
                          data = AssocMap.empty,
                          id = txId,
                          votes = AssocMap.empty,
                          proposalProcedures = scalus.prelude.List.empty,
                          currentTreasuryAmount = Maybe.Nothing,
                          treasuryDonation = Maybe.Nothing)
            txInfo
        }
        //println(s"sir=${sir}")
        //println(sir.pretty.render(100))
        //println(s"sir.tp=${sir.tp.show}")
        val txInfoSIRType = sir.tp match
            case SIRType.Fun(u,tp) if u == SIRType.VoidPrimitive => tp
            case _ => fail(s"unexpected type ${sir.tp.show}")
        val constrDecl = txInfoSIRType match
            case SIRType.CaseClass(constrDecl, typeArgs) => constrDecl
            case SIRType.SumCaseClass(dataDecl, typeArgs) =>
                if (dataDecl.constructors.length == 1) then
                    val constrDecl = dataDecl.constructors.head
                    constrDecl
                else
                    fail(s"expected single case class, we have ${dataDecl.constructors.length} case classes")
            case _ => fail(s"case class expected, we have ${txInfoSIRType.show}: ${txInfoSIRType}")
        //println(s"constrDecl=${constrDecl}")
        val params = constrDecl.params
        // should be the same as in definition.
        assert(params(0).name == "inputs")
        assert(params(1).name == "referenceInputs")
        assert(params(2).name == "outputs")
        assert(params(3).name == "fee")
        assert(params(4).name == "mint")
        assert(params(5).name == "certificates")
        assert(params(6).name == "withdrawals")
        assert(params(7).name == "validRange")
        assert(params(8).name == "signatories")
        assert(params(9).name == "redeemers")
        assert(params(10).name == "data")
        assert(params(11).name == "id")
        assert(params(12).name == "votes")
        assert(params(13).name == "proposalProcedures")
        assert(params(14).name == "currentTreasuryAmount")
        assert(params(15).name == "treasuryDonation")
        val tp1 = encodeDecodeSIRType(txInfoSIRType)
        val constrDecl1 = tp1 match
            case SIRType.CaseClass(constrDecl, typeArgs) => constrDecl
            case SIRType.SumCaseClass(dataDecl, typeArgs) =>
                if (dataDecl.constructors.length == 1) then
                    val constrDecl = dataDecl.constructors.head
                    constrDecl
                else
                    fail(s"expected single case class, we have ${dataDecl.constructors.length} case classes")
            case _ => fail(s"case class expected, we have ${tp1.show}: ${tp1}")
        val params1 = constrDecl1.params
        // should be the same as in definition.
        assert(params1(0).name == "inputs")
        assert(params1(1).name == "referenceInputs")
        assert(params1(2).name == "outputs")
        assert(params1(3).name == "fee")
        assert(params1(4).name == "mint")
        assert(params1(5).name == "certificates")
        assert(params1(6).name == "withdrawals")
        assert(params1(7).name == "validRange")
        assert(params1(8).name == "signatories")
    }



}
