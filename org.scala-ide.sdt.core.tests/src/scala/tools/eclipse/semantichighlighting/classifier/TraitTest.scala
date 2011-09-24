package scala.tools.eclipse.semantichighlighting.classifier

import SymbolTypes._
import org.junit._

class TraitTest extends AbstractSymbolClassifierTest {

  @Test
  def basic_trait() {
    checkSymbolClassification("""
      trait Trait
      trait `Trait2` {
         new Trait {}
         new `Trait2` {}
      }""", """
      trait $TRT$
      trait $ TRT  $ {
         new $TRT$ {}
         new $ TRT  $ {}
      }""",
      Map("TRT" -> Trait))
  }

}