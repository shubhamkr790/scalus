package scalus.ledger.api.v1

import scalus.Compile
import scalus.builtins.Builtins
import scalus.builtins.ByteString
import scalus.prelude.AssocMap
import scalus.prelude.List
import scalus.prelude.Maybe
import scalus.prelude.Prelude.===
import scalus.prelude.Prelude.Eq
import scalus.prelude.Prelude.given
import scalus.prelude.These.*
import scalus.uplc.Data
import scalus.uplc.Data.FromData
import scalus.uplc.Data.fromData

type ValidatorHash = ByteString
type Datum = Data
type DatumHash = ByteString
type Redeemer = Data
type ScriptHash = ByteString
type RedeemerHash = ByteString
type CurrencySymbol = ByteString
type TokenName = ByteString
type POSIXTime = BigInt
type POSIXTimeRange = Interval[POSIXTime]
type Value = AssocMap[CurrencySymbol, AssocMap[TokenName, BigInt]]

@Compile
object FromDataInstances {
  import scalus.uplc.FromDataInstances.given

  implicit def fromDataTxId(d: Data): TxId =
    val hash = fromData[ByteString](Builtins.unsafeDataAsConstr(d).snd.head)
    new TxId(hash)

  implicit def fromDataPubKeyHash(d: Data): PubKeyHash =
    val hash = fromData[ByteString](d)
    new PubKeyHash(hash)

  implicit def fromDataTxOutRef(d: Data): TxOutRef =
    val args = Builtins.unsafeDataAsConstr(d).snd
    val txidx = args.tail
    new TxOutRef(
      fromData[TxId](args.head),
      fromData[BigInt](txidx.head)
    )

  given FromData[DCert] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val tag = pair.fst
    val args = pair.snd
    if tag === BigInt(0) then new DCert.DelegRegKey(fromData[StakingCredential](args.head))
    else if tag === BigInt(1) then new DCert.DelegDeRegKey(fromData[StakingCredential](args.head))
    else if tag === BigInt(2) then
      new DCert.DelegDelegate(
        fromData[StakingCredential](args.head),
        fromData[PubKeyHash](args.tail.head)
      )
    else if tag === BigInt(3) then
      new DCert.PoolRegister(
        fromData[PubKeyHash](args.head),
        fromData[PubKeyHash](args.tail.head)
      )
    else if tag === BigInt(4) then
      new DCert.PoolRetire(
        fromData[PubKeyHash](args.head),
        fromData[BigInt](args.tail.head)
      )
    else if tag === BigInt(5) then DCert.Genesis
    else if tag === BigInt(6) then DCert.Mir
    else throw new Exception(s"Unknown DCert tag: $tag")

  given ExtendedFromData[A: FromData]: FromData[Extended[A]] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val tag = pair.fst
    val args = pair.snd
    if tag === BigInt(0) then Extended.NegInf
    else if tag === BigInt(1) then new Extended.Finite(fromData[A](args.head))
    else if tag === BigInt(2) then Extended.PosInf
    else throw new Exception(s"Unknown Extended tag: $tag")

  given FromData[Credential] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val tag = pair.fst
    val args = pair.snd
    if tag === BigInt(0) then new Credential.PubKeyCredential(fromData[PubKeyHash](args.head))
    else if tag === BigInt(1) then new Credential.ScriptCredential(fromData[ByteString](args.head))
    else throw new Exception(s"Unknown Credential tag: $tag")

  given FromData[StakingCredential] =
    (d: Data) =>
      val pair = Builtins.unsafeDataAsConstr(d)
      val tag = pair.fst
      if tag === BigInt(0) then
        new StakingCredential.StakingHash(fromData[Credential](pair.snd.head))
      else if tag === BigInt(1) then
        val fromBI = summon[FromData[BigInt]]
        val ptrs = pair.snd
        new StakingCredential.StakingPtr(
          fromBI(pair.snd.head),
          fromBI(pair.snd.tail.head),
          fromBI(pair.snd.tail.tail.head)
        )
      else throw new RuntimeException("Invalid tag")

