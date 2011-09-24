package scala.tools.eclipse.semantichighlighting.classifier

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
import scala.tools.nsc.interactive.Response
import java.io.File

class AbstractSymbolClassifierTest {
  private lazy val compiler = {
    val settings = new Settings
    val scalaVersion = "2.9.1"
    //    settings.classpath.tryToSet(List(
    //      "project/boot/scala-" + scalaVersion + "/lib/scala-compiler.jar" +
    //        ":project/boot/scala-" + scalaVersion + "/lib/scala-library.jar"))
    //    val compiler = new Global(settings, new ConsoleReporter(settings))
    //    new compiler.Run
    //    compiler

    val scalaObjectSource = java.lang.Class.forName("scala.ScalaObject").getProtectionDomain.getCodeSource

    // is null in Eclipse/OSGI but luckily we don't need it there
    if (scalaObjectSource != null) {
      val compilerPath = java.lang.Class.forName("scala.tools.nsc.Interpreter").getProtectionDomain.getCodeSource.getLocation
      val libPath = scalaObjectSource.getLocation
      val pathList = List(compilerPath, libPath)
      val origBootclasspath = settings.bootclasspath.value
      settings.bootclasspath.value = ((origBootclasspath :: pathList) ::: Nil) mkString File.pathSeparator
    }

    val compiler = new Global(settings, new ConsoleReporter(settings) {
      override def printMessage(pos: Position, msg: String) {
        //throw new Exception(pos.source.file.name + pos.show + msg)
      }
    })

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

  protected def checkSymbolClassification(initial: String, locationTemplate: String, regionTagToSymbolType: Map[String, SymbolType]) {
    val expectedRegionMap: Map[Region, String] = RegionParser.getRegions(locationTemplate)
    val expectedRegionSymbolInfos = expectedRegionMap mapValues { s => regionTagToSymbolType(s.trim) } toSet

    val sourceFile = new BatchSourceFile("", initial)
    val symbolInfos: List[SymbolInfo] = new SymbolClassifier(sourceFile, compiler, useScalariform = true).classifySymbols

    val actualRegionSymbolInfos =
      for {
        SymbolInfo(symbolType, regions, deprecated) <- symbolInfos.toSet
        region <- regions
        if expectedRegionMap.keySet.exists(_ intersects region)
      } yield (region, symbolType)

    if (expectedRegionSymbolInfos != actualRegionSymbolInfos) {
      val sb = new StringBuffer
      sb.append("Actual != Expected.").append("\n")
      sb.append("Expected:").append("\n")
      expectedRegionSymbolInfos.toList.sortBy(_._1.getOffset).foreach {
        case (region @ Region(offset, length), symbolType) =>
          sb.append(region).append(" '" + initial.substring(offset, offset + length) + "' ").append(symbolType).append("\n")
      }
      sb.append("Actual:").append("\n")
      actualRegionSymbolInfos.toList.sortBy(_._1.getOffset).foreach {
        case (region @ Region(offset, length), symbolType) =>
          sb.append(region).append(" '" + initial.substring(offset, offset + length) + "' ").append(symbolType).append("\n")
      }
      throw new AssertionError(sb.toString)
    }
  }

}