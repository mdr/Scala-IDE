package scala.tools.eclipse.properties

import org.eclipse.jdt.ui.PreferenceConstants._
import org.eclipse.jdt.ui.text.IJavaColorConstants._

object ScalaSyntaxClasses {

  val SINGLE_LINE_COMMENT = ScalaSyntaxClass("Single-line comment", "syntaxColouring.singleLineComment")
  val MULTI_LINE_COMMENT = ScalaSyntaxClass("Multi-line comment", "syntaxColouring.multiLineComment")
  val SCALADOC = ScalaSyntaxClass("Scaladoc comment", "syntaxColouring.scaladoc")
  val OPERATOR = ScalaSyntaxClass("Operator", "syntaxColouring.operator")
  val KEYWORD = ScalaSyntaxClass("Keywords (excluding 'return')", "syntaxColouring.keyword")
  val RETURN = ScalaSyntaxClass("Keyword 'return'", "syntaxColouring.return")
  val STRING = ScalaSyntaxClass("Strings", "syntaxColouring.string")
  val MULTI_LINE_STRING = ScalaSyntaxClass("Multi-line string", "syntaxColouring.multiLineString")
  val BRACKET = ScalaSyntaxClass("Brackets", "syntaxColouring.bracket")
  val DEFAULT = ScalaSyntaxClass("Others", "syntaxColouring.default")

  val XML_COMMENT = ScalaSyntaxClass("Comments", "syntaxColouring.xml.comment")
  val XML_ATTRIBUTE_VALUE = ScalaSyntaxClass("Attribute values", "syntaxColouring.xml.attributeValue")
  val XML_ATTRIBUTE_NAME = ScalaSyntaxClass("Attribute names", "syntaxColouring.xml.attributeName")
  val XML_ATTRIBUTE_EQUALS = ScalaSyntaxClass("Attribute equal signs", "syntaxColouring.xml.equals")
  val XML_TAG_DELIMITER = ScalaSyntaxClass("Tag delimiters", "syntaxColouring.xml.tagDelimiter")
  val XML_TAG_NAME = ScalaSyntaxClass("Tag names", "syntaxColouring.xml.tagName")
  val XML_PI = ScalaSyntaxClass("Processing instructions", "syntaxColouring.xml.processingInstruction")
  val XML_CDATA_BORDER = ScalaSyntaxClass("CDATA delimiters", "syntaxColouring.xml.cdata")

  val LOCAL_VAL = ScalaSyntaxClass("Local val", "syntaxColouring.semantic.localVal", canBeDisabled = true)
  val LOCAL_VAR = ScalaSyntaxClass("Local var", "syntaxColouring.semantic.localVar", canBeDisabled = true)
  val TEMPLATE_VAL = ScalaSyntaxClass("Template val", "syntaxColouring.semantic.templateVal", canBeDisabled = true)
  val TEMPLATE_VAR = ScalaSyntaxClass("Template var", "syntaxColouring.semantic.templateVar", canBeDisabled = true)
  val METHOD = ScalaSyntaxClass("Method", "syntaxColouring.semantic.method", canBeDisabled = true)
  val METHOD_PARAM = ScalaSyntaxClass("Method parameter", "syntaxColouring.semantic.methodParam", canBeDisabled = true)

  case class Category(name: String, children: List[ScalaSyntaxClass])

  val scalaCategory = Category("Scala", List(
    BRACKET, KEYWORD, LOCAL_VAL, LOCAL_VAR, RETURN, METHOD, METHOD_PARAM, MULTI_LINE_STRING, OPERATOR,
    TEMPLATE_VAL, TEMPLATE_VAR, DEFAULT, STRING))

  val commentsCategory = Category("Comments", List(
    SINGLE_LINE_COMMENT, MULTI_LINE_COMMENT, SCALADOC))

  val xmlCategory = Category("XML", List(
    XML_ATTRIBUTE_NAME, XML_ATTRIBUTE_VALUE, XML_ATTRIBUTE_EQUALS, XML_CDATA_BORDER, XML_COMMENT, XML_TAG_DELIMITER, XML_TAG_NAME, XML_PI))

  val categories = List(scalaCategory, commentsCategory, xmlCategory)

  val ALL_SYNTAX_CLASSES = categories.flatMap(_.children)

  val ENABLED_SUFFIX = ".enabled"
  val COLOUR_SUFFIX = ".colour"
  val BOLD_SUFFIX = ".bold"
  val ITALIC_SUFFIX = ".italic"
  val STRIKETHROUGH_SUFFIX = ".strikethrough"
  val UNDERLINE_SUFFIX = ".underline"

  val ALL_SUFFIXES = List(ENABLED_SUFFIX, COLOUR_SUFFIX, BOLD_SUFFIX, ITALIC_SUFFIX, STRIKETHROUGH_SUFFIX, UNDERLINE_SUFFIX)

  val ALL_KEYS = (for {
    syntaxClass <- ALL_SYNTAX_CLASSES
    suffix <- ALL_SUFFIXES
  } yield syntaxClass.baseName + suffix).toSet

}