package scala.tools.eclipse.ui

import org.eclipse.jface.text._
import scalariform.lexer.ScalaLexer
import scalariform.lexer.Token
import scalariform.lexer.Tokens._

class ScalaAutoIndentStrategy2 extends IAutoEditStrategy {

  def customizeDocumentCommand(document: IDocument, command: DocumentCommand) {
    val offset = command.offset
    if (offset == -1)
      return

    if (command.length == 0 && command.text != null && isLineDelimiter(document, command.text)) {

      // Find all tokens between start of current line and insert position
      //

      val text = document.get
      val tokens = ScalaLexer.rawTokenise(text, forgiveErrors = true).toArray
      if (tokens.isEmpty)
        return

      var continue = true
      var i = 0
      while (continue) {
        val token = tokens(i)
        if (offset >= token.startIndex && offset <= token.startIndex + token.length)
          continue = false
        else
          i += 1
      }

      val finalTokenIndex = i

      continue = true
      i = finalTokenIndex
      while (continue) {
        if (i == -1)
          continue = false
        else {
          val token = tokens(i)
          if (token.tokenType == WS) {
            if (token.getText.lastIndexOf('\n') >= 0)
              continue = false
            else
              i -= 1
          } else
            i -= 1
        }
      }
      val startTokenIndex = if (i == 0) None else Some(i)
      
    }
  }

  private def contains(token: Token, offset: Int) = {
    offset >= token.startIndex && offset < token.startIndex + token.length

  }

  private def isLineDelimiter(document: IDocument, text: String) = document.getLegalLineDelimiters match {
    case null => false
    case delimiters => TextUtilities.equals(delimiters, text) > -1
  }

}