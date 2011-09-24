package scala.tools.eclipse.semantichighlighting.classifier

import SymbolTypes._
import org.junit._

class LazyTemplateValTest extends AbstractSymbolClassifierTest {

  @Test
  def lazy_template_val() {
    checkSymbolClassification("""
        class A {
          lazy val immutableVal = 42
          immutableVal
        }""", """
        class A {
          lazy val $   TVAL   $ = 42
          $   TVAL   $
        }""",
      Map("TVAL" -> LazyTemplateVal))
  }

}