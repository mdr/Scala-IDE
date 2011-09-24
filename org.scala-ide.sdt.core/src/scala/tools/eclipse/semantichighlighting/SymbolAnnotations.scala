package scala.tools.eclipse.semantichighlighting

import scala.tools.eclipse.properties.ScalaSyntaxClass
import scala.tools.eclipse.properties.ScalaSyntaxClasses
import scala.tools.eclipse.semantichighlighting.classifier.SymbolType
import scala.tools.eclipse.semantichighlighting.classifier.SymbolTypes
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.text.source.AnnotationPainter
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport

/**
 * Misc wiring for semantic highlighting annotations
 */
object SymbolAnnotations {

  private val TEXT_PREFERENCE_KEY = "scala.tools.eclipse.semantichighlighting.text"

  private def annotationType(syntaxClass: ScalaSyntaxClass, deprecated: Boolean) =
    syntaxClass.baseName + (if (deprecated) ".deprecated" else "") + ".annotationType"

  private def paintingStrategyId(syntaxClass: ScalaSyntaxClass, deprecated: Boolean) =
    syntaxClass.baseName + (if (deprecated) ".deprecated" else "") + ".paintingStrategyId"

  // Used to look up the paintingStrategyId in a pref store
  private def stylePreferenceKey(syntaxClass: ScalaSyntaxClass, deprecated: Boolean) =
    syntaxClass.baseName + (if (deprecated) ".deprecated" else "") + ".stylePreferenceKey"

  def initAnnotationPreferences(javaPrefStore: IPreferenceStore) {
    javaPrefStore.setDefault(TEXT_PREFERENCE_KEY, true)
    for {
      syntaxClass <- ScalaSyntaxClasses.scalaSemanticCategory.children
      deprecated <- List(false, true)
      paintingId = paintingStrategyId(syntaxClass, deprecated)
      preferenceKey = stylePreferenceKey(syntaxClass, deprecated)
    } javaPrefStore.setDefault(preferenceKey, paintingId)
  }

  def symbolAnnotation(symbolType: SymbolType, deprecated: Boolean) = {
    val syntaxClass = SymbolAnnotations.symbolTypeToSyntaxClass(symbolType)
    new SymbolAnnotation(annotationType(syntaxClass, deprecated))
  }

  def addAnnotationPreferences(support: SourceViewerDecorationSupport) =
    for {
      syntaxClass <- ScalaSyntaxClasses.scalaSemanticCategory.children
      deprecated <- List(false, true)
    } support.setAnnotationPreference(annotationPreference(syntaxClass, deprecated))

  private def annotationPreference(syntaxClass: ScalaSyntaxClass, deprecated: Boolean) =
    new AnnotationPreferenceWithForegroundColourStyle(
      annotationType(syntaxClass, deprecated),
      TEXT_PREFERENCE_KEY,
      stylePreferenceKey(syntaxClass, deprecated))

  def addTextStyleStrategies(annotationPainter: AnnotationPainter) {
    for {
      syntaxClass <- ScalaSyntaxClasses.scalaSemanticCategory.children
      deprecated <- List(false, true)
      paintingStrategyId = SymbolAnnotations.paintingStrategyId(syntaxClass, deprecated)
      textStyleStrategy = new SymbolTextStyleStrategy(syntaxClass, deprecated)
    } annotationPainter.addTextStyleStrategy(paintingStrategyId, textStyleStrategy)
  }

  private val symbolTypeToSyntaxClass: Map[SymbolType, ScalaSyntaxClass] = {
    import SymbolTypes._
    import ScalaSyntaxClasses._
    Map(
      Annotation -> ANNOTATION,
      CaseClass -> CASE_CLASS,
      CaseObject -> CASE_OBJECT,
      Class -> CLASS,
      LazyLocalVal -> LAZY_LOCAL_VAL,
      LazyTemplateVal -> LAZY_TEMPLATE_VAL,
      LocalVal -> LOCAL_VAL,
      LocalVar -> LOCAL_VAR,
      Method -> METHOD,
      Param -> PARAM,
      Object -> OBJECT,
      Package -> PACKAGE,
      TemplateVar -> TEMPLATE_VAR,
      TemplateVal -> TEMPLATE_VAL,
      Trait -> TRAIT,
      Type -> TYPE,
      TypeParameter -> TYPE_PARAMETER)
  }
}
