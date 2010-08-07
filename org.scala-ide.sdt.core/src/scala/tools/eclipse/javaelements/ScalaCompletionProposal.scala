/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse.javaelements

import org.eclipse.jdt.internal.codeassist.InternalCompletionProposal

class ScalaCompletionProposal(kind : Int, completionLocation : Int)
  extends InternalCompletionProposal(kind, completionLocation) 
  with scala.tools.eclipse.contribution.weaving.jdt.IScalaCompletionProposal {
  var suppressArgList = false
}
