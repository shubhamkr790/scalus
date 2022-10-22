package scalus.macros

import scalus.sir.{Binding, SIR}
import scalus.uplc.ExprBuilder.*
import scalus.uplc.{Data, ExprBuilder, NamedDeBruijn, Expr as Exp, Term as Trm}
import scalus.utils.Utils

import scala.collection.immutable
import scala.quoted.*
object Macros {
  def lamMacro[A: Type, B: Type](f: Expr[Exp[A] => Exp[B]])(using Quotes): Expr[Exp[A => B]] =
    import quotes.reflect.*
    val name = f.asTerm match
      // lam(x => body)
      case Inlined(_, _, Block(List(DefDef(_, List(List(ValDef(name, _, _))), _, body)), _)) =>
        Expr(name)
      // lam { x => body }
      case Inlined(
            _,
            _,
            Block(List(), Block(List(DefDef(_, List(List(ValDef(name, _, _))), _, body)), _))
          ) =>
        Expr(name)
      case x => report.errorAndAbort(x.toString)
    '{
      Exp(Trm.LamAbs($name, $f(vr($name)).term))
    }

  def asExprMacro[A: Type](e: Expr[A])(using Quotes): Expr[Exp[A]] =
    import quotes.reflect.*
    e.asTerm match
      // lam(x => body)
      case Inlined(_, _, Block(stmts, expr)) =>
        def asdf(e: Term): Expr[Exp[A]] = e match
          case Ident(name) =>
            val nm = Expr(name)
            '{ vr[A]($nm) }
          case Typed(e, _) =>
            asdf(e)
          case _ =>
            report.errorAndAbort(e.toString)
        asdf(expr)
      case x => report.errorAndAbort("asExprMacro: " + x.toString)

  def fieldAsDataMacro[A: Type](e: Expr[A => Any])(using Quotes): Expr[Exp[Data] => Exp[Data]] =
    import quotes.reflect.*
    e.asTerm match
      case Inlined(
            _,
            _,
            Block(List(DefDef(_, _, _, Some(select @ Select(_, fieldName)))), _)
          ) =>
        def genGetter(typeSymbolOfA: Symbol, fieldName: String): Expr[Exp[Data] => Exp[Data]] =
          val fieldOpt: Option[(Symbol, Int)] =
            if typeSymbolOfA == TypeRepr.of[Tuple2].typeSymbol then
              fieldName match
                case "_1" => typeSymbolOfA.caseFields.find(_.name == fieldName).map(s => (s, 0))
                case "_2" => typeSymbolOfA.caseFields.find(_.name == fieldName).map(s => (s, 1))
                case _ =>
                  report.errorAndAbort("Unexpected field name for Tuple2 type: " + fieldName)
            else typeSymbolOfA.caseFields.zipWithIndex.find(_._1.name == fieldName)
