package scala.tools.eclipse.properties

import java.util.HashMap
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore
import org.eclipse.jdt.internal.ui.preferences.PreferencesMessages
import org.eclipse.jdt.internal.ui.preferences.ScrolledPageContent
import org.eclipse.jdt.ui.PreferenceConstants
import org.eclipse.jdt.ui.text.IColorManager
import org.eclipse.jdt.ui.text.IJavaPartitions
import org.eclipse.jface.layout.PixelConverter
import org.eclipse.jface.preference.ColorSelector
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.preference.PreferenceConverter
import org.eclipse.jface.preference.PreferencePage
import org.eclipse.jface.resource.JFaceResources
import org.eclipse.jface.text.Document
import org.eclipse.jface.text.IDocumentPartitioner
import org.eclipse.jface.text.TextUtilities
import org.eclipse.jface.util.IPropertyChangeListener
import org.eclipse.jface.util.PropertyChangeEvent
import org.eclipse.jface.viewers._
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.graphics.RGB
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Link
import org.eclipse.swt.widgets.Scrollable
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.eclipse.ui.dialogs.PreferencesUtil
import org.eclipse.ui.editors.text.EditorsUI
import org.eclipse.ui.texteditor.ChainedPreferenceStore
import scala.PartialFunction.condOpt
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.ScalaPreviewerFactory
import scala.tools.eclipse.ScalaSourceViewerConfiguration
import scala.tools.eclipse.lexical.ScalaDocumentPartitioner
import scala.tools.eclipse.properties.ScalaSyntaxClasses._
import scala.tools.eclipse.util.EclipseUtils.SelectedItems
import scala.tools.eclipse.util.SWTUtils._
import scalariform.lexer.ScalaLexer
import scalariform.lexer.Token
import org.eclipse.jface.text.source.SourceViewer
import org.eclipse.jdt.internal.ui.JavaPlugin
import org.eclipse.swt.custom.StyleRange

/**
 * @see org.eclipse.jdt.internal.ui.preferences.JavaEditorColoringConfigurationBlock
 */
class SyntaxColouringPreferencePage extends PreferencePage with IWorkbenchPreferencePage {
  import SyntaxColouringPreferencePage._

  setPreferenceStore(ScalaPlugin.plugin.getPreferenceStore)
  private val overlayStore = makeOverlayPreferenceStore

  private var colorEditorLabel: Label = _
  private var syntaxForegroundColorEditor: ColorSelector = _
  private var enabledCheckBox: Button = _
  private var boldCheckBox: Button = _
  private var italicCheckBox: Button = _
  private var underlineCheckBox: Button = _
  private var strikethroughCheckBox: Button = _
  private var treeViewer: TreeViewer = _
  private var previewer: SourceViewer = _

  def init(workbench: IWorkbench) {}

  def createContents(parent: Composite): Control = {
    initializeDialogUnits(parent)

    val scrolled = new ScrolledPageContent(parent, SWT.H_SCROLL | SWT.V_SCROLL)
    scrolled.setExpandHorizontal(true)
    scrolled.setExpandVertical(true)

    val control = createSyntaxPage(scrolled)

    scrolled.setContent(control)
    val size = control.computeSize(SWT.DEFAULT, SWT.DEFAULT)
    scrolled.setMinSize(size.x, size.y)

    scrolled
  }

  def makeOverlayPreferenceStore = {
    import OverlayPreferenceStore._
    val keys = ALL_SYNTAX_CLASSES.flatMap { syntaxClass =>
      List(
        new OverlayKey(BOOLEAN, syntaxClass.enabledKey),
        new OverlayKey(STRING, syntaxClass.colourKey),
        new OverlayKey(BOOLEAN, syntaxClass.boldKey),
        new OverlayKey(BOOLEAN, syntaxClass.italicKey),
        new OverlayKey(BOOLEAN, syntaxClass.strikethroughKey),
        new OverlayKey(BOOLEAN, syntaxClass.underlineKey))
    }.toArray
    new OverlayPreferenceStore(getPreferenceStore, keys)
  }

