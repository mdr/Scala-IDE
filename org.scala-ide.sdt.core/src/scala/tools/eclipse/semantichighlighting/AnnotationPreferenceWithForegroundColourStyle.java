package scala.tools.eclipse.semantichighlighting;

import org.eclipse.ui.texteditor.AnnotationPreference;

// Java so we can access the protected static field TEXT_STYLE_PREFERENCE_KEY
public class AnnotationPreferenceWithForegroundColourStyle extends AnnotationPreference {
  
  public AnnotationPreferenceWithForegroundColourStyle(Object annotationType, String colorKey, String textKey, String styleKey) {
    super(annotationType, colorKey, textKey, "not-used", 0);
    setValue(TEXT_STYLE_PREFERENCE_KEY, styleKey);
  }
  
}
