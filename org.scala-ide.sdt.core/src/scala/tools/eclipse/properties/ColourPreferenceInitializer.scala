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

class ColourPreferenceInitializer extends AbstractPreferenceInitializer {

  def initializeDefaultPreferences() {
    val scalaPrefStore = ScalaPlugin.plugin.getPreferenceStore
    val javaPrefStore = JavaPlugin.getDefault.getPreferenceStore

    def setDefaultsForSyntaxClass(syntaxClass: ScalaSyntaxClass, rgb: RGB,
      bold: Boolean = false, italic: Boolean = false, strikethrough: Boolean = false, underline: Boolean = false) =
      {
        val baseName = syntaxClass.baseName
        scalaPrefStore.setDefault(baseName + ENABLED_SUFFIX, false)
        scalaPrefStore.setDefault(baseName + COLOUR_SUFFIX, StringConverter.asString(rgb))
        scalaPrefStore.setDefault(baseName + BOLD_SUFFIX, bold)
        scalaPrefStore.setDefault(baseName + ITALIC_SUFFIX, italic)
        scalaPrefStore.setDefault(baseName + STRIKETHROUGH_SUFFIX, strikethrough)
        scalaPrefStore.setDefault(baseName + UNDERLINE_SUFFIX, underline)

      }

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

    setDefaultsForSyntaxClass(LOCAL_VAL, Colors.ocean.getRGB)
    setDefaultsForSyntaxClass(LOCAL_VAR, Colors.cayenne.getRGB)
    setDefaultsForSyntaxClass(TEMPLATE_VAL, Colors.rgb(0, 0, 192).getRGB)
    setDefaultsForSyntaxClass(TEMPLATE_VAR, Colors.salmon.getRGB)
    setDefaultsForSyntaxClass(METHOD, Colors.iron.getRGB)
    setDefaultsForSyntaxClass(METHOD_PARAM, Colors.eggplant.getRGB)

    javaPrefStore.setDefault(SymbolAnnotations.TEXT_PREFERENCE_KEY, true)

    for (annotationInfo <- SymbolAnnotations.allSymbolAnnotations.values)
      javaPrefStore.setDefault(annotationInfo.stylePreferenceKey, annotationInfo.syntaxClass.baseName)

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