  override def performOk() = {
    super.performOk()
    overlayStore.propagate()
    ScalaPlugin.plugin.savePluginPreferences()
    true
  }

  override def dispose() {
    overlayStore.stop()
    super.dispose()
  }

  override def performDefaults() {
    super.performDefaults()
    overlayStore.loadDefaults()
    handleSyntaxColorListSelection()
  }

  object TreeContentAndLabelProvider extends LabelProvider with ITreeContentProvider {

    def getElements(inputElement: AnyRef) = categories.toArray

    def getChildren(parentElement: AnyRef) = parentElement match {
      case Category(_, children) => children.toArray
      case _ => Array()
    }

    def getParent(element: AnyRef): Category = categories.find(_.children contains element).orNull

    def hasChildren(element: AnyRef) = getChildren(element).nonEmpty

    def inputChanged(viewer: Viewer, oldInput: AnyRef, newInput: AnyRef) {}

    override def getText(element: AnyRef) = element match {
      case Category(name, _) => name
      case ScalaSyntaxClass(displayName, _, _) => displayName
    }
  }

  def createTreeViewer(editorComposite: Composite) {
    treeViewer = new TreeViewer(editorComposite, SWT.SINGLE | SWT.BORDER)

    treeViewer.setContentProvider(TreeContentAndLabelProvider)
    treeViewer.setLabelProvider(TreeContentAndLabelProvider)

    // scrollbars and tree indentation guess
    val widthHint = ALL_SYNTAX_CLASSES.map { syntaxClass => convertWidthInCharsToPixels(syntaxClass.displayName.length) }.max +
      Option(treeViewer.getControl.asInstanceOf[Scrollable].getVerticalBar).map { _.getSize.x * 3 }.getOrElse(0)

    treeViewer.getControl.setLayoutData(gridData(
      horizontalAlignment = SWT.BEGINNING,
      verticalAlignment = SWT.BEGINNING,
      grabExcessHorizontalSpace = false,
      grabExcessVerticalSpace = true,
      widthHint = widthHint,
      heightHint = convertHeightInCharsToPixels(9)))

    treeViewer.addDoubleClickListener { event: DoubleClickEvent =>
      val element = event.getSelection.asInstanceOf[IStructuredSelection].getFirstElement
      if (treeViewer.isExpandable(element))
        treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element))
    }

    treeViewer.addSelectionChangedListener {
      handleSyntaxColorListSelection()
    }

    treeViewer.setInput(new Object)
  }

  private def gridLayout(marginHeight: Int = 5, marginWidth: Int = 5, numColumns: Int = 1): GridLayout = {
    val layout = new GridLayout
    layout.marginHeight = marginHeight
    layout.marginWidth = marginWidth
    layout.numColumns = numColumns
    layout
  }

  private def gridData(
    horizontalAlignment: Int = SWT.BEGINNING,
    verticalAlignment: Int = SWT.CENTER, //
    grabExcessHorizontalSpace: Boolean = false,
    grabExcessVerticalSpace: Boolean = false,
    widthHint: Int = SWT.DEFAULT,
    heightHint: Int = SWT.DEFAULT,
    horizontalSpan: Int = 1,
    horizontalIndent: Int = 0): GridData =
    {
      val gridData = new GridData(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace,
        grabExcessVerticalSpace)
      gridData.widthHint = widthHint
      gridData.heightHint = heightHint
      gridData.horizontalSpan = horizontalSpan
      gridData.horizontalIndent = horizontalIndent
      gridData
    }

  def createSyntaxPage(parent: Composite): Control = {
    overlayStore.load()
    overlayStore.start()

    val outerComposite = new Composite(parent, SWT.NONE)
    outerComposite.setLayout(gridLayout(marginHeight = 0, marginWidth = 0))

    val link = new Link(outerComposite, SWT.NONE)
    link.setText(PreferencesMessages.JavaEditorColoringConfigurationBlock_link)
    link.addSelectionListener { e: SelectionEvent =>
      PreferencesUtil.createPreferenceDialogOn(parent.getShell, e.text, null, null)
    }
    link.setLayoutData(gridData(
      horizontalAlignment = SWT.FILL,
      verticalAlignment = SWT.BEGINNING,
      grabExcessHorizontalSpace = true,
      grabExcessVerticalSpace = false,
      widthHint = 150,
      horizontalSpan = 2))

    val filler = new Label(outerComposite, SWT.LEFT)
    filler.setLayoutData(gridData(
      horizontalAlignment = SWT.FILL,
      horizontalSpan = 1,
      heightHint = new PixelConverter(outerComposite).convertHeightInCharsToPixels(1) / 2))

    var elementLabel = new Label(outerComposite, SWT.LEFT)
    elementLabel.setText(PreferencesMessages.JavaEditorPreferencePage_coloring_element)
    elementLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))

    val elementEditorComposite = new Composite(outerComposite, SWT.NONE)
    elementEditorComposite.setLayout(gridLayout(marginHeight = 0, marginWidth = 0, numColumns = 2))
    elementEditorComposite.setLayoutData(gridData(
      horizontalAlignment = SWT.FILL, verticalAlignment = SWT.BEGINNING, grabExcessHorizontalSpace = true,
      grabExcessVerticalSpace = false))

    createTreeViewer(elementEditorComposite)

    val stylesComposite = new Composite(elementEditorComposite, SWT.NONE)
    stylesComposite.setLayout(gridLayout(marginHeight = 0, marginWidth = 0, numColumns = 2))
    stylesComposite.setLayoutData(new GridData(GridData.FILL_BOTH))

    enabledCheckBox = new Button(stylesComposite, SWT.CHECK)
    enabledCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_enable)
    enabledCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 0, horizontalSpan = 2))

    colorEditorLabel = new Label(stylesComposite, SWT.LEFT)
    colorEditorLabel.setText(PreferencesMessages.JavaEditorPreferencePage_color)

    colorEditorLabel.setLayoutData(gridData(horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20))

    syntaxForegroundColorEditor = new ColorSelector(stylesComposite)
    val foregroundColorButton = syntaxForegroundColorEditor.getButton
    foregroundColorButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING))

    boldCheckBox = new Button(stylesComposite, SWT.CHECK)
    boldCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_bold)

    boldCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    italicCheckBox = new Button(stylesComposite, SWT.CHECK)
    italicCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_italic)
    italicCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    strikethroughCheckBox = new Button(stylesComposite, SWT.CHECK)
    strikethroughCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_strikethrough)
    strikethroughCheckBox.setLayoutData(gridData(
      horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    underlineCheckBox = new Button(stylesComposite, SWT.CHECK)
    underlineCheckBox.setText(PreferencesMessages.JavaEditorPreferencePage_underline)
    underlineCheckBox.setLayoutData(
      gridData(horizontalAlignment = GridData.BEGINNING, horizontalIndent = 20, horizontalSpan = 2))

    val previewLabel = new Label(outerComposite, SWT.LEFT)
    previewLabel.setText(PreferencesMessages.JavaEditorPreferencePage_preview)
    previewLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))

    previewer = createPreviewer(outerComposite)
    val previewerControl = previewer.getControl
    previewerControl.setLayoutData(gridData(
      horizontalAlignment = GridData.FILL,
      verticalAlignment = GridData.FILL,
      grabExcessHorizontalSpace = true,
      grabExcessVerticalSpace = true,
      widthHint = convertWidthInCharsToPixels(20),
      heightHint = convertHeightInCharsToPixels(5)))
    updatePreviewerColours()
    overlayStore.addPropertyChangeListener { event: PropertyChangeEvent =>
      updatePreviewerColours()
    }

    enabledCheckBox.addSelectionListener {
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.enabledKey, enabledCheckBox.getSelection)
    }
    foregroundColorButton.addSelectionListener {
      for (syntaxClass <- selectedSyntaxClass)
        PreferenceConverter.setValue(overlayStore, syntaxClass.colourKey, syntaxForegroundColorEditor.getColorValue)
    }
    boldCheckBox.addSelectionListener {
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.boldKey, boldCheckBox.getSelection)
    }
    italicCheckBox.addSelectionListener {
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.italicKey, italicCheckBox.getSelection)
    }
    underlineCheckBox.addSelectionListener {
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.underlineKey, underlineCheckBox.getSelection)
    }
    strikethroughCheckBox.addSelectionListener {
      for (syntaxClass <- selectedSyntaxClass)
        overlayStore.setValue(syntaxClass.strikethroughKey, strikethroughCheckBox.getSelection)
    }

    treeViewer.setSelection(new StructuredSelection(scalaSyntacticCategory))

    outerComposite.layout(false)
    outerComposite
  }

  private def createPreviewer(parent: Composite): SourceViewer =
    ScalaPreviewerFactory.createPreviewer(parent, overlayStore, previewText)

  private def selectedSyntaxClass: Option[ScalaSyntaxClass] = condOpt(treeViewer.getSelection) {
    case SelectedItems(syntaxClass: ScalaSyntaxClass) => syntaxClass
  }

  private def massSetEnablement(enabled: Boolean) =
    List(enabledCheckBox, syntaxForegroundColorEditor.getButton, colorEditorLabel, boldCheckBox, italicCheckBox,
      strikethroughCheckBox, underlineCheckBox) foreach { _.setEnabled(enabled) }

  private def handleSyntaxColorListSelection() = selectedSyntaxClass match {
    case None =>
      massSetEnablement(false)
    case Some(syntaxClass) =>
      val rgb = PreferenceConverter.getColor(overlayStore, syntaxClass.colourKey)
      syntaxForegroundColorEditor.setColorValue(rgb)
      enabledCheckBox.setSelection(overlayStore.getBoolean(syntaxClass.enabledKey))
      boldCheckBox.setSelection(overlayStore.getBoolean(syntaxClass.boldKey))
      italicCheckBox.setSelection(overlayStore.getBoolean(syntaxClass.italicKey))
      strikethroughCheckBox.setSelection(overlayStore.getBoolean(syntaxClass.strikethroughKey))
      underlineCheckBox.setSelection(overlayStore.getBoolean(syntaxClass.underlineKey))
      massSetEnablement(true)
      enabledCheckBox.setEnabled(syntaxClass.canBeDisabled)
  }

  private def updatePreviewerColours() {
    val textWidget = previewer.getTextWidget
    for (ColouringLocation(syntaxClass, offset, length) <- semanticLocations) {
      val styleRange = syntaxClass.getStyleRange(overlayStore)
      styleRange.start = offset
      styleRange.length = length
      textWidget.setStyleRange(styleRange)
    }
  }

}

