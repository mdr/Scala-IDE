package scala.tools.eclipse.semantichighlighting.classifier

import SymbolTypes._
import org.junit._

class PackageTest extends AbstractSymbolClassifierTest {

  @Test
  def package_decl() {
    checkSymbolClassification("""
      package packageName1.packageName2
      """, """
      package $  PACKAGE $.$  PACKAGE $""",
      Map("PACKAGE" -> Package))
  }

}