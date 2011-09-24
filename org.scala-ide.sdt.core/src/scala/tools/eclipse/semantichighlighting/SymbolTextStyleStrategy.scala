package scala.tools.eclipse.semantichighlighting

import org.eclipse.swt.custom.StyleRange
import scala.tools.eclipse.properties.ScalaSyntaxClass
import org.eclipse.jface.text.source.AnnotationPainter
import org.eclipse.swt.graphics.Color
import scala.tools.eclipse.ScalaPlugin

class SymbolTextStyleStrategy(syntaxClass: ScalaSyntaxClass, deprecated: Boolean) extends AnnotationPainter.ITextStyleStrategy {

  def applyTextStyle(styleRange: StyleRange, annotationColor: Color) {
    syntaxClass.populateStyleRange(styleRange, ScalaPlugin.plugin.getPreferenceStore)
    styleRange.background = styleRange.background
    if (deprecated)
      styleRange.strikeout = true
  }
  
}