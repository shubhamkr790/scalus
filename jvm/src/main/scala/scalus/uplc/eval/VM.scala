package scalus.uplc
package eval

import scalus.builtin.*

object VM extends VMBase {
    def evaluateTerm(params: MachineParams, term: Term): CekResult = {
        val cek = new CekMachine(params)
        cek.runCek(term)
    }
}

/** @inheritdoc */
final class CekMachine(params: MachineParams)
    extends AbstractCekMachine(params)
    with JVMPlatformSpecific

@deprecated("Use VM instead", "0.7.0")
object Cek {
    @deprecated("Use VM methods instead", "0.7.0")
    def evalUPLC(term: Term): Term = {
        val params = MachineParams.defaultParams
        val debruijnedTerm = DeBruijn.deBruijnTerm(term)
        new CekMachine(params).evaluateTerm(debruijnedTerm)
    }

    @deprecated("Use VM methods instead", "0.7.0")
    def evalUPLCProgram(p: Program): Term = evalUPLC(p.term)
}
