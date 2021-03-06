/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse.javaelements

import org.eclipse.jdt.core.search.{ SearchMatch, SearchParticipant, TypeDeclarationMatch, TypeReferenceMatch, MethodReferenceMatch, FieldReferenceMatch, SearchPattern }

import org.eclipse.jdt.internal.compiler.ast.{ SingleTypeReference, TypeDeclaration }
import org.eclipse.jdt.internal.core.search.matching.{ MatchLocator, PossibleMatch, OrPattern }

import scala.tools.nsc.util.{ RangePosition, Position }

import scala.tools.eclipse.ScalaPresentationCompiler
import scala.tools.eclipse.util.ReflectionUtils
import org.eclipse.jdt.internal.core.search.matching.{ FieldPattern, MethodPattern, TypeReferencePattern };

//FIXME should report all and let matcher to the selection OR only report matcher interest (pre select by type) OR ...

trait ScalaMatchLocator { self: ScalaPresentationCompiler =>
  class MatchLocatorTraverser(scu: ScalaCompilationUnit, matchLocator: MatchLocator, possibleMatch: PossibleMatch) extends Traverser {
    import MatchLocatorUtils._

    private val patterns = {
      matchLocator.pattern match {
          case orp : OrPattern => MatchLocatorUtils.patterns(orp)
          case singlePattern => Array(singlePattern)
      }
    }

    override def traverse(tree: Tree): Unit = {
      if (tree.pos.isOpaqueRange) {
        tree match {
          case t : TypeTree => {
            if (t.pos.isDefined)
              reportTypeReference(t.tpe, t.pos)
            if (t.tpe.isInstanceOf[TypeRef])
              t.tpe.asInstanceOf[TypeRef].args.foreach(a => reportTypeReference(a, t.pos))
            if (t.tpe.isInstanceOf[TypeBounds]) {
              reportTypeReference(t.tpe.asInstanceOf[TypeBounds].hi, t.pos)
              reportTypeReference(t.tpe.asInstanceOf[TypeBounds].lo, t.pos)
            }
          }
          case v : ValOrDefDef if (v.tpt.pos.isDefined && !v.tpt.pos.isRange) =>
            reportTypeReference(v.tpt.asInstanceOf[TypeTree].tpe,
              new RangePosition(v.tpt.pos.source,
                v.tpt.pos.point,
                v.tpt.pos.point,
                v.tpt.pos.point + v.name.length))
          case id : Ident if (!id.symbol.toString.startsWith("package")) =>	patterns.foreach {
            case pattern : TypeReferencePattern => reportObjectReference(pattern, id.symbol, id.pos)
            case _ => ()
          }
          case im : Import if (!im.expr.symbol.toString.startsWith("package")) => patterns.foreach {
            case pattern : TypeReferencePattern => reportObjectReference(pattern, im.expr.symbol, im.pos)
            case _ => ()
          }
          case s : Select => patterns.foreach { pattern =>
            (s.symbol, pattern) match {
              case (symbol : ModuleSymbol, pattern : TypeReferencePattern) => reportObjectReference(pattern, symbol, s.pos)
              case (symbol : MethodSymbol, pattern : MethodPattern)        => reportValueOrMethodReference(s, pattern)
              case (symbol : MethodSymbol, pattern : FieldPattern)         => reportVariableReference(s, pattern)
              case _ => ()
            }
          }
          case n: New =>
            reportTypeReference(n.tpe, n.tpt.pos)
          case c: ClassDef if c.pos.isDefined =>
            reportTypeDefinition(c.symbol.tpe, c.pos)
          case _ => ()
        }

        if (tree.symbol != null)
          reportAnnotations(tree.symbol)

        super.traverse(tree)
      }
    }

    def reportAnnotations(sym: Symbol) {
      for (annot <- sym.annotations if annot.pos.isDefined) {
        reportTypeReference(annot.atp, annot.pos)
        traverseTrees(annot.args)
      }
    }

    def reportTypeDefinition(tpe: Type, declPos: Position) {
      val decl = new TypeDeclaration(null)
      decl.name = tpe.typeSymbol.nameString.toArray
      if (matchLocator.patternLocator.`match`(decl, possibleMatch.nodeSet) > 0) {
        val element = scu match {
          case ssf: ScalaSourceFile => ssf.getElementAt(declPos.start)
          case _ => null
        }
        //since we consider only the class name (and not its fully qualified name),
        //the search is inaccurate
        val accuracy = SearchMatch.A_INACCURATE
        val offset = declPos.start
        val length = declPos.end - offset
        val participant = possibleMatch.document.getParticipant
        val resource = possibleMatch.resource
        val sm = new TypeDeclarationMatch(element, accuracy, offset, length, participant, resource)

        report(matchLocator, sm)
      }
    }

    def reportTypeReference(tpe: Type, refPos: Position) {
      if (tpe == null) return;
      val ref = new SingleTypeReference(tpe.typeSymbol.nameString.toArray, posToLong(refPos));
      if (matchLocator.patternLocator.`match`(ref, possibleMatch.nodeSet) > 0) {
        val enclosingElement = scu match {
          case ssf: ScalaSourceFile => ssf.getElementAt(refPos.start)
          case _ => null
        }
        //since we consider only the class name (and not its fully qualified name),
        //the search is inaccurate
        // Matt: JUnit search results require ACCURATE matches to locate its annotations
        val accuracy = SearchMatch.A_ACCURATE
        val offset = refPos.start
        val length = refPos.end - offset
        val insideDocComment = false
        val participant = possibleMatch.document.getParticipant
        val resource = possibleMatch.resource
        val sm = new TypeReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource)

        report(matchLocator, sm)
      }
    }

