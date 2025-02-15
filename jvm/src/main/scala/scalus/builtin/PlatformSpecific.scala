package scalus.builtin

import org.bitcoin.NativeSecp256k1
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.bouncycastle.jcajce.provider.digest.Keccak
import org.bouncycastle.jcajce.provider.digest.SHA3
import scalus.utils.Utils
import supranational.blst.P1
import supranational.blst.P2
import supranational.blst.PT

import java.math.BigInteger

object JVMPlatformSpecific extends JVMPlatformSpecific
trait JVMPlatformSpecific extends PlatformSpecific {
    val scalarPeriod: BigInt =
        BigInt("73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", 16)
    override def sha2_256(bs: ByteString): ByteString =
        ByteString.unsafeFromArray(Utils.sha2_256(bs.bytes))

    override def sha3_256(bs: ByteString): ByteString =
        val digestSHA3 = new SHA3.Digest256()
        ByteString.unsafeFromArray(digestSHA3.digest(bs.bytes))

    override def blake2b_224(bs: ByteString): ByteString =
        val digest = new Blake2bDigest(224)
        digest.update(bs.bytes, 0, bs.length)
        val hash = new Array[Byte](digest.getDigestSize)
        digest.doFinal(hash, 0)
        ByteString.unsafeFromArray(hash)

    override def blake2b_256(bs: ByteString): ByteString =
        val digest = new Blake2bDigest(256)
        digest.update(bs.bytes, 0, bs.length)
        val hash = new Array[Byte](digest.getDigestSize)
        digest.doFinal(hash, 0)
        ByteString.unsafeFromArray(hash)

    override def verifySchnorrSecp256k1Signature(
        pk: ByteString,
        msg: ByteString,
        sig: ByteString
    ): Boolean = {
        require(pk.length == 32, s"Invalid public key length ${pk.length}")
        // parity byte 0x02 for compressed public key
        require(NativeSecp256k1.isValidPubKey(0x02 +: pk.bytes), s"Invalid public key ${pk}")
        require(sig.length == 64, s"Invalid signature length ${sig.length}")
        NativeSecp256k1.schnorrVerify(sig.bytes, msg.bytes, pk.bytes)
    }

    override def verifyEd25519Signature(pk: ByteString, msg: ByteString, sig: ByteString): Boolean =
        require(pk.length == 32, s"Invalid public key length ${pk.length}")
        require(sig.length == 64, s"Invalid signature length ${sig.length}")
        val pubKeyParams =
            try new Ed25519PublicKeyParameters(pk.bytes, 0)
            catch
                case e: IllegalArgumentException =>
                    return false
        val verifier = new Ed25519Signer()
        verifier.init(false, pubKeyParams)
        verifier.update(msg.bytes, 0, msg.length)
        verifier.verifySignature(sig.bytes)

    override def verifyEcdsaSecp256k1Signature(
        pk: ByteString,
        msg: ByteString,
        sig: ByteString
    ): Boolean = {
        require(
          pk.length == 33,
          s"Invalid public key length ${pk.length}, expected 33, ${pk.toHex}"
        )
        require(NativeSecp256k1.isValidPubKey(pk.bytes), s"Invalid public key ${pk}")
        require(msg.length == 32, s"Invalid message length ${msg.length}, expected 32")
        require(sig.length == 64, s"Invalid signature length ${sig.length}, expected 64")

        val r = BigInt(new BigInteger(1, sig.bytes, 0, 32)) // avoid copying the array
        val s = BigInt(new BigInteger(1, sig.bytes, 32, 32)) // avoid copying the array
        val rsSize = r.toByteArray.length + s.toByteArray.length
        val totalSize = 4 + rsSize
        val signature = Array(
          0x30.toByte,
          totalSize.toByte,
          0x2.toByte,
          r.toByteArray.length.toByte
        ) ++ r.toByteArray ++ Array(0x2.toByte, s.toByteArray.length.toByte) ++ s.toByteArray
        NativeSecp256k1.verify(msg.bytes, signature, pk.bytes)
    }

    // BLS12_381 operations
    override def bls12_381_G1_equal(p1: BLS12_381_G1_Element, p2: BLS12_381_G1_Element): Boolean = {
        p1.p.is_equal(p2.p)
    }

    override def bls12_381_G1_add(
        p1: BLS12_381_G1_Element,
        p2: BLS12_381_G1_Element
    ): BLS12_381_G1_Element = BLS12_381_G1_Element(p1.p.add(p2.p))

