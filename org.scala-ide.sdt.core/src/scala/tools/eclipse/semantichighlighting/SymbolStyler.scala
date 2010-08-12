package scala.tools.eclipse.semantichighlighting

import org.eclipse.jface.text.Position
import org.eclipse.jface.text.source._
import scala.collection.JavaConversions._
import scala.collection.Set

class SymbolStyler {

  private var annotations: Set[Annotation] = Set()

  def updateSymbolAnnotations(symbolInfos: List[SymbolInfo], sourceViewer: ISourceViewer) {
    
    val annotationPositionPairs = for {
      SymbolInfo(symbolType, regions) <- symbolInfos
      region <- regions
      annotationInfo <- SymbolAnnotations.allSymbolAnnotations.get(symbolType)
      annotationType = annotationInfo.annotationId
      annotation = new Annotation(annotationType, false, null)
      position = new Position(region.getOffset, region.getLength)
    } yield (annotation -> position)
    val annotationsToPositions: Map[Annotation, Position] = annotationPositionPairs.toMap

    val annotationModel = sourceViewer.getAnnotationModel.asInstanceOf[IAnnotationModelExtension]
    annotationModel.replaceAnnotations(annotations.toArray, annotationsToPositions)
    annotations = annotationsToPositions.keySet
  }
}