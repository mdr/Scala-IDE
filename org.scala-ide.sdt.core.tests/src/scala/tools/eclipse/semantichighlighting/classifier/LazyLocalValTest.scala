package scala.tools.eclipse.semantichighlighting.classifier

import SymbolTypes._
import org.junit._

class LazyLocalValTest extends AbstractSymbolClassifierTest {

  @Test
  def decl_and_ref_of_lazy_vals() {
    checkSymbolClassification("""
      object A {
        {
           lazy val xxxxxx = 100
           xxxxxx * xxxxxx
           lazy val `xxxx` = 100
           `xxxx` * `xxxx`
        }
      }""", """
      object A {
        {
           lazy val $LVAL$ = 100
           $LVAL$ * $LVAL$
           lazy val $LVAL$ = 100
           $LVAL$ * $LVAL$
        }
      }""",
      Map("LVAL" -> LazyLocalVal))
  }

}