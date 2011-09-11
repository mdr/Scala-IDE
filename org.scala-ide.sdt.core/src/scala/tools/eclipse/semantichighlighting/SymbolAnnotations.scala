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
    Class -> ClassAnnotation,
    LocalVal -> LocalValAnnotation,
    LocalVar -> LocalVarAnnotation,
    Method -> MethodAnnotation,
    Param -> ParamAnnotation,
    Object -> ObjectAnnotation,
    Package -> PackageAnnotation,
    TemplateVar -> TemplateVarAnnotation,
    TemplateVal -> TemplateValAnnotation,
    Trait -> TraitAnnotation,
    Type -> TypeAnnotation,
    TypeParameter -> TypeParamAnnotation)
}

abstract class AnnotationInfo(name: String, val syntaxClass: ScalaSyntaxClass) {

  val annotationId = PREFIX + "." + name

  val defaultColour = syntaxClass.getTextAttribute(ScalaPlugin.plugin.getPreferenceStore).getForeground

  val colourPreferenceKey = syntaxClass.colourKey

  val stylePreferenceKey = annotationId + ".style"

  val annotationPreference = new AnnotationPreferenceWithForegroundColourStyle(annotationId, colourPreferenceKey,
    SymbolAnnotations.TEXT_PREFERENCE_KEY, stylePreferenceKey)

}

object ClassAnnotation extends AnnotationInfo("class", ScalaSyntaxClasses.CLASS)
object LocalValAnnotation extends AnnotationInfo("localVal", ScalaSyntaxClasses.LOCAL_VAL)
object LocalVarAnnotation extends AnnotationInfo("localVar", ScalaSyntaxClasses.LOCAL_VAR)
object MethodAnnotation extends AnnotationInfo("method", ScalaSyntaxClasses.METHOD)
object ParamAnnotation extends AnnotationInfo("methodParam", ScalaSyntaxClasses.PARAM)
object ObjectAnnotation extends AnnotationInfo("object", ScalaSyntaxClasses.OBJECT)
object PackageAnnotation extends AnnotationInfo("package", ScalaSyntaxClasses.PACKAGE)
object TemplateValAnnotation extends AnnotationInfo("templateVal", ScalaSyntaxClasses.TEMPLATE_VAL)
object TemplateVarAnnotation extends AnnotationInfo("templateVar", ScalaSyntaxClasses.TEMPLATE_VAR)
object TraitAnnotation extends AnnotationInfo("trait", ScalaSyntaxClasses.TRAIT)
object TypeAnnotation extends AnnotationInfo("type", ScalaSyntaxClasses.TYPE)
object TypeParamAnnotation extends AnnotationInfo("typeParam", ScalaSyntaxClasses.TYPE_PARAMETER)
