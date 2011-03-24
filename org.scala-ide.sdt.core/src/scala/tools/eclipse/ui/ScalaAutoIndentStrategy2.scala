package scala.tools.eclipse.ui

import scalariform.lexer.TokenType
import scalariform.lexer.Tokens
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

      def previousAndCurrentTokenPos(): (Option[Int], Option[Int]) = {
        var i = 0
        for (token <- tokens) {
          if (offset == token.startIndex)
            return (if (i > 0) Some(i - 1) else None, None)
          else if (offset > token.startIndex && offset < token.startIndex + token.length) {
            return (if (i > 0) Some(i - 1) else None, Some(i))
          } else
            i += 1
        }
        throw new AssertionError("Should not reach here")
      }

      def scanBackToNewline(start: Int): (List[Token], Int) = {
        var i = start
        while (true) {
          val token = tokens(i)
          val text = token.getText
          if (token.tokenType == WS && text.lastIndexOf('\n') >= 0) {
            val indent = text.drop(text.lastIndexOf('\n') + 1).takeWhile { _ == ' ' }.length
            return (tokens.slice(i, start + 1).toList, indent)
          } else if (i == 0) {
            return (tokens.take(start + 1).toList, 0)
          } else {
            i = i - 1
          }
        }
        throw new AssertionError("Should not reach here")
      }

      val (lineTokens, currentIndent) = previousAndCurrentTokenPos match {
        case (Some(previous), _) => scanBackToNewline(previous)
        case (None, _) => (Nil, 0)
      }

      val midTokenOpt = previousAndCurrentTokenPos._2
      println(currentIndent, lineTokens, midTokenOpt)

      var stack = List[TokenType]()
      for (token <- lineTokens) {
        token.tokenType match {
          case Tokens.LBRACE =>
            stack ::= token.tokenType
          case Tokens.LPAREN =>
            stack ::= token.tokenType
          case Tokens.RPAREN =>
            stack match {
              case LPAREN :: rest =>
                stack = rest
              case _ =>
            }
          case Tokens.RBRACE =>
            stack match {
              case LBRACE :: rest =>
                stack = rest
              case _ =>
            }
          case _ =>
        }
      }
      val newIndent = currentIndent + (if (stack.isEmpty) 0 else 2)
      command.text = command.text + " " * newIndent 
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