  given FromData[ScriptPurpose] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val tag = pair.fst
    val args = pair.snd
    if tag === BigInt(0) then new ScriptPurpose.Minting(fromData[TokenName](args.head))
    else if tag === BigInt(1) then new ScriptPurpose.Spending(fromData[TxOutRef](args.head))
    else if tag === BigInt(2) then
      new ScriptPurpose.Rewarding(fromData[StakingCredential](args.head))
    else if tag === BigInt(3) then new ScriptPurpose.Certifying(fromData[DCert](args.head))
    else throw new Exception(s"Unknown ScriptPurpose tag: $tag")

  given FromData[Address] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    new Address(
      fromData[Credential](pair.snd.head),
      fromData[Maybe[StakingCredential]](pair.snd.tail.head)
    )

  given FromData[TxOut] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val args = pair.snd
    new TxOut(
      fromData[Address](args.head),
      fromData[Value](args.tail.head),
      fromData[Maybe[DatumHash]](args.tail.tail.head)
    )

  given FromData[TxInInfo] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val args = pair.snd
    new TxInInfo(
      fromData[TxOutRef](args.head),
      fromData[TxOut](args.tail.head)
    )

  given UpperBoundFromData[A: FromData]: FromData[UpperBound[A]] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val args = pair.snd
    new UpperBound(
      fromData[Extended[A]](args.head),
      fromData[Closure](args.tail.head)
    )

  given LowerBoundFromData[A: FromData]: FromData[LowerBound[A]] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val args = pair.snd
    new LowerBound(
      fromData[Extended[A]](args.head),
      fromData[Closure](args.tail.head)
    )

  given IntervalFromData[A: FromData]: FromData[Interval[A]] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val args = pair.snd
    new Interval(
      fromData[LowerBound[A]](args.head),
      fromData[UpperBound[A]](args.tail.head)
    )

    /*
     txInfoInputs: List[TxInInfo],
    txInfoOutputs: List[TxOut],
    txInfoFee: Value,
    txInfoMint: Value,
    txInfoDCert: List[DCert],
    txInfoWdrl: List[(StakingCredential, BigInt)],
    txInfoValidRange: POSIXTimeRange,
    txInfoSignatories: List[PubKeyHash],
    txInfoData: List[(DatumHash, Datum)],
    txInfoId: TxId */
  given FromData[TxInfo] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val args = pair.snd
    val fromValue = summon[FromData[Value]]
    new TxInfo(
      fromData[List[TxInInfo]](args.head),
      fromData[List[TxOut]](args.tail.head),
      fromValue(args.tail.tail.head),
      fromValue(args.tail.tail.tail.head),
      fromData[List[DCert]](args.tail.tail.tail.tail.head),
      fromData[List[(StakingCredential, BigInt)]](args.tail.tail.tail.tail.tail.head),
      fromData[Interval[POSIXTime]](args.tail.tail.tail.tail.tail.tail.head),
      fromData[List[PubKeyHash]](args.tail.tail.tail.tail.tail.tail.tail.head),
      fromData[List[(DatumHash, Datum)]](args.tail.tail.tail.tail.tail.tail.tail.tail.head),
      fromData[TxId](args.tail.tail.tail.tail.tail.tail.tail.tail.tail.head)
    )

  given FromData[ScriptContext] = (d: Data) =>
    val pair = Builtins.unsafeDataAsConstr(d)
    val args = pair.snd
    new ScriptContext(
      fromData[TxInfo](args.head),
      fromData[ScriptPurpose](args.tail.head)
    )

}

type Closure = Boolean
enum Extended[+A]:
  case NegInf extends Extended[Nothing]
  case Finite(a: A)
  case PosInf extends Extended[Nothing]

case class UpperBound[A](upper: Extended[A], closure: Closure)
case class LowerBound[A](extended: Extended[A], closure: Closure)
case class Interval[A](from: LowerBound[A], to: UpperBound[A])

@Compile
object Interval:
  def always[A]: Interval[A] =
    new Interval(new LowerBound(Extended.NegInf, true), new UpperBound(Extended.PosInf, true))

enum DCert:
  case DelegRegKey(cred: StakingCredential)
  case DelegDeRegKey(cred: StakingCredential)
  case DelegDelegate(cred: StakingCredential, delegatee: PubKeyHash)
  case PoolRegister(poolId: PubKeyHash, vrf: PubKeyHash)
  case PoolRetire(poolId: PubKeyHash, epoch: BigInt)
  case Genesis
  case Mir

