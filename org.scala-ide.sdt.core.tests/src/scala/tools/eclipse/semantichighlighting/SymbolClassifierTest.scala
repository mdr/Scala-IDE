package scala.tools.eclipse.semantichighlighting

import org.eclipse.jface.text.Region
import scala.tools.nsc.util.BatchSourceFile
import scala.tools.nsc.util.Position
import scala.tools.nsc.reporters.ConsoleReporter
import scala.tools.nsc.interactive.Global
import scala.tools.nsc.Settings
import org.junit.Assert._
import org.junit.runner.RunWith
import org.junit._
import java.io.File.pathSeparator
import SymbolTypes._

class SymbolClassifierTest {

  @Test
  def bug_with_static_import() {
    checkSymbolClassification("""
      import java.io.File.pathSeparator
      object A {
        val foobar = 42
      }""", """
      import java.io.File.pathSeparator
      object A {
        val $TVAL$ = 42
      }""",
      Map("TVAL" -> TemplateVal))
  }

  
  @Test
  def template_var_and_val() {
    checkSymbolClassification("""
      class A {
        var mutableVar = -127
        val immutableVal = 42
      }""", """
      class A {
        var $  TVAR  $ = -127
        val $   TVAL   $ = 42
      }""",
      Map("TVAR" -> TemplateVar, "TVAL" -> TemplateVal))
  }