    def reportObjectReference(pattern : TypeReferencePattern, symbol : Symbol, pos : Position) {
        val searchedObjectName = new String(simpleName(pattern))
        val currentObjectName = symbol.name.toString
        if (searchedObjectName.equals(currentObjectName) || searchedObjectName.equals(currentObjectName + "$")) {
        	val enclosingElement = scu match {
              case ssf: ScalaSourceFile => ssf.getElementAt(pos.start)
              case _ => null
            }
	        //since we consider only the object name (and not its fully qualified name),
	        //the search is inaccurate
	        val accuracy = SearchMatch.A_INACCURATE
	        val offset = pos.start
	        val length = pos.end - offset
	        val insideDocComment = false
	        val participant = possibleMatch.document.getParticipant
	        val resource = possibleMatch.resource
	        val sm = new TypeReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource)

	        report(matchLocator, sm)
        }
    }

    def reportValueOrMethodReference(s : Select, methodPattern : MethodPattern) {
    	if (!s.name.toString.equals(new String(methodPattern.selector))) return;

    	def checkSignature(methodType : MethodType) : Boolean = {
    	  if (methodPattern.parameterCount != methodType.paramTypes.size)
    	    return false;
    	  val searchedParamTypes = methodPattern.parameterSimpleNames.map(sp => new String(sp));
    	  val currentParamTypes = methodType.paramTypes;

    	  for (i <- 0 to currentParamTypes.size - 1)
    		if (!currentParamTypes(i).baseClasses.exists(bc => bc.name.toString.equals(searchedParamTypes(i))))
    		  return false;
    	  true;
    	}

    	if (checkCallingObjectType(s, new String(methodPattern.declaringSimpleName)))
    	  if ((!s.tpe.isInstanceOf[MethodType] && methodPattern.parameterCount == 0) ||
    	      (s.tpe.isInstanceOf[MethodType] && checkSignature(s.tpe.asInstanceOf[MethodType]))) {
    		  val enclosingElement = scu match {
    			case ssf: ScalaSourceFile => ssf.getElementAt(s.pos.start)
    			case _ => null
    		  }
    		  val accuracy = SearchMatch.A_INACCURATE
    		  val offset = s.pos.start
    		  val length = s.pos.end - offset
    		  val insideDocComment = false
    		  val participant = possibleMatch.document.getParticipant
    		  val resource = possibleMatch.resource

    		  val sm = new MethodReferenceMatch(enclosingElement, accuracy, offset, length, insideDocComment, participant, resource)

    		  report(matchLocator, sm)
    	}
    }

    def reportVariableReference(s : Select, fieldPattern : FieldPattern) {
        val nameOfTheSearchedVar = new String(fieldPattern.getIndexKey)

        if ((!s.name.toString.equals(nameOfTheSearchedVar) &&
        	 !s.name.toString.equals(nameOfTheSearchedVar + "_$eq")) ||
        	!checkCallingObjectType(s, new String(declaringSimpleName(fieldPattern))))
        	return;

        val enclosingElement = scu match {
    	  case ssf: ScalaSourceFile => ssf.getElementAt(s.pos.start)
    	  case _ => null
        }
    	val accuracy = SearchMatch.A_INACCURATE
    	val offset = s.pos.start
    	val length = s.pos.end - offset
    	val insideDocComment = false
    	val participant = possibleMatch.document.getParticipant
    	val resource = possibleMatch.resource

    	val sm = new FieldReferenceMatch(enclosingElement, accuracy, offset, length, true, false, insideDocComment, participant, resource)

    	report(matchLocator, sm)
    }

	def checkCallingObjectType(s : Select, classNameContainingTheSearchedVal : String) = {
      val containerClass = s.qualifier.tpe.asInstanceOf[Type];
      val classNames = containerClass.baseClasses.map(bc => bc.name.toString);
      classNames.exists(n => n.equals(classNameContainingTheSearchedVal));
    }


    def posToLong(pos: Position): Long = pos.startOrPoint << 32 | pos.endOrPoint
  }
}

object MatchLocatorUtils extends ReflectionUtils {
  val mlClazz = classOf[MatchLocator]
  val reportMethod = getDeclaredMethod(mlClazz, "report", classOf[SearchMatch])
  def report(ml: MatchLocator, sm: SearchMatch) = reportMethod.invoke(ml, sm)

  val fpClazz = classOf[FieldPattern]
  val declaringSimpleNameField = getDeclaredField(fpClazz, "declaringSimpleName")
  def declaringSimpleName(fp : FieldPattern) = declaringSimpleNameField.get(fp).asInstanceOf[Array[Char]];

  val ftrClazz = classOf[TypeReferencePattern]
  val simpleNameField = getDeclaredField(ftrClazz, "simpleName")
  def simpleName(trp : TypeReferencePattern) = simpleNameField.get(trp).asInstanceOf[Array[Char]];

  val orpClazz = classOf[OrPattern]
  val patternsField = getDeclaredField(orpClazz, "patterns")
  def patterns(orp : OrPattern) = patternsField.get(orp).asInstanceOf[Array[SearchPattern]]
}
