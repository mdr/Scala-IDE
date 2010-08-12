package scala.tools.eclipse.semantichighlighting

import scala.tools.eclipse.util.Colors
import org.eclipse.swt.graphics.Color
import org.eclipse.ui.texteditor.AnnotationPreference

object SymbolAnnotations {

  val PREFIX = "scala.tools.eclipse.semantichighlighting"

  val TEXT_PREFERENCE_KEY = PREFIX + ".text"

  val FOREGROUND_COLOUR_STYLE = PREFIX + ".foregroundColourStyle"

  import SymbolTypes._
  val allSymbolAnnotations: Map[SymbolType, AnnotationInfo] = Map(
    LocalVal -> LocalValAnnotation,
    LocalVar -> LocalVarAnnotation,
    TemplateVar -> TemplateVarAnnotation,
    TemplateVal -> TemplateValAnnotation,
    Method -> MethodAnnotation,
    MethodParam -> MethodParamAnnotation,
    Other -> OtherAnnotation)
}

import SymbolAnnotations._

abstract class AnnotationInfo(name: String, val defaultColour: Color) {

  val annotationId = PREFIX + "." + name

  val colourPreferenceKey = annotationId + ".colour"

  val stylePreferenceKey = annotationId + ".style"

  val annotationPreference = new AnnotationPreferenceWithForegroundColourStyle(annotationId, colourPreferenceKey, SymbolAnnotations.TEXT_PREFERENCE_KEY, stylePreferenceKey)

}

object LocalValAnnotation extends AnnotationInfo("localVal", Colors.ocean)
object LocalVarAnnotation extends AnnotationInfo("localVar", Colors.cayenne)
object TemplateValAnnotation extends AnnotationInfo("templateVal", Colors.rgb(0, 0, 192))
object TemplateVarAnnotation extends AnnotationInfo("templateVar", Colors.salmon)
object MethodAnnotation extends AnnotationInfo("method", Colors.iron)
object MethodParamAnnotation extends AnnotationInfo("methodParam", Colors.eggplant)
object OtherAnnotation extends AnnotationInfo("other", Colors.black)