object SyntaxColouringPreferencePage {

  private val previewText = """/** Scaladoc */
class ClassName extends SomeTrait {
  object Obj
  val n = 42
  var m = 24
  def method(param: String): Int = {
    // Single-line comment
    /* Multi-line comment */
    val s = "foo" + """ + "\"\"\"" + "multiline string" + "\"\"\"" + """
    var xml =
      <tag attributeName="value">
        <!-- XML comment -->
        <?processinginstruction?>
        <![CDATA[ CDATA ]]>
        PCDATA
      </tag>
    return 42
  }
}"""

  private case class ColouringLocation(syntaxClass: ScalaSyntaxClass, offset: Int, length: Int)

  private val semanticLocations: List[ColouringLocation] = {

    val identifierToSyntaxClass = Map(
      "ClassName" -> CLASS,
      "SomeTrait" -> TRAIT,
      "method" -> METHOD,
      "param" -> PARAM,
      "s" -> LOCAL_VAL,
      "xml" -> LOCAL_VAR,
      "n" -> TEMPLATE_VAL,
      "m" -> TEMPLATE_VAR,
      "Obj" -> OBJECT)

    val identifierLocations: Map[String, (Int, Int)] = {
      for (token <- ScalaLexer.rawTokenise(previewText, forgiveErrors = true) if token.tokenType.isId)
        yield (token.text, (token.startIndex, token.length))
    } toMap

    for {
      (identifier, syntaxClass) <- identifierToSyntaxClass.toList
      (offset, length) = identifierLocations(identifier)
    } yield ColouringLocation(syntaxClass, offset, length)
  }
}