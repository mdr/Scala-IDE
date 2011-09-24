package scala.tools.eclipse.semantichighlighting.classifier

import scala.tools.nsc.interactive.Global
import scala.tools.nsc.util.SourceFile
import scala.tools.refactoring.analysis.GlobalIndexes
import scala.tools.refactoring.common.Selections
import scala.tools.eclipse.semantichighlighting.classifier.SymbolTypes._
import scala.PartialFunction.{ cond, condOpt }
import scala.tools.nsc.util.BatchSourceFile
import scala.tools.nsc.util.RangePosition
import scalariform.parser.ScalaParser
import scalariform.lexer.ScalaLexer
import scalariform.parser.{ Type => _, Param => _, Annotation => _, _ }
import scalariform.parser.Argument
import scalariform.utils.Utils.time

class SymbolClassifier(
  protected val sourceFile: SourceFile,
  g: Global,
  useScalariform: Boolean)
    extends Selections
    with GlobalIndexes
    with Debugger
    with SymbolTests
    with CompilerStuff {

  import global._
  val global = g

  lazy val source = sourceFile.content.mkString

  lazy val SyntacticInfo(namedArgs, forVals, maybeSelfRefs, maybeClassOfs, annotations) =
    if (useScalariform) SyntacticInfo.getSyntacticInfo(source) else SyntacticInfo(Set(), Set(), Set(), Set(), Set())

  lazy val forValSymbols: Set[Symbol] = for {
    Region(offset, length) <- forVals
    pos = new RangePosition(sourceFile, offset, offset, offset + length - 1)
    symbol <- index.positionToSymbol(pos)
  } yield symbol

  private def treeNameRegion(tree: Tree): Option[Region] =
    try
      condOpt(tree.namePosition) {
        case rangePosition: RangePosition => Region(rangePosition.start, rangePosition.end - rangePosition.start)
      }
    catch {
      case e => None
    }

  def handleSymOccurrence(occurrence: Tree, sym: Symbol): Option[Region] = treeNameRegion(occurrence) flatMap { region =>
    val Region(start, length) = region
    val end = start + length
    val text = source.slice(start, end)
    val symName = sym.nameString
    condOpt(occurrence) {

      // Compensate for range pos being two chars too long for certain trees 
      case _: Select | _: Ident if source.slice(start, end - 2) == "`" + symName + "`" =>
        Region(start, length - 2)

      // compensate for apparent off-by-one error in pos for cases like "var Some(xxx) = Some(3)":
      case _: ValDef if source.slice(start + 1, end + 1) == symName =>
        Region(start + 1, length)

      case _: PackageDef | _: TypeDef | _: ClassDef | _: ModuleDef | _: ImportSelectorTree | _: Select |
        _: DefDef | _: ValDef | _: Ident | _: Bind if text == symName || text == "`" + symName + "`" =>
        region
    }
  }

  def handleSym(sym: Symbol): SymbolInfo = {
    val symbolType = getSymbolType(sym)
    val regions = occurrences(sym) flatMap { handleSymOccurrence(_, sym) }
    SymbolInfo(symbolType, regions, sym.isDeprecated)
  }

  def classifySymbols: List[SymbolInfo] = {

    if (true)
      printSymbolInfo()
    time("classifySymbols") {
      val allSymbols = index.allSymbols
      val basicSymbolInfos = allSymbols map handleSym

      val localVars: Set[Region] = basicSymbolInfos.collect { case SymbolInfo(LocalVar, regions, _) => regions }.flatten.toSet
      val templateValsAndVars: Set[Region] = basicSymbolInfos.collect { case SymbolInfo(TemplateVal | TemplateVar, regions, _) => regions }.flatten.toSet
      val types: Set[Region] = basicSymbolInfos.collect { case SymbolInfo(Type, regions, _) => regions }.flatten.toSet
      //      val objects: Set[Region] = basicSymbolInfos.collect { case SymbolInfo(Object, regions) => regions }.flatten.toSet
      val classes: Set[Region] = basicSymbolInfos.collect { case SymbolInfo(Class, regions, _) => regions }.flatten.toSet
      val caseClasses: Set[Region] = basicSymbolInfos.collect { case SymbolInfo(CaseClass, regions, _) => regions }.flatten.toSet

      val all: Set[Region] = basicSymbolInfos.flatMap(_.regions).toSet

      val assistsFromSyntax = List(
        SymbolInfo(LocalVal, forVals toList, deprecated = false),
        SymbolInfo(Param, namedArgs filterNot localVars toList, deprecated = false),
        SymbolInfo(TemplateVal, maybeSelfRefs filterNot all toList, deprecated = false),
        SymbolInfo(Method, maybeClassOfs filterNot all toList, deprecated = false),
        SymbolInfo(Annotation, annotations filterNot all toList, deprecated = false))

      // Sometimes a symbol gets classified as more than one type -- some simple rules to give one precedence
      def pruneMisidentifiedSymbols(symbolInfo: SymbolInfo): SymbolInfo = symbolInfo match {
        case SymbolInfo(LocalVal, regions, _) => symbolInfo.copy(regions = regions filterNot localVars filterNot templateValsAndVars)
        case SymbolInfo(Method, regions, _) => symbolInfo.copy(regions = regions filterNot templateValsAndVars)
        case SymbolInfo(Class, regions, _) => symbolInfo.copy(regions = regions filterNot types filterNot caseClasses)
        case SymbolInfo(Object, regions, _) => symbolInfo.copy(regions = regions filterNot classes)

        case x => x
      }

      (assistsFromSyntax ++ basicSymbolInfos.map(pruneMisidentifiedSymbols)) filter { _.regions.nonEmpty }
    }
  }

}
