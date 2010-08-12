package scala.tools.eclipse.semantichighlighting

import scala.tools.eclipse.util.Colors
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.core.runtime.preferences.{ AbstractPreferenceInitializer, DefaultScope }

import scala.tools.eclipse.ScalaPlugin
import scalariform.formatter._
import scalariform.formatter.preferences._

class ScalaHighlightingPreferenceInitializer extends AbstractPreferenceInitializer {

  def initializeDefaultPreferences() =
    ScalaPlugin.plugin.check {
      val preferenceStore = JavaPlugin.getDefault.getPreferenceStore

      preferenceStore.setDefault(SymbolAnnotations.TEXT_PREFERENCE_KEY, true)
      
      for (annotationInfo <- SymbolAnnotations.allSymbolAnnotations.values) {
        val colour = annotationInfo.defaultColour
        val colourPrefString = colour.getRed + "," + colour.getGreen + "," + colour.getBlue 
        preferenceStore.setDefault(annotationInfo.colourPreferenceKey, colourPrefString)
        preferenceStore.setDefault(annotationInfo.stylePreferenceKey, SymbolAnnotations.FOREGROUND_COLOUR_STYLE)
      }
    }
}
