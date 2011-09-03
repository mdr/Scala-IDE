package scala.tools.eclipse.properties

import scala.tools.eclipse.properties.ScalaSyntaxClasses._
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.swt.graphics.RGB
import org.eclipse.swt.SWT
import org.eclipse.jface.text._
import scala.tools.eclipse.ScalaPlugin
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.swt.custom.StyleRange
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.swt.graphics.Color

case class ScalaSyntaxClass(displayName: String, baseName: String, canBeDisabled: Boolean = false) {

  import ScalaSyntaxClasses._
  def enabledKey = baseName + ENABLED_SUFFIX
  def colourKey = baseName + COLOUR_SUFFIX
  def boldKey = baseName + BOLD_SUFFIX
  def italicKey = baseName + ITALIC_SUFFIX
  def underlineKey = baseName + UNDERLINE_SUFFIX
  def strikethroughKey = baseName + STRIKETHROUGH_SUFFIX

  def getTextAttribute(preferenceStore: IPreferenceStore): TextAttribute = {
    val styleInfo = getStyleInfo(preferenceStore)
    val style: Int = fullStyle(styleInfo)
    val backgroundColour = null
    new TextAttribute(styleInfo.foreground, null, style)
  }

  private val colourManager = JavaPlugin.getDefault.getJavaTextTools.getColorManager

  def getStyleRange(preferenceStore: IPreferenceStore): StyleRange = {
    val styleRange = new StyleRange
    populateStyleRange(styleRange, preferenceStore)
    styleRange
  }

  def populateStyleRange(styleRange: StyleRange, preferenceStore: IPreferenceStore) =
    if (preferenceStore.getBoolean(enabledKey)) {
      val StyleInfo(enabled, foregroundColour, bold, italic, strikethrough, underline) = getStyleInfo(preferenceStore)
      val style = basicStyle(bold, italic)
      styleRange.fontStyle = style
      styleRange.foreground = foregroundColour
      styleRange.underline = underline
      styleRange.underlineColor = styleRange.foreground
      styleRange.strikeout = strikethrough
    }

  case class StyleInfo(enabled: Boolean, foreground: Color, bold: Boolean, italic: Boolean, strikethrough: Boolean, underline: Boolean)
  def getStyleInfo(preferenceStore: IPreferenceStore): StyleInfo = {
    StyleInfo(
      preferenceStore.getBoolean(enabledKey),
      colourManager.getColor(PreferenceConverter.getColor(preferenceStore, colourKey)),
      preferenceStore.getBoolean(boldKey),
      preferenceStore.getBoolean(italicKey),
      preferenceStore.getBoolean(strikethroughKey),
      preferenceStore.getBoolean(underlineKey))
  }

  private def basicStyle(bold: Boolean, italic: Boolean): Int = {
    var style = SWT.NORMAL
    if (bold) style |= SWT.BOLD
    if (italic) style |= SWT.ITALIC
    style
  }

  private def fullStyle(styleInfo: StyleInfo): Int = {
    val StyleInfo(_, _, bold, italic, strikethrough, underline) = styleInfo
    var style = basicStyle(bold, italic)
    if (strikethrough) style |= TextAttribute.STRIKETHROUGH
    if (underline) style |= TextAttribute.UNDERLINE
    style
  }

}
