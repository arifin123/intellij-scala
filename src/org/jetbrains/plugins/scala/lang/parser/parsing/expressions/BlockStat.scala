package org.jetbrains.plugins.scala
package lang
package parser
package parsing
package expressions

import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.parsing.base.Import
import org.jetbrains.plugins.scala.lang.parser.parsing.builder.ScalaPsiBuilder
import org.jetbrains.plugins.scala.lang.parser.parsing.statements.{Dcl, Def, EmptyDcl}
import org.jetbrains.plugins.scala.lang.parser.parsing.top.TmplDef
import org.jetbrains.plugins.scala.lang.parser.util.ParserPatcher

/**
* @author Alexander Podkhalyuzin
* Date: 06.03.2008
*/

/*
 * BlockStat ::= Import
 *             | ['implicit'] Def
 *             | {LocalModifier} TmplDef
 *             | Expr1
 */
object BlockStat extends BlockStat {
  override protected def `def` = Def
  override protected def expr1 = Expr1
  override protected def dcl = Dcl
  override protected def tmplDef = TmplDef
  override protected def emptyDcl = EmptyDcl
}

trait BlockStat {
  protected def `def`: Def
  protected def tmplDef: TmplDef
  protected def expr1: Expr1
  protected def dcl: Dcl
  protected def emptyDcl: EmptyDcl

  def parse(builder: ScalaPsiBuilder): Boolean = {
    val tokenType = builder.getTokenType

    val patcher = ParserPatcher.getSuitablePatcher(builder)

    tokenType match {
      case ScalaTokenTypes.kIMPORT =>
        Import parse builder
        return true
      case ScalaTokenTypes.tSEMICOLON =>
        builder.advanceLexer()
        return true
      case ScalaTokenTypes.kDEF | ScalaTokenTypes.kVAL | ScalaTokenTypes.kVAR | ScalaTokenTypes.kTYPE =>
        if (!`def`.parse(builder, isMod = false, isImplicit = true)) {
          if (dcl.parse(builder)) {
            builder error ErrMsg("wrong.declaration.in.block")
            return true
          } else {
            emptyDcl.parse(builder)
            builder error ErrMsg("wrong.declaration.in.block")
            return true
          }
        }
      case ScalaTokenTypes.kCLASS | ScalaTokenTypes.kTRAIT | ScalaTokenTypes.kOBJECT =>
        return tmplDef.parse(builder)
      case _ if patcher.parse(builder) => parse(builder)
      case _ =>
        if (!expr1.parse(builder)) {
          if (!`def`.parse(builder, isMod = false, isImplicit = true)) {
            if (!tmplDef.parse(builder)) {
              if (dcl.parse(builder)) {
                builder error ErrMsg("wrong.declaration.in.block")
                return true
              }
              else {
                if (emptyDcl.parse(builder)) {
                  builder error ErrMsg("wrong.declaration.in.block")
                  return true
                } else
                  return false
              }
            }
          }
        }
    }
    true
  }
}