@Compile
object DCert {
  given Eq[DCert] = (x: DCert, y: DCert) =>
    x match
      case DCert.DelegRegKey(cred) =>
        y match
          case DCert.DelegRegKey(cred)              => cred === cred
          case DCert.DelegDeRegKey(cred)            => false
          case DCert.DelegDelegate(cred, delegatee) => false
          case DCert.PoolRegister(poolId, vrf)      => false
          case DCert.PoolRetire(poolId, epoch)      => false
          case DCert.Genesis                        => false
          case DCert.Mir                            => false

      case DCert.DelegDeRegKey(cred) =>
        y match
          case DCert.DelegRegKey(cred)              => false
          case DCert.DelegDeRegKey(cred)            => cred === cred
          case DCert.DelegDelegate(cred, delegatee) => false
          case DCert.PoolRegister(poolId, vrf)      => false
          case DCert.PoolRetire(poolId, epoch)      => false
          case DCert.Genesis                        => false
          case DCert.Mir                            => false
      case DCert.DelegDelegate(cred, delegatee) =>
        y match
          case DCert.DelegRegKey(cred)              => false
          case DCert.DelegDeRegKey(cred)            => false
          case DCert.DelegDelegate(cred, delegatee) => cred === cred && delegatee === delegatee
          case DCert.PoolRegister(poolId, vrf)      => false
          case DCert.PoolRetire(poolId, epoch)      => false
          case DCert.Genesis                        => false
          case DCert.Mir                            => false
      case DCert.PoolRegister(poolId, vrf) =>
        y match
          case DCert.DelegRegKey(cred)              => false
          case DCert.DelegDeRegKey(cred)            => false
          case DCert.DelegDelegate(cred, delegatee) => false
          case DCert.PoolRegister(poolId, vrf)      => poolId === poolId && vrf === vrf
          case DCert.PoolRetire(poolId, epoch)      => false
          case DCert.Genesis                        => false
          case DCert.Mir                            => false
      case DCert.PoolRetire(poolId, epoch) =>
        y match
          case DCert.DelegRegKey(cred)              => false
          case DCert.DelegDeRegKey(cred)            => false
          case DCert.DelegDelegate(cred, delegatee) => false
          case DCert.PoolRegister(poolId, vrf)      => false
          case DCert.PoolRetire(poolId, epoch)      => poolId === poolId && epoch === epoch
          case DCert.Genesis                        => false
          case DCert.Mir                            => false
      case DCert.Genesis =>
        y match
          case DCert.DelegRegKey(cred)              => false
          case DCert.DelegDeRegKey(cred)            => false
          case DCert.DelegDelegate(cred, delegatee) => false
          case DCert.PoolRegister(poolId, vrf)      => false
          case DCert.PoolRetire(poolId, epoch)      => false
          case DCert.Genesis                        => true
          case DCert.Mir                            => false
      case DCert.Mir =>
        y match
          case DCert.DelegRegKey(cred)              => false
          case DCert.DelegDeRegKey(cred)            => false
          case DCert.DelegDelegate(cred, delegatee) => false
          case DCert.PoolRegister(poolId, vrf)      => false
          case DCert.PoolRetire(poolId, epoch)      => false
          case DCert.Genesis                        => false
          case DCert.Mir                            => true
}

case class TxId(hash: ByteString):
  override def toString = s"TxId(${hash.toHex})"

@Compile
object TxId:
  given Eq[TxId] = (a: TxId, b: TxId) => Builtins.equalsByteString(a.hash, b.hash)

/*
data TxOutRef = TxOutRef {
    txOutRefId  :: TxId,
    txOutRefIdx :: Integer -- ^ Index into the referenced transaction's outputs
    }
 */
case class TxOutRef(id: TxId, idx: BigInt)

@Compile
object TxOutRef {
  given Eq[TxOutRef] = (a: TxOutRef, b: TxOutRef) =>
    a match
      case TxOutRef(aTxId, aTxOutIndex) =>
        b match
          case TxOutRef(bTxId, bTxOutIndex) =>
            aTxOutIndex === bTxOutIndex && aTxId === bTxId
}

case class PubKeyHash(hash: ByteString) {
  override def toString = s"PubKeyHash(${hash})"
}

@Compile
object PubKeyHash {
  given Eq[PubKeyHash] = (a: PubKeyHash, b: PubKeyHash) => Builtins.equalsByteString(a.hash, b.hash)
}

