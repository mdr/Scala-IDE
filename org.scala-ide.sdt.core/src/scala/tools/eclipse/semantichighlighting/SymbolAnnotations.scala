package scala.tools.eclipse.semantichighlighting

import scala.tools.eclipse.util.Colors
import org.eclipse.swt.graphics.Color
import org.eclipse.ui.texteditor.AnnotationPreference
import SymbolAnnotations._
import scala.tools.eclipse.properties.ScalaSyntaxClass
import scala.tools.eclipse.properties.ScalaSyntaxClasses
import scala.tools.eclipse.ScalaPlugin
import org.eclipse.jdt.internal.ui.JavaPlugin

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
    MethodParam -> MethodParamAnnotation)
}

abstract class AnnotationInfo(name: String, val syntaxClass: ScalaSyntaxClass) {

  val annotationId = PREFIX + "." + name

  val defaultColour = syntaxClass.getTextAttribute(ScalaPlugin.plugin.getPreferenceStore).getForeground

  val colourPreferenceKey = syntaxClass.colourKey

  val stylePreferenceKey = annotationId + ".style"

  val annotationPreference = new AnnotationPreferenceWithForegroundColourStyle(annotationId, colourPreferenceKey,
    SymbolAnnotations.TEXT_PREFERENCE_KEY, stylePreferenceKey)

}

object LocalValAnnotation extends AnnotationInfo("localVal", ScalaSyntaxClasses.LOCAL_VAL)
object LocalVarAnnotation extends AnnotationInfo("localVar", ScalaSyntaxClasses.LOCAL_VAR)
object TemplateValAnnotation extends AnnotationInfo("templateVal", ScalaSyntaxClasses.TEMPLATE_VAL)
object TemplateVarAnnotation extends AnnotationInfo("templateVar", ScalaSyntaxClasses.TEMPLATE_VAR)
object MethodAnnotation extends AnnotationInfo("method", ScalaSyntaxClasses.METHOD)
object MethodParamAnnotation extends AnnotationInfo("methodParam", ScalaSyntaxClasses.METHOD_PARAM)
