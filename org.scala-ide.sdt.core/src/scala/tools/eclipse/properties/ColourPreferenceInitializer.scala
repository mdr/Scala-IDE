package scala.tools.eclipse.properties

import scala.tools.eclipse.util.Colors
import scala.tools.eclipse.properties.ScalaSyntaxClasses._
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.semantichighlighting.SymbolAnnotations
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.resource.StringConverter
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.swt.graphics.RGB
import org.eclipse.jface.util.{ IPropertyChangeListener, PropertyChangeEvent }
import org.eclipse.jface.preference.IPreferenceStore
import scala.tools.eclipse.util.SWTUtils._
import org.eclipse.swt.graphics.Color

class ColourPreferenceInitializer extends AbstractPreferenceInitializer {

  def initializeDefaultPreferences() {
    val scalaPrefStore = ScalaPlugin.plugin.getPreferenceStore
    val javaPrefStore = JavaPlugin.getDefault.getPreferenceStore

    def setDefaultsForSyntaxClass(
      syntaxClass: ScalaSyntaxClass,
      foregroundRGB: RGB,
      enabled: Boolean = true,
      backgroundRGBOpt: Option[RGB] = None,
      bold: Boolean = false,
      italic: Boolean = false,
      strikethrough: Boolean = false,
      underline: Boolean = false) =
      {
        scalaPrefStore.setDefault(syntaxClass.enabledKey, enabled)
        scalaPrefStore.setDefault(syntaxClass.foregroundColourKey, StringConverter.asString(foregroundRGB))
        // TODO: Investigate why this blows up
        //        scalaPrefStore.setDefault(syntaxClass.backgroundColourKey, StringConverter.asString(backgroundRGBOpt getOrElse Color.WHITE.getRGB))
        scalaPrefStore.setDefault(syntaxClass.backgroundColourKey, StringConverter.asString(backgroundRGBOpt getOrElse new RGB(255, 255, 255)))
        scalaPrefStore.setDefault(syntaxClass.backgroundColourEnabledKey, backgroundRGBOpt.isDefined)
        scalaPrefStore.setDefault(syntaxClass.boldKey, bold)
        scalaPrefStore.setDefault(syntaxClass.italicKey, italic)
        scalaPrefStore.setDefault(syntaxClass.underlineKey, underline)
      }

    scalaPrefStore.setDefault(USE_ACCURATE_SEMANTIC_HIGHLIGHTING, true)
    scalaPrefStore.setDefault(STRIKETHROUGH_DEPRECATED, true)

    setDefaultsForSyntaxClass(SINGLE_LINE_COMMENT, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(MULTI_LINE_COMMENT, new RGB(63, 127, 95))
    setDefaultsForSyntaxClass(SCALADOC, new RGB(63, 95, 191))
    setDefaultsForSyntaxClass(KEYWORD, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(STRING, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(MULTI_LINE_STRING, new RGB(42, 0, 255))
    setDefaultsForSyntaxClass(DEFAULT, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(OPERATOR, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(BRACKET, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(RETURN, new RGB(127, 0, 85), bold = true)
    setDefaultsForSyntaxClass(BRACKET, new RGB(0, 0, 0))

    // See org.eclipse.wst.xml.ui.internal.preferences.XMLUIPreferenceInitializer
    setDefaultsForSyntaxClass(XML_COMMENT, new RGB(63, 85, 191))
    setDefaultsForSyntaxClass(XML_ATTRIBUTE_VALUE, new RGB(42, 0, 255), italic = true)
    setDefaultsForSyntaxClass(XML_ATTRIBUTE_NAME, new RGB(127, 0, 127))
    setDefaultsForSyntaxClass(XML_ATTRIBUTE_EQUALS, new RGB(0, 0, 0))
    setDefaultsForSyntaxClass(XML_TAG_DELIMITER, new RGB(0, 128, 128))
    setDefaultsForSyntaxClass(XML_TAG_NAME, new RGB(63, 127, 127))
    setDefaultsForSyntaxClass(XML_PI, new RGB(0, 128, 128))
    setDefaultsForSyntaxClass(XML_CDATA_BORDER, new RGB(0, 128, 128))

    setDefaultsForSyntaxClass(ANNOTATION, new RGB(222, 0, 172))
    setDefaultsForSyntaxClass(CASE_CLASS, new RGB(162, 46, 0), bold = true)
    setDefaultsForSyntaxClass(CASE_OBJECT, new RGB(162, 46, 0), bold = true)
    setDefaultsForSyntaxClass(CLASS, new RGB(50, 147, 153))
    setDefaultsForSyntaxClass(LAZY_LOCAL_VAL, new RGB(94, 94, 255))
    setDefaultsForSyntaxClass(LAZY_TEMPLATE_VAL, new RGB(0, 0, 192))
    setDefaultsForSyntaxClass(LOCAL_VAL, new RGB(94, 94, 255))
    setDefaultsForSyntaxClass(LOCAL_VAR, new RGB(255, 94, 94))
    setDefaultsForSyntaxClass(METHOD, new RGB(76, 76, 76), italic = true)
    setDefaultsForSyntaxClass(PARAM, new RGB(100, 0, 103))
    setDefaultsForSyntaxClass(TEMPLATE_VAL, new RGB(0, 0, 192))
    setDefaultsForSyntaxClass(TEMPLATE_VAR, new RGB(192, 0, 0))
    setDefaultsForSyntaxClass(TRAIT, new RGB(50, 147, 153))
    setDefaultsForSyntaxClass(OBJECT, new RGB(50, 147, 153))
    setDefaultsForSyntaxClass(PACKAGE, new RGB(0, 110, 4))
    setDefaultsForSyntaxClass(TYPE, new RGB(50, 147, 153), italic = true)
    setDefaultsForSyntaxClass(TYPE_PARAMETER, new RGB(23, 0, 129), underline = true)

    SymbolAnnotations.initAnnotationPreferences(javaPrefStore)

    mirrorColourPreferencesIntoJavaPreferenceStore(scalaPrefStore, javaPrefStore)
  }

  // Mirror across the colour preferences into the Java preference store so that they can be read by the annotation
  // mechanism.
  private def mirrorColourPreferencesIntoJavaPreferenceStore(scalaPrefStore: IPreferenceStore, javaPrefStore: IPreferenceStore) {
    for (key <- ALL_KEYS) {
      val value = scalaPrefStore.getDefaultString(key)
      javaPrefStore.setDefault(key, value)
    }

    scalaPrefStore.addPropertyChangeListener { event: PropertyChangeEvent =>
      val key = event.getProperty
      if (ALL_KEYS contains key) {
        val value = event.getNewValue
        javaPrefStore.setValue(key, value.toString)
      }
    }

  }

}