enum Credential:
  case PubKeyCredential(hash: PubKeyHash)
  case ScriptCredential(hash: ValidatorHash)

@Compile
object Credential {
  given Eq[Credential] = (a: Credential, b: Credential) =>
    a match
      case Credential.PubKeyCredential(hash) =>
        b match
          case Credential.PubKeyCredential(hash2) => hash === hash2
          case Credential.ScriptCredential(hash)  => false
      case Credential.ScriptCredential(hash) =>
        b match
          case Credential.PubKeyCredential(hash2) => false
          case Credential.ScriptCredential(hash2) => hash === hash2
}

enum StakingCredential:
  case StakingHash(cred: Credential)
  case StakingPtr(a: BigInt, b: BigInt, c: BigInt)

@Compile
object StakingCredential {
  given Eq[StakingCredential] = (lhs: StakingCredential, rhs: StakingCredential) =>
    lhs match
      case StakingCredential.StakingHash(cred) =>
        rhs match
          case StakingCredential.StakingHash(cred2)  => cred === cred2
          case StakingCredential.StakingPtr(a, b, c) => false
      case StakingCredential.StakingPtr(a, b, c) =>
        rhs match
          case StakingCredential.StakingHash(cred2)     => false
          case StakingCredential.StakingPtr(a2, b2, c2) => a === a2 && b === b2 && c === c2
}

case class Address(
    credential: Credential,
    stakingCredential: Maybe[StakingCredential]
)

@Compile
object Address {
  given Eq[Address] = (a: Address, b: Address) =>
    a match
      case Address(aCredential, aStakingCredential) =>
        b match
          case Address(bCredential, bStakingCredential) =>
            aCredential === bCredential && aStakingCredential === bStakingCredential
}

case class TxOut(address: Address, value: Value, datumHash: Maybe[DatumHash])

// TxInInfo
case class TxInInfo(
    outRef: TxOutRef,
    resolved: TxOut
)

case class TxInfo(
    inputs: List[TxInInfo],
    outputs: List[TxOut],
    fee: Value,
    mint: Value,
    dcert: List[DCert],
    withdrawals: List[(StakingCredential, BigInt)],
    validRange: POSIXTimeRange,
    signatories: List[PubKeyHash],
    data: List[(DatumHash, Datum)],
    id: TxId
)

enum ScriptPurpose:
  case Minting(curSymbol: ByteString)
  case Spending(txOutRef: TxOutRef)
  case Rewarding(stakingCred: StakingCredential)
  case Certifying(cert: DCert)

@Compile
object ScriptPurpose {
  given Eq[ScriptPurpose] = (x: ScriptPurpose, y: ScriptPurpose) =>
    x match
      case ScriptPurpose.Minting(curSymbol) =>
        y match
          case ScriptPurpose.Minting(curSymbol)     => curSymbol === curSymbol
          case ScriptPurpose.Spending(txOutRef)     => false
          case ScriptPurpose.Rewarding(stakingCred) => false
          case ScriptPurpose.Certifying(cert)       => false
      case ScriptPurpose.Spending(txOutRef) =>
        y match
          case ScriptPurpose.Minting(curSymbol)     => false
          case ScriptPurpose.Spending(txOutRef)     => txOutRef === txOutRef
          case ScriptPurpose.Rewarding(stakingCred) => false
          case ScriptPurpose.Certifying(cert)       => false
      case ScriptPurpose.Rewarding(stakingCred) =>
        y match
          case ScriptPurpose.Minting(curSymbol)     => false
          case ScriptPurpose.Spending(txOutRef)     => false
          case ScriptPurpose.Rewarding(stakingCred) => stakingCred === stakingCred
          case ScriptPurpose.Certifying(cert)       => false
      case ScriptPurpose.Certifying(cert) =>
        y match
          case ScriptPurpose.Minting(curSymbol)     => false
          case ScriptPurpose.Spending(txOutRef)     => false
          case ScriptPurpose.Rewarding(stakingCred) => false
          case ScriptPurpose.Certifying(cert)       => cert === cert
}

// data ScriptContext = ScriptContext{scriptContextTxInfo :: TxInfo, scriptContextPurpose :: ScriptPurpose }
case class ScriptContext(txInfo: TxInfo, purpose: ScriptPurpose)