    override def bls12_381_G1_scalarMul(
        s: BigInt,
        p: BLS12_381_G1_Element
    ): BLS12_381_G1_Element = {
        val scalar = s.bigInteger.mod(scalarPeriod.bigInteger)
        BLS12_381_G1_Element(p.p.mult(scalar))
    }

    override def bls12_381_G1_neg(
        p: BLS12_381_G1_Element
    ): BLS12_381_G1_Element = {
        BLS12_381_G1_Element(p.p.neg())
    }

    override def bls12_381_G1_compress(p: BLS12_381_G1_Element): ByteString = {
        ByteString.fromArray(p.p.compress())
    }

    override def bls12_381_G1_uncompress(bs: ByteString): BLS12_381_G1_Element = {
        if bs.length != 48 then throw new IllegalArgumentException("Not a compressed point")
        if (bs.bytes(0) & 0x80) == 0 then // compressed bit not set
            throw new IllegalArgumentException(s"BSL point not compressed: $bs")
        val p = new P1(bs.bytes)
        if !p.in_group() then throw new IllegalArgumentException("Invalid point")
        BLS12_381_G1_Element(p)
    }

    override def bls12_381_G1_hashToGroup(bs: ByteString, dst: ByteString): BLS12_381_G1_Element = {
        if dst.length > 255 then throw RuntimeException(s"HashToCurveDstTooBig: ${dst.length}")
        else
            val p = new P1()
            p.hash_to(bs.bytes, new String(dst.bytes, "Latin1"))
            BLS12_381_G1_Element(p)
    }

    override val bls12_381_G1_compressed_generator: ByteString = {
        ByteString.fromArray(P1.generator().compress())
    }

    override def bls12_381_G2_equal(p1: BLS12_381_G2_Element, p2: BLS12_381_G2_Element): Boolean = {
        p1.p.is_equal(p2.p)
    }

    override def bls12_381_G2_add(
        p1: BLS12_381_G2_Element,
        p2: BLS12_381_G2_Element
    ): BLS12_381_G2_Element = BLS12_381_G2_Element(p1.p.add(p2.p))

    override def bls12_381_G2_scalarMul(
        s: BigInt,
        p: BLS12_381_G2_Element
    ): BLS12_381_G2_Element = {
        val scalar = s.bigInteger.mod(scalarPeriod.bigInteger)
        BLS12_381_G2_Element(p.p.mult(scalar))
    }

    override def bls12_381_G2_neg(
        p: BLS12_381_G2_Element
    ): BLS12_381_G2_Element = {
        BLS12_381_G2_Element(p.p.neg())
    }

    override def bls12_381_G2_compress(p: BLS12_381_G2_Element): ByteString = {
        ByteString.fromArray(p.p.compress())
    }

    override def bls12_381_G2_uncompress(bs: ByteString): BLS12_381_G2_Element = {
        if bs.length != 96 then throw new IllegalArgumentException("Not a compressed point")
        if (bs.bytes(0) & 0x80) == 0 then // compressed bit not set
            throw new IllegalArgumentException(s"BSL point not compressed: $bs")
        val p = new P2(bs.bytes)
        if !p.in_group() then throw new IllegalArgumentException("Invalid point")
        BLS12_381_G2_Element(p)
    }

    override def bls12_381_G2_hashToGroup(bs: ByteString, dst: ByteString): BLS12_381_G2_Element = {
        if dst.length > 255 then throw RuntimeException(s"HashToCurveDstTooBig: ${dst.length}")
        else
            val p = new P2()
            p.hash_to(bs.bytes, new String(dst.bytes, "Latin1"))
            BLS12_381_G2_Element(p)
    }

    override def bls12_381_G2_compressed_generator: ByteString = {
        ByteString.fromArray(P2.generator().compress())
    }

    override def bls12_381_millerLoop(
        p1: BLS12_381_G1_Element,
        p2: BLS12_381_G2_Element
    ): BLS12_381_MlResult = {
        val pt = new PT(p1.p, p2.p)
        BLS12_381_MlResult(pt)
    }

    override def bls12_381_mulMlResult(
        r1: BLS12_381_MlResult,
        r2: BLS12_381_MlResult
    ): BLS12_381_MlResult = {
        val pt = r1.p.mul(r2.p)
        BLS12_381_MlResult(pt)
    }

    override def bls12_381_finalVerify(p1: BLS12_381_MlResult, p2: BLS12_381_MlResult): Boolean = {
        PT.finalverify(p1.p, p2.p)
    }

    override def keccak_256(bs: ByteString): ByteString = {
        val digest = new Keccak.Digest256()
        ByteString.unsafeFromArray(digest.digest(bs.bytes))
    }
}

given PlatformSpecific = JVMPlatformSpecific
