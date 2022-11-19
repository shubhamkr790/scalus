package scalus.macros

import scalus.builtins
import scalus.sir.{Binding, SIR}
import scalus.uplc.ExprBuilder.*
import scalus.builtins.Builtins
import scalus.uplc.{Constant, Data, DefaultUni, Expr as Exp, ExprBuilder, NamedDeBruijn, Term as Trm}
import scalus.utils.Utils

import scala.collection.mutable.ListBuffer
import scala.collection.{immutable, mutable, IterableFactory, SeqFactory}
import scala.quoted.*
import scalus.sir.DataDecl
import scalus.sir.Case
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

  def fieldAsDataMacro1[A: Type](e: Expr[A => Any])(using Quotes): Expr[Data => Data] =
    import quotes.reflect.*
    fieldAsDataMacro2(e.asTerm)

  def fieldAsDataMacro2(using q: Quotes)(e: q.reflect.Term): Expr[Data => Data] =
    import quotes.reflect.*
    e match
      case Inlined(_, _, block) => fieldAsDataMacro2(block)
      case Block(List(DefDef(_, _, _, Some(select @ Select(_, fieldName)))), _) =>
        def genGetter(typeSymbolOfA: Symbol, fieldName: String): Expr[Data => Data] =
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

              var expr: Expr[Data => builtins.List[Data]] = '{ d =>
                Builtins.unsafeDataAsConstr(d).snd
              }
              var i = 0
              while i < idx do
                val exp = expr // save the current expr, otherwise it will loop forever
                expr = '{ d => $exp(d).tail }
                i += 1
              '{ d => $expr(d).head }

            case None =>
              report.errorAndAbort("fieldMacro: " + fieldName)

        def composeGetters(tree: Tree): Expr[Data => Data] = tree match
          case Select(select @ Select(_, _), fieldName) =>
            val a = genGetter(select.tpe.typeSymbol, fieldName)
            val b = composeGetters(select)
            '{ ddd => $a($b(ddd)) }
          case Select(ident @ Ident(_), fieldName) =>
            genGetter(ident.tpe.typeSymbol, fieldName)
          case _ =>
            report.errorAndAbort(
              s"field macro supports only this form: _.caseClassField1.field2, but got " + tree.show
            )
        composeGetters(select)
      case x => report.errorAndAbort(x.toString)

  def compileImpl(e: Expr[Any])(using q: Quotes): Expr[SIR] =
    import q.reflect.{*, given}
