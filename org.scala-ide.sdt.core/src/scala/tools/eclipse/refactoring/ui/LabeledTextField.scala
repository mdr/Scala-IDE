package scala.tools.eclipse.refactoring

import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Text
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Label

package ui {
    
  class LabeledTextField(parent: Composite, obs: String => Unit, labelName: String, textContent: String) extends Composite(parent, SWT.None) {
  
    val label = new Label(this, SWT.NONE)
    label.setText(labelName)
    label.setLayoutData(new GridData)
      
    val textField = new Text(this, SWT.BORDER | SWT.SINGLE)
    textField.setText(textContent)
    textField.selectAll()
    
    private val gridLayout = new GridLayout
    gridLayout.numColumns = 2
    setLayout(gridLayout)
  
    private val textData = new GridData(GridData.FILL_HORIZONTAL)
    textData.grabExcessHorizontalSpace = true
    textField.setLayoutData(textData)
    
    override def setEnabled(enabled: Boolean) {
      label.setEnabled(enabled)
      textField.setEnabled(enabled)
    }
    
    obs(textField.getText)
        
    textField.addModifyListener(new ModifyListener {
      def modifyText(e: ModifyEvent) {
         obs(textField.getText)
      }
    })
  }
}