  @Test
  def template_var_issue() {
    checkSymbolClassification("""
        class A {
          private var templateVar: String = _
          templateVar = "foo"
        }""", """
        class A {
          private var $  TVAR   $: String = _
          $   TVAR  $ = "foo"
        }""",
        Map("TVAR" -> TemplateVar, "TVAL" -> TemplateVal))
  }

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
      Map("TVAL" -> TemplateVal))
  }

  @Test
  def lazy_local_val() {
    checkSymbolClassification("""
        class A {
          {
            lazy val immutableVal = 42
            immutableVal
          }
        }""", """
        class A {
          {
            lazy val $   LVAL   $ = 42
            $   LVAL   $
          }
        }""",
      Map("LVAL" -> LocalVal))
  }

  @Test
  def backticked_identifiers() {
    checkSymbolClassification("""
      class A {
        val identifier = 42
        `identifier`
        identifier
      }""", """
      class A {
        val $  TVAL  $ = 42
        `$  TVAL  $`
        $  TVAL  $
      }""",
      Map("TVAL" -> TemplateVal))
  }

  @Test
  def backticked_this() {
    checkSymbolClassification("""
        class A {
          val `this` = 42
        }""", """
        class A {
          val `$TV$` = 42
        }""",
      Map("TV" -> TemplateVal))
  }

  @Test
  def implicit_apply_method_calls() {
    checkSymbolClassification("""
      class A {
        List(42)
        List.apply(42)
        List.`apply`(42)
      }""", """
      class A {
        List(42)
        List.$ M $(42)
        List.`$ M $`(42)
      }""",
      Map("M" -> Method))
  }

  @Test
  def vals_from_predef() {
    checkSymbolClassification("""
      class A {
        Set(42)
      }""", """
      class A {
        $S$(42)
      }""",
      Map("S" -> TemplateVal))
  }

  @Test
  def vals_from_objects() {
    checkSymbolClassification("""
      object X { val objectMember = 42 }
      class A { X.objectMember }""", """
      object X { val $    TVAL  $ = 42 }
      class A { X.$    TVAL  $ }""",
      Map("TVAL" -> TemplateVal))
  }

  @Test
  @Ignore
  def self_references_are_classified_as_template_vals() {
    checkSymbolClassification("""
      class A { myself =>
        myself
      }""", """
      class A { $ S  $ =>
        $  S $
      }""",
      Map("S" -> TemplateVal))
  }

  @Test // Replace with previous once fixed
  def self_references_are_classified_as_template_vals_2() {
    checkSymbolClassification("""
      class A { myself =>
      }""", """
      class A { $ S  $ =>
      }""",
      Map("S" -> TemplateVal))
  }

  @Test
  def extractor_unapply() {
    checkSymbolClassification("""
        class A { 
          object Unapplier { def unapply(param: String): Option[String] = None }
          "foo" match { case Unapplier(blahblah) => }
          Unapplier.unapply("foo")
        }""", """
        class A { 
          object Unapplier { def $METH $($PAR$: String): Option[String] = None }
          "foo" match { case Unapplier($ LVAL $) => }
          Unapplier.$METH $("foo")
        }""",
      Map("METH" -> Method, "PAR" -> MethodParam, "LVAL" -> LocalVal))
  }

  @Test
  def extractor_unapplySeq() {
    checkSymbolClassification("""
        class A { 
          object Unapplier { def unapplySeq(param: String): Option[List[String]] = None }
          "foo" match { case Unapplier(blahblah) => }
          Unapplier.unapplySeq("foo")
        }""", """
        class A { 
          object Unapplier { def $  METH  $($PAR$: String): Option[List[String]] = None }
          "foo" match { case Unapplier($ LVAL $) => }
          Unapplier.$  METH  $("foo")
        }""",
      Map("METH" -> Method, "PAR" -> MethodParam, "LVAL" -> LocalVal))
  }

  @Test
  def new_invocation() {
    checkSymbolClassification("""
        class A { 
          new StringBuilder
        }""", """
        class A { 
          new StringBuilder
        }""",
      Map("METH" -> Method, "PAR" -> MethodParam, "LVAL" -> LocalVal))
  }

  lazy val compiler = {
    val settings = new Settings
    val compiler = new Global(settings, new ConsoleReporter(settings))
    new compiler.Run
    compiler
  }

  object RegionParser {

    def getRegions(s: String): Map[Region, String] = {
      var regions: Map[Region, String] = Map()
      var inRegion = false
      var regionStart = -1
      val regionTextBuffer = new StringBuffer
      var pos = 0
      while (pos < s.length) {
        val c = s(pos)
        if (inRegion) {
          if (c == '$') {
            val regionText = regionTextBuffer.toString
            regions = regions + (new Region(regionStart, pos - regionStart + 1) -> regionText)
            inRegion = false
            regionStart = -1
            regionTextBuffer.setLength(0)
          } else
            regionTextBuffer.append(c)
        } else {
          if (c == '$') {
            inRegion = true
            regionStart = pos
          }
        }
        pos += 1
      }
      require(!inRegion)
      regions
    }

  }

  private def checkSymbolClassification(initial: String, locationTemplate: String, regionTagToSymbolType: Map[String, SymbolType]) {
    val regions = RegionParser.getRegions(locationTemplate)
    val expectedRegionSymbolInfos = (for ((region, s) <- regions) yield (region, regionTagToSymbolType(s.trim))).toSet
    val sourceFile = new BatchSourceFile("<parse>", initial)
    compiler.typedTree(sourceFile, true)
    val unit = new compiler.CompilationUnit(sourceFile)
    val symbolInfos: List[SymbolInfo] = new SymbolClassifier(unit.source, compiler).classifySymbols
    val actualRegionSymbolInfos = (for (symbolInfo <- symbolInfos; region <- symbolInfo.regions) yield (region, symbolInfo.symbolType)).toSet
    if (expectedRegionSymbolInfos != actualRegionSymbolInfos) {
      val sb = new StringBuffer
      sb.append("Actual != Expected.").append("\n")
      sb.append("Expected:").append("\n")
      expectedRegionSymbolInfos.toList.sortBy(_._1.getOffset).foreach { pair => sb.append(pair).append("\n") }
      sb.append("Actual:").append("\n")
      actualRegionSymbolInfos.toList.sortBy(_._1.getOffset).foreach { pair => sb.append(pair).append("\n") }
      throw new AssertionError(sb.toString)
    }
  }

}

