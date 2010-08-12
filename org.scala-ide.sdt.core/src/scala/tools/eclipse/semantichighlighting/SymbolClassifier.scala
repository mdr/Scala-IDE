package scala.tools.eclipse.semantichighlighting

import scala.tools.nsc.interactive.Global
import scala.tools.nsc.util.SourceFile
import org.eclipse.jface.text.Region
import scala.tools.refactoring.analysis.GlobalIndexes
import scala.tools.refactoring.common.Selections
import scala.tools.eclipse.semantichighlighting.SymbolTypes._
import PartialFunction.{ cond, condOpt }

/**
 *  Issues:
 * for (notAParam <- 1 to 10) -- identified as a Param
 */

object SymbolClassifier {

  private val LAZY_SUFFIX = "$lzy"

}

class SymbolClassifier(source: SourceFile, val global: Global) extends Selections with GlobalIndexes {
  import global._
  import SymbolClassifier._

  val index = GlobalIndex(unitOf(source).body)

  def classifySymbols: List[SymbolInfo] = index.allSymbols flatMap { symbol =>
    if (symbol != NoSymbol) {
      import symbol._
      println("Symbol: " + symbol)
      println("  pos: " + symbol.pos)
      println("  class: " + symbol.getClass.getSimpleName)
      println("  Name = >>" + symbol.name + "<<<")
      println("  isTerm: " + isTerm)
      println("  isLocal: " + isLocal)
      println("  isVariable: " + isVariable)
      println("  isParameter: " + isParameter)
      println("  isModule: " + isModule)
      println("  isMethod: " + isMethod)
      println("  isSynthetic: " + isSynthetic)
      println("  isSourceMethod: " + isSourceMethod)
      println("  isGetterOrSetter: " + isGetterOrSetter)
      println("  isGetter: " + isGetter)
      println("  isSetter: " + isSetter)
      println("  isLazy: " + isLazy)
      println("  isLocalDummy: " + isLocalDummy)
      println("  hasGetter: " + hasGetter)
    }
    if (!symbol.isTerm || (symbol.isThisSym && symbol.nameString == "this"))
      None
    else if (!symbol.pos.isRange && index.declaration(symbol).isDefined)
      None
    else if (!symbol.isLocal && symbol.isLazy && symbol.isMethod) // Skip extraneous lazy method symbols
      None
    else
      classifySymbol(symbol) flatMap { symbolType =>
        val baseSymbolName = symbol.nameString
        val symbolName = baseSymbolName match {
          case _ if baseSymbolName endsWith LAZY_SUFFIX => baseSymbolName.substring(0, symbol.nameString.length - 4)
          case UnaryOperator(unaryPrefix) => unaryPrefix
          case _ => baseSymbolName
        }
        val regions = findOccurrences(symbol, baseSymbolName, symbolName)
        Some(SymbolInfo(symbolType, regions))
      }
  }

  private def classifySymbol(symbol: Symbol): Option[SymbolType] = {
    import symbol._
    require(isTerm)
    if (isClassConstructor)
      None
    else if (isMethod)
      Some(if (isLazy || !isSourceMethod) (if (isLocal) LocalVal else TemplateVal) else Method)
    else if (isLocal)
      Some(if (isVariable && !isLazy) LocalVar else if (isParameter) MethodParam else LocalVal)
    else if (!isModule)
      Some(if (isVariable && !isLazy) TemplateVar else TemplateVal)
    else
      None
  }

  private def findOccurrences(symbol: Symbol, baseSymbolName: String, symbolName: String): List[Region] =
    index occurences symbol flatMap { occurrence =>
      val position = occurrence.namePosition
      val extractorUnapply = cond(occurrence) { case Select(qualifier, _) => qualifier.pos == occurrence.pos && Set("unapply", "unapplySeq")(symbolName) }
      lazy val fiveAhead = source.content.slice(position.start, position.start + 5).mkString
      lazy val sixAhead = source.content.slice(position.start, position.start + 6).mkString
      if (position == NoPosition)
        None
      else if (symbol.isThisSym && !occurrence.symbol.isThisSym) // Adjust for incorrect occurrences of self references
        None
      else if (extractorUnapply)
        None
      else if (symbolName == "apply" && fiveAhead != "apply" && sixAhead != "`apply")
        None
      else {
        val start = position.start
        val adjustedStart =
          if (source.content(start) == '`')
            start + 1
          else if (UnaryOperator.unapply(baseSymbolName) exists { _ != source.content(start).toString })
            occurrence.pos.start
          else
            start
        Some(new Region(adjustedStart, symbolName.length))
      }
    }

  private object UnaryOperator {
    def unapply(s: String) = condOpt(s) {
      case "unary_!" => "!"
      case "unary_~" => "~"
      case "unary_+" => "+"
      case "unary_-" => "-"
    }
  }

}
