package scala.tools.eclipse.semantichighlighting

import org.eclipse.jface.text.Region

case class SymbolInfo(symbolType: SymbolType, regions: List[Region])

sealed trait SymbolType

object SymbolTypes {
  case object LocalVar extends SymbolType
  case object LocalVal extends SymbolType
  case object TemplateVar extends SymbolType
  case object TemplateVal extends SymbolType
  case object Method extends SymbolType
  case object MethodParam extends SymbolType
  case object Other extends SymbolType

}