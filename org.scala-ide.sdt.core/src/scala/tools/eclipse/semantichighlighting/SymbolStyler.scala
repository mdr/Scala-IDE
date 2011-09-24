package scala.tools.eclipse.semantichighlighting

import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source._
import scala.collection.JavaConversions._
import scala.collection.Set
import scala.tools.eclipse.semantichighlighting.classifier.SymbolInfo
import scala.tools.eclipse.javaelements.ScalaCompilationUnit
import scala.tools.eclipse.ScalaPlugin
import scala.tools.eclipse.properties.ScalaSyntaxClasses
import scala.tools.eclipse.semantichighlighting.classifier.SymbolClassifier

class SymbolStyler(sourceViewer: ISourceViewer) {

  private var annotations: Set[Annotation] = Set()

  private def addAnnotations(symbolInfos: List[SymbolInfo]) {
    val prefStore = ScalaPlugin.plugin.getPreferenceStore
    val strikethroughDeprecated = prefStore.getBoolean(ScalaSyntaxClasses.STRIKETHROUGH_DEPRECATED)

    val annotationsToPositions: Map[Annotation, Position] = {
      for {
        SymbolInfo(symbolType, regions, deprecated) <- symbolInfos
        region <- regions
        annotation = SymbolAnnotations.symbolAnnotation(symbolType, strikethroughDeprecated && deprecated)
        position = new Position(region.getOffset, region.getLength)
      } yield (annotation -> position)
    }.toMap

    for (annotationModel <- annotationModelOpt) {
      annotationModel.replaceAnnotations(annotations.toArray, annotationsToPositions)
      annotations = annotationsToPositions.keySet
    }
  }

  private def annotationModelOpt = Option(sourceViewer.getAnnotationModel.asInstanceOf[IAnnotationModelExtension])

  def updateSymbolAnnotations(scu: ScalaCompilationUnit) =
    scu.doWithSourceFile { (sourceFile, compiler) =>
      val prefStore = ScalaPlugin.plugin.getPreferenceStore
      val semanticHighlightingRequired = ScalaSyntaxClasses.scalaSemanticCategory.children.map(_.enabledKey).forall(prefStore.getBoolean)
      if (semanticHighlightingRequired) {
        val useScalariform = prefStore.getBoolean(ScalaSyntaxClasses.USE_ACCURATE_SEMANTIC_HIGHLIGHTING)
        val symbolInfos = new SymbolClassifier(sourceFile, compiler, useScalariform).classifySymbols
        addAnnotations(symbolInfos)
      } else if (annotations.nonEmpty)
        for (annotationModel <- annotationModelOpt) {
          annotationModel.replaceAnnotations(annotations.toArray, Map[Annotation, Position]())
          annotations = Set()
        }
    }

}