//    import scalus.uplc.Constant.*
    import scalus.uplc.DefaultFun
    import scalus.sir.Recursivity

    type Env = immutable.HashSet[Symbol]

    case class B(name: String, symbol: Symbol, recursivity: Recursivity, body: Expr[SIR]):
      def fullName = symbol.fullName

    val globalDefs: mutable.LinkedHashMap[Symbol, B] = mutable.LinkedHashMap.empty
    val globalDataDecls: mutable.LinkedHashMap[Symbol, Expr[DataDecl]] = mutable.LinkedHashMap.empty

    var globalPosition = 0

    def debugInfo(s: String): Unit =
      globalPosition += 1
      report.info(s, Position(SourceFile.current, globalPosition, 0))

    extension (t: Term) def isList = t.tpe <:< TypeRepr.of[builtins.List[_]]
    extension (t: Term) def isPair = t.tpe <:< TypeRepr.of[builtins.Pair[_, _]]
    extension (t: Term) def isLiteral = compileConstant.isDefinedAt(t)
    extension (t: Term) def isData = t.tpe <:< TypeRepr.of[scalus.uplc.Data]

    given ToExpr[Recursivity] with
      def apply(x: Recursivity)(using Quotes): Expr[Recursivity] =
        import quotes.reflect._
        x match
          case Recursivity.NonRec => '{ Recursivity.NonRec }
          case Recursivity.Rec    => '{ Recursivity.Rec }

    given ToExpr[DefaultUni] with {
      def apply(x: DefaultUni)(using Quotes) =
        import quotes.reflect._
        x match
          case DefaultUni.Unit       => '{ DefaultUni.Unit }
          case DefaultUni.Bool       => '{ DefaultUni.Bool }
          case DefaultUni.Integer    => '{ DefaultUni.Integer }
          case DefaultUni.String     => '{ DefaultUni.String }
          case DefaultUni.ByteString => '{ DefaultUni.ByteString }
          case DefaultUni.Data       => '{ DefaultUni.Data }
          case DefaultUni.Apply(DefaultUni.ProtoList, a) =>
            '{ DefaultUni.List(${ Expr(a) }) }
          case DefaultUni.Apply(DefaultUni.Apply(DefaultUni.ProtoPair, a), b) =>
            '{ DefaultUni.Pair(${ Expr(a) }, ${ Expr(b) }) }
          case DefaultUni.Apply(f, a) =>
            '{ DefaultUni.Apply(${ Expr(f) }, ${ Expr(a) }) }
          case _ => report.errorAndAbort(s"Unsupported DefaultUni type $x")
    }

    def typeReprToDefaultUni(t: TypeRepr): DefaultUni =
      t.asType match
        case '[BigInt]              => DefaultUni.Integer
        case '[java.lang.String]    => DefaultUni.String
        case '[Boolean]             => DefaultUni.Bool
        case '[Unit]                => DefaultUni.Unit
        case '[builtins.ByteString] => DefaultUni.ByteString
        case '[builtins.List[a]] =>
          val immutable.List(a) = t.typeArgs
          val aType = typeReprToDefaultUni(a)
          DefaultUni.List(aType)
        case '[builtins.Pair[a, b]] =>
          t.typeArgs match
            case immutable.List(a, b) =>
              DefaultUni.Pair(typeReprToDefaultUni(a), typeReprToDefaultUni(b))
            case _ => report.errorAndAbort("Unexpected type arguments for Pair: " + t.show)
        case _ if t <:< TypeRepr.of[scalus.uplc.Data] => DefaultUni.Data
        case _ => report.errorAndAbort(s"Unsupported type: ${t.show}")

    def compileStmt(env: Env, stmt: Statement): B = {
      //debugInfo(s"compileStmt  ${stmt}", Position(SourceFile.current, globalPosition, 0))
      stmt match
        case ValDef(name, tpe, Some(body)) =>
          val bodyExpr = compileExpr(env, body)
          val aExpr = Expr(name)
          B(name, stmt.symbol, Recursivity.NonRec, bodyExpr)
        case DefDef(name, argss, tpe, Some(body)) =>
          val args = argss.collect({ case TermParamClause(args) => args }).flatten
          val bodyExpr: Expr[scalus.sir.SIR] = {
            if args.isEmpty then
              val bE = compileExpr(env + stmt.symbol, body)
              '{ SIR.LamAbs("_", $bE) }
            else
              val symbols = args.map { case v @ ValDef(name, tpe, rhs) => v.symbol }
              val bE = compileExpr(env ++ symbols + stmt.symbol, body)
              symbols.foldRight(bE) { (symbol, acc) =>
                '{ SIR.LamAbs(${ Expr(symbol.name) }, $acc) }
              }
          }
          B(name, stmt.symbol, Recursivity.Rec, bodyExpr)
        case ValDef(name, tpe, None) =>
          report.errorAndAbort(
            s"""compileStmt: val ${stmt.symbol.fullName} has no body. Try adding "scalacOptions += "-Yretain-trees" to your build.sbt"""
          )
        case DefDef(name, args, tpe, None) =>
          report.errorAndAbort(
            s"""compileStmt: def ${stmt.symbol.fullName} has no body. Try adding "scalacOptions += "-Yretain-trees" to your build.sbt"""
          )
        case x: Term =>
          B("_", Symbol.noSymbol, Recursivity.NonRec, compileExpr(env, x))

        case x => report.errorAndAbort(s"compileStmt: $x", stmt.pos)
    }

    def compileBlock(env: Env, stmts: immutable.List[Statement], expr: Term): Expr[SIR] = {
      val exprs = ListBuffer.empty[B]
      val exprEnv = stmts.foldLeft(env) { case (env, stmt) =>
        val bind = compileStmt(env, stmt)
        exprs += bind
        env + bind.symbol
      }
      val exprExpr = compileExpr(exprEnv, expr)
      exprs.foldRight(exprExpr) { (bind, expr) =>
        '{
          SIR.Let(
            ${ Expr(bind.recursivity) },
            List(Binding(${ Expr(bind.name) }, ${ bind.body })),
            $expr
          )
        }
      }
    }

    def compileConstant: PartialFunction[Term, Expr[scalus.uplc.Constant]] = {
      case Literal(UnitConstant()) => '{ scalus.uplc.Constant.Unit }
      case Literal(StringConstant(lit)) =>
        val litE = Expr(lit)
        '{ scalus.uplc.Constant.String($litE) }
      case Literal(BooleanConstant(lit)) =>
        val litE = Expr(lit)
        '{ scalus.uplc.Constant.Bool($litE) }
      case e @ Literal(_) => report.errorAndAbort(s"compileExpr: Unsupported literal ${e.show}\n$e")
      case lit @ Apply(Select(Ident("BigInt"), "apply"), _) =>
        val litE = lit.asExprOf[BigInt]
        '{ scalus.uplc.Constant.Integer($litE) }
      case lit @ Apply(Ident("int2bigInt"), _) =>
        val litE = lit.asExprOf[BigInt]
        '{ scalus.uplc.Constant.Integer($litE) }
      case lit @ Ident("empty") if lit.tpe.show == "scalus.builtins.ByteString.empty" =>
        val litE = lit.asExprOf[builtins.ByteString]
        '{ scalus.uplc.Constant.ByteString($litE) }
      case lit @ Apply(Select(byteString, "fromHex" | "unsafeFromArray" | "apply"), args)
          if byteString.tpe =:= TypeRepr.of[builtins.ByteString.type] =>
        val litE = lit.asExprOf[builtins.ByteString]
        '{ scalus.uplc.Constant.ByteString($litE) }
    }

    def compileNewConstructor(
        env: Env,
        con: Term,
        tpe: TypeRepr,
        args: immutable.List[Term]
    ): Expr[SIR] = {
      /* We support these cases:
        1. case class Foo(a: Int, b: String)
        2. case object Bar
        3. enum Base { case A ...}
        4. enum Base { case B(a, b) }
        5. sealed abstract class Base; object Base { case object A extends Base }
        6. sealed abstract class Base; object Base { case class B(a: Int, b: String) extends Base }

       */
      val typeSymbol = tpe.typeSymbol

      // look for a base `sealed abstract class`. If it exists, we are in case 5 or 6
      val adtBaseType = tpe.baseClasses.find(b => b.flags.is(Flags.Sealed | Flags.Abstract) && !b.flags.is(Flags.Trait))

      // debugInfo(s"compileNewConstructor0")
      // debugInfo(s"compileNewConstructor1 ${tpe.show} base type: ${adtBaseType}")
      /* debugInfo(s"compileNewConstructor1 ${typeSymbol} singleton ${tpe.isSingleton} companion: ${typeSymbol.maybeOwner.companionClass} " +
        s"${typeSymbol.children} widen: ${tpe.widen.typeSymbol}, widen.children: ${tpe.widen.typeSymbol.children} ${typeSymbol.maybeOwner.companionClass.children}") */


      val (constructorTypeSymbol, dataTypeSymbol, constructors) =
        adtBaseType match
          case None => // case 1 or 2
            (typeSymbol, typeSymbol, List(typeSymbol))
          case Some(baseClassSymbol) if tpe.isSingleton => // case 3, 4, 5 or 6
            (tpe.termSymbol, baseClassSymbol, baseClassSymbol.children)
          case Some(baseClassSymbol) => // case 3, 4, 5 or 6
            (typeSymbol, baseClassSymbol, baseClassSymbol.children)

      val argsE = Expr.ofList(args.map(compileExpr(env, _)))
      val constrName = constructorTypeSymbol.name
      // sort by name to get a stable order
      val sortedConstructors = constructors.sortBy(_.name)
      val constrDecls = Expr.ofList(sortedConstructors.map { sym =>
        val params = sym.caseFields.map(_.name)
        '{ scalus.sir.ConstrDecl(${ Expr(sym.name) }, ${ Expr(params) }) }
      })
      val dataName = dataTypeSymbol.name
      // debugInfo(s"compileNewConstructor2: dataTypeSymbol $dataTypeSymbol, dataName $dataName, constrName $constrName, children ${constructors}")
      val dataDecl = globalDataDecls.get(dataTypeSymbol) match
        case Some(decl) => decl
        case None =>
          val decl = '{ scalus.sir.DataDecl(${ Expr(dataName) }, $constrDecls) }
          globalDataDecls.addOne(dataTypeSymbol -> decl)
          decl
      // constructor body as: constr arg1 arg2 ...
      '{ SIR.Constr(${ Expr(constrName) }, $dataDecl, $argsE) }
    }

    def isConstructorVal(symbol: Symbol, tpe: TypeRepr): Boolean =
      /* debugInfo(
        s"isConstructorVal: ${tpe.typeSymbol.isClassDef && symbol.flags.is(Flags.Case)} $symbol: ${tpe.show} <: ${tpe.widen.show}, ${tpe.typeSymbol.isClassDef}, ${symbol.flags
            .is(Flags.Case)}"
      ) */
      tpe.typeSymbol.isClassDef && symbol.flags.is(Flags.Case)

    def compileExpr(env: Env, e: Term): Expr[SIR] = {
      // debugInfo( s"compileExpr: ${e.show}\n${e}\nin ${env}" )
      if compileConstant.isDefinedAt(e) then
        val const = compileConstant(e)
        '{ SIR.Const($const) }
      else
        e match
          case Ident(a) =>
            // report.info(s"Ident: ${e.show}, a $a, full: ${e.symbol.fullName}, name: ${e.symbol.name}", Position(SourceFile.current, globalPosition, 0))
            if !env.contains(e.symbol) then
              val b = compileStmt(immutable.HashSet.empty, e.symbol.tree.asInstanceOf[Definition])
              globalDefs.update(b.symbol, b)
              /*report.errorAndAbort(
                s"compileExpr: Unknown identifier: $a, env: ${env.mkString(", ")}, tree: ${e.symbol.tree}"
              )*/
              '{ SIR.Var(NamedDeBruijn(${ Expr(e.symbol.fullName) })) }
            else '{ SIR.Var(NamedDeBruijn(${ Expr(e.symbol.name) })) }
          case If(cond, t, f) =>
            '{
              SIR.IfThenElse(
                ${ compileExpr(env, cond) },
                ${ compileExpr(env, t) },
                ${ compileExpr(env, f) }
              )
            }
          /*
              enum A:
                case C0
                case C1(a)
                case C2(b, c)
              compiles to:
              Decl(DataDecl("A", List(ConstrDecl("C0", List()), ConstrDecl("C1", List("a")), ConstrDecl("C2", List("b", "c")))), ...)

              c ==> Constr("C0", constrDecl, List())

              c match
                C1(a) -> 0
                C2(b, c) -> 1
              compiles to:
              Match(c, List(Case(C1, List(a), 0), Case(C2, List(b, c), 1)))
           */
          case Match(t, cases) =>
            // report.info(s"Match: ${t.tpe.typeSymbol} ${t.tpe.typeSymbol.children}", e.pos)

            def constructCase(constrSymbol: Symbol, bindings: List[String], rhs: Expr[SIR]): Expr[scalus.sir.Case] = {
              val params = constrSymbol.caseFields.map(_.name)
              val constrDecl = '{ scalus.sir.ConstrDecl(${ Expr(constrSymbol.name) }, ${ Expr(params) }) }
              '{scalus.sir.Case($constrDecl, ${Expr(bindings)}, $rhs)}
            }


            if t.tpe.typeSymbol.children.isEmpty
            then
              cases match
                case cs :: Nil =>
                  cs match
                    case CaseDef(_, Some(guard), _) =>
                      report.errorAndAbort(
                        s"Guards are not supported in match expressions",
                        guard.pos
                      )

                    case CaseDef(Unapply(fun, implicits, pats), None, rhs) =>
                      // report.error(s"Case: ${fun}, pats: ${pats}, rhs: $rhs", t.pos)
                      val names = pats.map {
                        case b @ Bind(name, Ident("_")) => b.symbol
                        case p => report.errorAndAbort(s"Unsupported binding: ${p}", p.pos)
                      }
                      val rhsE = compileExpr(env ++ names, rhs)
                      val tE = compileExpr(env, t)
                      val cases = List(constructCase(t.tpe.typeSymbol, names.map(_.name), rhsE))
                      '{ SIR.Match($tE, ${Expr.ofList(cases)}) }

                case _ =>
                  report.errorAndAbort(
                    s"Only single constructor pattern supported for type ${t.tpe.show}",
                    e.pos
                  )
            else if cases.length != t.tpe.typeSymbol.children.length
            then
              report.errorAndAbort(
                s"Unsupported pattern matching for type ${t.tpe.widen.show}, constructors: ${t.tpe.typeSymbol.children}",
                e.pos
              )
            else
              val cs = cases.map {
                case CaseDef(_, Some(guard), _) =>
                  report.errorAndAbort(s"Guards are not supported in match expressions", guard.pos)
                // case object
                case CaseDef(pat @ Ident(name), None, rhs) =>
                  val rhsE = compileExpr(env, rhs)
                  // no-arg constructor, it's a Val, so we use termSymbol
                  (pat.tpe.termSymbol, Nil, rhsE)
                case CaseDef(pat, None, rhs) =>
                  if pat.isInstanceOf[Typed] then
                    val (inner, constrTpe) = Typed.unapply(pat.asInstanceOf[Typed])

                    // report.info(s"Case: ${inner}, tpe ${constrTpe.tpe.widen.show}", t.pos)
                    inner match
                      case Unapply(fun, implicits, pats) =>
                        val bindings = pats.map {
                          case b @ Bind(name, Ident("_")) => b.symbol
                          case p => report.errorAndAbort(s"Unsupported binding: ${p}", p.pos)
                        }
                        val rhsE = compileExpr(env ++ bindings, rhs)
                        (constrTpe.tpe.typeSymbol, bindings, rhsE)
                      case _ => report.errorAndAbort(s"Unsupported case: ${inner}", inner.pos)
                  else report.errorAndAbort("AAAA", e.pos)
                case a =>
                  report.errorAndAbort(
                    s"Unsupported match expression: ${e.show}\n$e\n${t.tpe.typeSymbol}, cases: ${cases}",
                    t.pos
                  )
              }
              val tE = compileExpr(env, t)
              val sortedCases = cs.sortBy((t, _, _) => t.fullName).map { (sym, bindings, rhs) =>
                val params = sym.caseFields.map(_.name)
                '{ scalus.sir.ConstrDecl(${ Expr(sym.name) }, ${ Expr(params) }) }
                constructCase(sym, bindings.map(_.name), rhs)
              }
              // report.info(s"Sorted constrs: ${sortedCases}", cases.head.pos)
              '{ SIR.Match($tE, ${Expr.ofList(sortedCases)}) }

          // PAIR
          case Select(pair, fun) if pair.isPair =>
            fun match
              case "fst" =>
                '{ SIR.Apply(SIR.Builtin(DefaultFun.FstPair), ${ compileExpr(env, pair) }) }
              case "snd" =>
                '{ SIR.Apply(SIR.Builtin(DefaultFun.SndPair), ${ compileExpr(env, pair) }) }
              case _ => report.errorAndAbort(s"compileExpr: Unsupported pair function: $fun")
          case Apply(TypeApply(pair, immutable.List(tpe1, tpe2)), immutable.List(a, b))
              if pair.tpe.show == "scalus.builtins.Pair.apply" =>
            // We can create a Pair by either 2 literals as (con pair...)
            // or 2 Data variables using MkPairData builtin
            if a.isLiteral && b.isLiteral then
              '{
                SIR.Const(
                  scalus.uplc.Constant.Pair(${ compileConstant(a) }, ${ compileConstant(b) })
                )
              }
            else if a.isData && b.isData then
              '{
                SIR.Apply(
                  SIR.Apply(SIR.Builtin(DefaultFun.MkPairData), ${ compileExpr(env, a) }),
                  ${ compileExpr(env, b) }
                )
              }
            else
              report.errorAndAbort(
                s"""Builtin Pair can only be created either by 2 literals or 2 Data variables:
              |Pair[${tpe1.tpe.show},${tpe2.tpe.show}](${a.show}, ${b.show})
              |- ${a.show} literal: ${a.isLiteral}, data: ${a.isData}
              |- ${b.show} literal: ${b.isLiteral}, data: ${b.isData}
              |""".stripMargin
              )

          case Select(lst, fun) if lst.isList =>
            fun match
              case "head" =>
                '{ SIR.Apply(SIR.Builtin(DefaultFun.HeadList), ${ compileExpr(env, lst) }) }
              case "tail" =>
                '{ SIR.Apply(SIR.Builtin(DefaultFun.TailList), ${ compileExpr(env, lst) }) }
              case "isEmpty" =>
                '{ SIR.Apply(SIR.Builtin(DefaultFun.NullList), ${ compileExpr(env, lst) }) }
              case _ =>
                report.errorAndAbort(
                  s"compileExpr: Unsupported list method $fun. Only head, tail and isEmpty are supported"
                )

          case Apply(TypeApply(Ident("fieldAsData1"), List(tpe)), List(expr)) =>
            val getter = fieldAsDataMacro2(expr)
            // report.errorAndAbort(s"Getter")
            val r = compileExpr(env, getter.asTerm)
            // report.errorAndAbort(s"Getter: ${r.show}", expr.pos)
            r
          case TypeApply(Select(list, "empty"), immutable.List(tpe))
              if list.tpe =:= TypeRepr.of[builtins.List.type] =>
            val tpeE = Expr(typeReprToDefaultUni(tpe.tpe))
            '{ SIR.Const(scalus.uplc.Constant.List($tpeE, Nil)) }
          case Apply(
                TypeApply(Select(list, "::"), immutable.List(tpe)),
                immutable.List(arg)
              ) if list.isList =>
            val argE = compileExpr(env, arg)
            '{
              SIR.Apply(
                SIR.Apply(SIR.Builtin(DefaultFun.MkCons), $argE),
                ${ compileExpr(env, list) }
              )
            }
          case Apply(
                TypeApply(Select(list, "apply"), immutable.List(tpe)),
                immutable.List(ex)
              ) if list.tpe =:= TypeRepr.of[builtins.List.type] =>
            val tpeE = Expr(typeReprToDefaultUni(tpe.tpe))
            ex match
              case Typed(Repeated(args, _), _) =>
                val allLiterals = args.forall(arg => compileConstant.isDefinedAt(arg))
                if allLiterals then
                  val lits = Expr.ofList(args.map(compileConstant))
                  '{ SIR.Const(scalus.uplc.Constant.List($tpeE, $lits)) }
                else
                  val nil = '{ SIR.Const(scalus.uplc.Constant.List($tpeE, Nil)) }
                  args.foldRight(nil) { (arg, acc) =>
                    '{
                      SIR.Apply(
                        SIR.Apply(SIR.Builtin(DefaultFun.MkCons), ${ compileExpr(env, arg) }),
                        $acc
                      )
                    }
                  }
              case _ =>
                report.errorAndAbort(
                  s"compileExpr: List is not supported yet ${ex}"
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

          // new Constr(args)
          case Apply(TypeApply(con @ Select(f, "<init>"), _), args) =>
            compileNewConstructor(env, con, f.tpe, args)
          case Apply(con @ Select(f, "<init>"), args) =>
            compileNewConstructor(env, con, f.tpe, args)
          // (a, b) as scala.Tuple2.apply(a, b)
          case Apply(TypeApply(Select(f, "apply"), _), args)
              if f.tpe.widen =:= TypeRepr.of[scala.Tuple2.type] =>
            val typeSymbol = f.tpe.typeSymbol
            val constrName = typeSymbol.name
            val argsE = args.map(compileExpr(env, _))
            val constr =
              argsE.foldLeft('{ SIR.Var(NamedDeBruijn(${ Expr(constrName) })) })((acc, arg) =>
                '{ SIR.Apply($acc, $arg) }
              )
            '{ SIR.LamAbs(${ Expr(constrName) }, $constr) }
          // f.apply(arg) => Apply(f, arg)
          case Apply(Select(f, "apply"), args) if f.tpe.widen.isFunctionType =>
            val fE = compileExpr(env, f)
            val argsE = args.map(compileExpr(env, _))
            argsE.foldLeft(fE)((acc, arg) => '{ SIR.Apply($acc, $arg) })

          // BigInt equality
          case Select(lhs, "==") if lhs.tpe.widen =:= TypeRepr.of[BigInt] =>
            '{ SIR.Apply(SIR.Builtin(DefaultFun.EqualsInteger), ${ compileExpr(env, lhs) }) }
          // ByteString equality
          case Select(lhs, "==") if lhs.tpe.widen =:= TypeRepr.of[builtins.ByteString] =>
            '{ SIR.Apply(SIR.Builtin(DefaultFun.EqualsByteString), ${ compileExpr(env, lhs) }) }
          // Data BUILTINS
          case bi if bi.tpe.show == "scalus.builtins.Builtins.mkConstr" =>
            '{ SIR.Builtin(DefaultFun.ConstrData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.mkList" =>
            '{ SIR.Builtin(DefaultFun.ListData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.mkMap" =>
            '{ SIR.Builtin(DefaultFun.MapData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.mkB" =>
            '{ SIR.Builtin(DefaultFun.BData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.mkI" =>
            '{ SIR.Builtin(DefaultFun.IData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.unsafeDataAsConstr" =>
            '{ SIR.Builtin(DefaultFun.UnConstrData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.unsafeDataAsList" =>
            '{ SIR.Builtin(DefaultFun.UnListData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.unsafeDataAsMap" =>
            '{ SIR.Builtin(DefaultFun.UnMapData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.unsafeDataAsB" =>
            '{ SIR.Builtin(DefaultFun.UnBData) }
          case bi if bi.tpe.show == "scalus.builtins.Builtins.unsafeDataAsI" =>
            '{ SIR.Builtin(DefaultFun.UnIData) }
          // case class User(name: String, age: Int)
          // val user = User("John", 42) => \u - u "John" 42
          // user.name => \u name age -> name
          case sel @ Select(obj, ident) =>
            val ts = obj.tpe.widen.typeSymbol
            lazy val fieldIdx = ts.caseFields.indexOf(sel.symbol)
            if ts.isClassDef && fieldIdx >= 0 then
              val lhs = compileExpr(env, obj)
              val lam = ts.caseFields.foldRight('{ SIR.Var(NamedDeBruijn(${ Expr(ident) })) }) {
                case (f, acc) =>
                  '{ SIR.LamAbs(${ Expr(f.name) }, $acc) }
              }
              '{ SIR.Apply($lhs, $lam) }
            // else if obj.symbol.isPackageDef then
            // compileExpr(env, obj)
            else if isConstructorVal(e.symbol, e.tpe) then
              compileNewConstructor(env, e, e.tpe, Nil)
            else
              // report.errorAndAbort(s"SELECT: ${obj.symbol.isPackageDef}", sel.pos)
              if !env.contains(e.symbol) then
                val b = compileStmt(immutable.HashSet.empty, e.symbol.tree.asInstanceOf[Definition])
                globalDefs.update(b.symbol, b)
              end if
              '{ SIR.Var(NamedDeBruijn(${ Expr(e.symbol.fullName) })) }
          case TypeApply(f, args) => compileExpr(env, f)
          // Generic Apply
          case Apply(f, args) =>
            val fE = compileExpr(env, f)
            val argsE = args.map(compileExpr(env, _))
            if argsE.isEmpty then '{ SIR.Apply($fE, SIR.Const(scalus.uplc.Constant.Unit)) }
            else argsE.foldLeft(fE)((acc, arg) => '{ SIR.Apply($acc, $arg) })
          // (x: T) => body
          case Block(
                immutable.List(
                  DefDef("$anonfun", immutable.List(TermParamClause(args)), tpe, Some(body))
                ),
                Closure(Ident("$anonfun"), _)
              ) =>
            val bodyExpr: Expr[scalus.sir.SIR] = {
              if args.isEmpty then
                val bE = compileExpr(env, body)
                '{ SIR.LamAbs("_", $bE) }
              else
                val names = args.map { case v @ ValDef(name, tpe, rhs) => v.symbol }
                val bE = compileExpr(env ++ names, body)
                names.foldRight(bE) { (name, acc) =>
                  '{ SIR.LamAbs(${ Expr(name.name) }, $acc) }
                }
            }
            bodyExpr
          case Block(stmt, expr) => compileBlock(env, stmt, expr)
          case Typed(expr, _)    => compileExpr(env, expr)
          case Inlined(_, bindings, expr) =>
            val r = compileBlock(env, bindings, expr)
            val t = r.asTerm.show
            // report.info(s"Inlined: ${bindings}, ${expr.show}\n${t}", Position(SourceFile.current, globalPosition, 0))
            r
          case x => report.errorAndAbort(s"Unsupported expression: ${x.show}\n$x")
    }

    // report.info(s"Compiling ${e.asTerm.show}\n${e.asTerm}", e.asTerm.pos)
    val result = compileExpr(immutable.HashSet.empty, e.asTerm)
//    report.info(s"Glogal defs: ${globalDefs}")

    val full = globalDefs.foldRight(result) { case ((_, b), acc) =>
      '{
        SIR.Let(${ Expr(b.recursivity) }, List(Binding(${ Expr(b.fullName) }, ${ b.body })), $acc)
      }
    }
    val dataDecls = globalDataDecls.foldRight(full){ case ((_, decl), acc) =>
      '{ SIR.Decl($decl, $acc) }
    }
    dataDecls
}
