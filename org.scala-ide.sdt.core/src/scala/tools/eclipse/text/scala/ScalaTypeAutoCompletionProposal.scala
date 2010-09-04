package scala.tools.eclipse.text.scala;
import java.io.StringWriter
import java.util.WeakHashMap
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.Image

import org.eclipse.jface.text.IDocument
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.contentassist.IContextInformation
import org.eclipse.jface.text.IDocumentListener;
import scala.tools.eclipse.javaelements.ScalaCompilationUnit
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal

import scala.tools.eclipse.ScalaWordFinder

class ScalaTypeAutoCompletionProposal(scu: ScalaCompilationUnit, doc: IDocument) extends IJavaCompletionProposal with IDocumentListener {

  def apply(document: IDocument): Unit = {
	document.replace(offset+1, 0, " "+currentTyp);
  }

  def getDisplayString(): String = currentTyp 

  def getImage(): Image = org.eclipse.jdt.internal.ui.JavaPluginImages.get(org.eclipse.jdt.internal.ui.JavaPluginImages.IMG_OBJS_CLASS)

  def getContextInformation(): IContextInformation = { null }
  
  def getSelection(document: IDocument): Point = { null }

  def getAdditionalProposalInfo(): String = { null }
  
  def getRelevance() = Integer.MAX_VALUE //temporary hack for jdt 

};