package scalus.sir

import scalus.sir.Recursivity.*
import scalus.uplc.DefaultFun
import scalus.uplc.DefaultFun.*
import scalus.uplc.ExprBuilder
import scalus.uplc.Meaning
import scalus.uplc.NamedDeBruijn
import scalus.uplc.Term
import scalus.uplc.TermDSL.{*, given}
import scalus.uplc.TypeScheme
import scala.collection.mutable.HashMap

class SimpleSirToUplcLowering {

  val builtinTerms = {
    def forceBuiltin(scheme: TypeScheme, term: Term): Term = scheme match
      case TypeScheme.All(_, t) => Term.Force(forceBuiltin(t, term))
      case _                    => term

    Meaning.BuiltinMeanings.map((bi, rt) => bi -> forceBuiltin(rt.typeScheme, Term.Builtin(bi)))
  }

  private var zCombinatorNeeded: Boolean = false
  private val decls = HashMap.empty[String, DataDecl]

  def lower(sir: SIR): Term =
    val term = lowerInner(sir)
    if zCombinatorNeeded then Term.Apply(Term.LamAbs("__z_combinator__", term), ExprBuilder.ZTerm)
    else term

  def lowerInner(sir: SIR): Term =
    sir match
      case SIR.Decl(data, body) =>
        decls(data.name) = data
        lowerInner(body)
      case SIR.Constr(name, data, args) =>
        /* data List a = Nil | Cons a (List a)
           Nil is represented as \Nil Cons -> force Nil
           Cons is represented as (\head tail Nil Cons -> Cons head tail) h tl
         */
        val constrs = data.constructors.map(_.name)
        val ctorParams = data.constructors.find(_.name == name) match
          case None => throw new IllegalArgumentException(s"Constructor $name not found in $data")
          case Some(value) => value.args

        // force Nil | Cons head tail
        val appInner = ctorParams match
          case Nil => Term.Force(Term.Var(NamedDeBruijn(name)))
          case _ =>
            ctorParams.foldLeft(Term.Var(NamedDeBruijn(name)))((acc, param) =>
              acc $ Term.Var(NamedDeBruijn(param))
            )
        // \Nil Cons -> ...
        val ctor = constrs.foldRight(appInner) { (constr, acc) =>
          Term.LamAbs(constr, acc)
        }
        // \head tail Nil Cons -> ...
        val ctorParamsLambda = ctorParams.foldRight(ctor) { (param, acc) =>
          Term.LamAbs(param, acc)
        }
        // (\Nil Cons -> force Nil) | (\head tail Nil Cons -> ...) h tl
        args.foldLeft(ctorParamsLambda) { (acc, arg) =>
          Term.Apply(acc, lowerInner(arg))
        }
      case SIR.Match(scrutinee, cases) =>
        /* list match
          case Nil -> 1
          case Cons(h, tl) -> 2

          lowers to list (delay 1) (\h tl -> 2)
         */
        val scrutineeTerm = lowerInner(scrutinee)
        val casesTerms = cases.map { case Case(constr, bindings, body) =>
          constr.args match
            case Nil => ~lowerInner(body)
            case _ => bindings.foldRight(lowerInner(body)) { (binding, acc) =>
              Term.LamAbs(binding, acc)
            }
          }
        casesTerms.foldLeft(scrutineeTerm) { (acc, caseTerm) => Term.Apply(acc, caseTerm) }
      case SIR.Var(name) => Term.Var(name)
      case SIR.Let(NonRec, bindings, body) =>
        bindings.foldRight(lowerInner(body)) { case (Binding(name, rhs), body) =>
          Term.Apply(Term.LamAbs(name, body), lowerInner(rhs))
        }
      case SIR.Let(Rec, Binding(name, rhs) :: Nil, body) =>
        /* let rec f x = f (x + 1)
           in f 0
           (\f -> f 0) (Z (\f. \x. f (x + 1)))
         */
        zCombinatorNeeded = true
        val fixed =
          Term.Apply(
            Term.Var(NamedDeBruijn("__z_combinator__")),
            Term.LamAbs(name, lowerInner(rhs))
          )
        Term.Apply(Term.LamAbs(name, lowerInner(body)), fixed)
      case SIR.Let(Rec, bindings, body) =>
        sys.error(s"Mutually recursive bindings are not supported: $bindings")
      case SIR.LamAbs(name, term) => Term.LamAbs(name, lowerInner(term))
      case SIR.Apply(f, arg)      => Term.Apply(lowerInner(f), lowerInner(arg))
      case SIR.Const(const)       => Term.Const(const)
      case SIR.IfThenElse(cond, t, f) =>
        !(builtinTerms(DefaultFun.IfThenElse) $ lowerInner(cond) $ ~lowerInner(t) $ ~lowerInner(f))
      case SIR.Builtin(bn) => builtinTerms(bn)
      case SIR.Error(msg)  => Term.Error(msg)
}
