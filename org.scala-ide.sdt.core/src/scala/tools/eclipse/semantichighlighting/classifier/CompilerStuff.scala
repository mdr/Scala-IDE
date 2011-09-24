package scala.tools.eclipse.semantichighlighting.classifier

import scala.tools.nsc.util.SourceFile
import scala.tools.refactoring.util.TreeCreationMethods

trait CompilerStuff extends TreeCreationMethods { self: SymbolClassifier =>

  import global._

  lazy val tree = treeFrom(sourceFile, forceReload = true)

  lazy val index: GlobalIndex = GlobalIndex(tree).asInstanceOf[GlobalIndex]

  // Scala-refactoring's symbol expansion is handly for vars, but not for everything (e.g. "val s: Set[Int] = Set()")
  def occurrences(sym: global.Symbol) =
    if (sym.isModule)
      index.occurences(sym)
    else if (sym.isVariable && !sym.isMethod)
      index.occurences(sym)
    else
      index.declaration(sym).toList ::: index.cus.flatMap { cu =>
        cu.references.get(sym).toList.flatten
      } filter (_.pos.isRange) distinct

}