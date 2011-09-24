package scala.tools.eclipse.semantichighlighting.classifier

import SymbolTypes._
import org.junit._

class CaseClassTest extends AbstractSymbolClassifierTest {

  @Test
  def case_class() {
    checkSymbolClassification("""
      case class CaseClass {
        def method(other: CaseClass) = 42
      }""", """
      case class $CASECLS$ {
        def method(other: $CASECLS$) = 42
      }""",
      Map("CASECLS" -> CaseClass))
  }

  @Test
  def case_class_creation() {
    checkSymbolClassification("""
      case class CaseClass(n: Int) {
        CaseClass(42)
      }""", """
      case class $CASECLS$(n: Int) {
        $CASECLS$(42)
      }""",
      Map("CASECLS" -> CaseClass))
  }

  @Test
  @Ignore
  def case_class_in_import() {
    checkSymbolClassification("""
      package foo { case class CaseClass }
      import foo.CaseClass
      """, """
      package foo { case class $CASECLS$ }
      import foo.$CASECLS$
      """,
      Map("CASECLS" -> CaseClass))
  }

}