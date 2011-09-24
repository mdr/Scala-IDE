package scala.tools.eclipse.semantichighlighting

import org.eclipse.jface.text.source.Annotation

class SymbolAnnotation(annotationType: String) extends Annotation(annotationType, false, null) {

  override lazy val toString = getClass.getSimpleName + "(" + annotationType + ")"

}