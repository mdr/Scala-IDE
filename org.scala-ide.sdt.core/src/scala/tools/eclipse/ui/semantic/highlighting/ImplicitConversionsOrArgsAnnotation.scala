package scala.tools.eclipse.ui.semantic.highlighting

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationPresentation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.graphics.Color
import org.eclipse.jface.text.source.AnnotationPainter;

class ImplicitConversionsOrArgsAnnotation(kind: String, isPersistent: Boolean, text: String) extends Annotation(kind, isPersistent, text)

object ImplicitConversionsOrArgsAnnotation {
	final val KIND = "scala.tools.eclipse.ui.semantic.highlighting.implicitConversionsOrArgsAnnotation" 
}


class ImplicitConversionsOrArgsDrawingStrategy(fUnderlineStyle: Int, fFontStyle: Int) extends AnnotationPainter.ITextStyleStrategy {
	
    def applyTextStyle(styleRange: StyleRange, annotationColor: Color) {
    	    styleRange.fontStyle = fFontStyle
    	    if (fUnderlineStyle==8) {
    	    	styleRange.underline= false
    	    	return
    	    }
    	    styleRange.underline= true
			styleRange.underlineStyle= fUnderlineStyle 
			styleRange.underlineColor= annotationColor 
	}
	
}