//          report.info(s"$typeSymbolOfA => fieldOpt: $fieldOpt")
          fieldOpt match
            case Some((fieldSym: Symbol, idx)) =>
              val idxExpr = Expr(idx)
              '{
                var expr: Exp[Data] => Exp[List[Data]] = d => sndPair(unConstrData(d))
                var i = 0
                while i < $idxExpr do
                  val exp = expr // save the current expr, otherwise it will loop forever
                  expr = d => tailList(exp(d))
                  i += 1
                d => headList(expr(d))
              }
            case None =>
              report.errorAndAbort("fieldMacro: " + fieldName)

        def composeGetters(tree: Tree): Expr[Exp[Data] => Exp[Data]] = tree match
          case Select(select @ Select(_, _), fieldName) =>
            val a = genGetter(select.tpe.typeSymbol, fieldName)
            val b = composeGetters(select)
            '{ $a compose $b }
          case Select(ident @ Ident(_), fieldName) =>
            genGetter(ident.tpe.typeSymbol, fieldName)
          case _ =>
            report.errorAndAbort(
              s"field macro supports only this form: _.caseClassField1.field2, but got " + tree.show
            )
        composeGetters(select)
      case x => report.errorAndAbort(x.toString)

  def fieldMacro[A: Type](e: Expr[A => Any])(using Quotes): Expr[Exp[Data] => Exp[Any]] =
    import quotes.reflect.*
    e.asTerm match
      case Inlined(
            _,
            _,
            Block(List(DefDef(_, _, _, Some(select @ Select(_, fieldName)))), _)
          ) =>
        def genGetter(
            typeSymbolOfA: Symbol,
            fieldName: String
        ): (Symbol, Expr[Exp[Data] => Exp[Data]]) =
          val fieldOpt: Option[(Symbol, Int)] =
            if typeSymbolOfA == TypeRepr.of[Tuple2].typeSymbol then
              fieldName match
                case "_1" => typeSymbolOfA.caseFields.find(_.name == fieldName).map(s => (s, 0))
                case "_2" => typeSymbolOfA.caseFields.find(_.name == fieldName).map(s => (s, 1))
                case _ =>
                  report.errorAndAbort("Unexpected field name for Tuple2 type: " + fieldName)
            else typeSymbolOfA.caseFields.zipWithIndex.find(_._1.name == fieldName)
          fieldOpt match
            case Some((fieldSym: Symbol, idx)) =>
              val idxExpr = Expr(idx)
              (
                fieldSym,
                '{
                  var expr: Exp[Data] => Exp[List[Data]] = d => sndPair(unConstrData(d))
                  var i = 0
                  while i < $idxExpr do
                    val exp = expr // save the current expr, otherwise it will loop forever
                    expr = d => tailList(exp(d))
                    i += 1
                  (d: Exp[Data]) => headList(expr(d))
                }
              )
            case None =>
              report.errorAndAbort("fieldMacro: " + fieldName)

        def composeGetters(tree: Tree): (TypeRepr, Expr[Exp[Data] => Exp[Data]]) = tree match
          case Select(select @ Select(_, _), fieldName) =>
            val (_, a) = genGetter(select.tpe.typeSymbol, fieldName)
            val (s, b) = composeGetters(select)
            (s, '{ $a compose $b })
          case Select(ident @ Ident(_), fieldName) =>
            val (fieldSym, f) = genGetter(ident.tpe.typeSymbol, fieldName)
            val fieldType = ident.tpe.memberType(fieldSym).dealias
            (fieldType, f)
          case _ =>
            report.errorAndAbort(
              s"field macro supports only this form: _.caseClassField1.field2, but got " + tree.show
            )

        val (fieldType, getter) = composeGetters(select)
        val unliftTypeRepr = TypeRepr.of[Unlift].appliedTo(fieldType)
        /*report.info(
          s"composeGetters: fieldType = ${fieldType.show} unliftTypeRepr = ${unliftTypeRepr.show}, detailed fieldType: $fieldType"
        )*/
        Implicits.search(unliftTypeRepr) match
          case success: ImplicitSearchSuccess =>
            unliftTypeRepr.asType match
              case '[Unlift[t]] =>
                val expr = success.tree
                val impl = success.tree.asExpr
                /*report
                  .info(
                    s"found implicit ${unliftTypeRepr.show} => ${expr.show}: ${expr.tpe.show}"
                  )*/
                '{ (d: Exp[Data]) =>
                  ExprBuilder
                    .app($impl.asInstanceOf[Unlift[t]].unlift, $getter(d))
                }
          case failure: ImplicitSearchFailure =>
            report.info(s"not found implicit of type ${unliftTypeRepr.show}")
            getter
      case x => report.errorAndAbort(x.toString)

  def compileImpl(e: Expr[Any])(using Quotes): Expr[SIR] =
    import quotes.reflect.*
    import scalus.uplc.Constant.*
    import scalus.uplc.DefaultFun
    import scalus.sir.Recursivity

    extension (t: Term) def isList = t.tpe <:< TypeRepr.of[immutable.List[_]]

    def compileStmt(stmt: Statement, expr: Expr[SIR]): Expr[SIR] = {
      stmt match
        case ValDef(a, tpe, Some(body)) =>
          val bodyExpr = compileExpr(body)
          val aExpr = Expr(a)
          '{ SIR.Let(Recursivity.NonRec, immutable.List(Binding($aExpr, $bodyExpr)), $expr) }
        case DefDef(name, immutable.List(TermParamClause(args)), tpe, Some(body)) =>
          val bodyExpr: Expr[scalus.sir.SIR] = {
            val bE = compileExpr(body)
            if args.isEmpty then '{ SIR.LamAbs("_", $bE) }
            else
              val names = args.map { case ValDef(name, tpe, rhs) => Expr(name) }
              names.foldRight(bE) { (name, acc) =>
                '{ SIR.LamAbs($name, $acc) }
              }
          }
          val nameExpr = Expr(name)
          '{ SIR.Let(Recursivity.Rec, immutable.List(Binding($nameExpr, $bodyExpr)), $expr) }
        case DefDef(name, args, tpe, _) =>
          report.errorAndAbort(
            "compileStmt: Only single argument list defs are supported, but given: " + stmt.show
          )
        case x => report.errorAndAbort(s"compileStmt: $x")
    }
    def compileBlock(stmts: immutable.List[Statement], expr: Term): Expr[SIR] = {
      import quotes.reflect.*
      val e = compileExpr(expr)
      stmts.foldRight(e)(compileStmt)
    }

    def compileExpr(e: Term): Expr[SIR] = {
      import quotes.reflect.*
      e match
        // lam(x => body)
        case Literal(UnitConstant()) => '{ SIR.Const(Unit) }
        case Literal(StringConstant(lit)) =>
          val litE = Expr(lit)
          '{ SIR.Const(String($litE)) }
        case Literal(BooleanConstant(lit)) =>
          val litE = Expr(lit)
          '{ SIR.Const(Bool($litE)) }
        case Literal(_) => report.errorAndAbort("compileExpr: Unsupported literal " + e.show)
        case lit @ Apply(Select(Ident("BigInt"), "apply"), _) =>
          val litE = lit.asExprOf[BigInt]
          '{ SIR.Const(Integer($litE)) }
        case lit @ Apply(Ident("int2bigInt"), _) =>
          val litE = lit.asExprOf[BigInt]
          '{ SIR.Const(Integer($litE)) }
        case lit @ Apply(
              Select(
                Apply(
                  Ident("StringInterpolators"),
                  immutable.List(
                    Apply(
                      Select(Select(Select(Ident("_root_"), "scala"), "StringContext"), "apply"),
                      _
                    )
                  )
                ),
                "hex"
              ),
              _
            ) =>
          val litE = lit.asExprOf[Array[Byte]]
          '{ SIR.Const(ByteString($litE)) }
        case Ident(a) =>
          val aE = Expr(a)
          '{ SIR.Var(NamedDeBruijn($aE)) }
        case If(cond, t, f) =>
          '{
            SIR.Apply(
              SIR.Apply(
                SIR.Apply(SIR.Builtin(DefaultFun.IfThenElse), ${ compileExpr(cond) }),
                ${ compileExpr(t) }
              ),
              ${ compileExpr(f) }
            )
          }
        case Select(lst, "head") if lst.isList =>
          '{ SIR.Apply(SIR.Builtin(DefaultFun.HeadList), ${ compileExpr(lst) }) }
        case Apply(
              TypeApply(Select(list, "apply"), _),
              immutable.List(ex)
            ) /*if list.tpe <:< TypeRepr.of[immutable.List]*/ =>
          report.errorAndAbort(
            s"compileExpr: List is not supported yet ${list.tpe.typeSymbol}\n$list\n${list.tpe <:< TypeRepr
                .of[immutable.List[?]]}"
          )
        // throw new Exception("error msg")
        // Supports any exception type that uses first argument as message
        case Apply(Ident("throw"), immutable.List(ex)) =>
          val msg = ex match
            case Apply(
                  Select(New(tpt), "<init>"),
                  immutable.List(Literal(StringConstant(msg)), _*)
                ) if tpt.tpe <:< TypeRepr.of[Exception] =>
              Expr(msg)
            case term =>
              Expr("error")
          '{ SIR.Error($msg) }
        case Apply(Select(Ident(a), "apply"), args) =>
          val argsE = args.map(compileExpr)
          argsE.foldLeft('{ SIR.Var(NamedDeBruijn(${ Expr(a) })) })((acc, arg) =>
            '{ SIR.Apply($acc, $arg) }
          )
        case Apply(f, args) =>
          val fE = compileExpr(f)
          val argsE = args.map(compileExpr)
          if argsE.isEmpty then '{ SIR.Apply($fE, SIR.Const(Unit)) }
          else argsE.foldLeft(fE)((acc, arg) => '{ SIR.Apply($acc, $arg) })
        case Block(stmt, expr)       => compileBlock(stmt, expr)
        case Typed(expr, _)          => compileExpr(expr)
        case Closure(Ident(name), _) => '{ SIR.Var(NamedDeBruijn(${ Expr(name) })) }
        case x                       => report.errorAndAbort("compileExpr: " + x.toString)
    }

    e.asTerm match
      // lam(x => body)
      case Inlined(_, _, expr) =>
        report.info(s"Compile: ${expr}")
        compileExpr(expr)
      case x => report.errorAndAbort("compileImpl: " + x.toString)

}
