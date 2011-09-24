package scala.tools.eclipse.semantichighlighting.classifier

import scala.tools.eclipse.semantichighlighting.classifier.SymbolTypes._

trait SymbolTests { self: SymbolClassifier =>

  import global._

  def getSymbolType(sym: Symbol): SymbolType = {
    if (isLocalValVarOrParam(sym))
      if (sym.isVariable)
        LocalVar
      else if (forValSymbols contains sym) // variables in for generators get mis-identified as params
        LocalVal
      else if (sym.isParameter)
        Param
      else if (sym.isLazy)
        LazyLocalVal
      else
        LocalVal
    else if (isMethod(sym))
      Method
    else if (isCaseObject(sym))
      CaseObject
    else if (isObject(sym))
      Object
    else if (isCaseClass(sym))
      CaseClass
    else if (isClass(sym))
      Class
    else if (isPackage(sym))
      Package
    else if (isTrait(sym))
      Trait
    else if (isTypeParam(sym))
      TypeParameter
    else if (isType(sym))
      Type
    else if (isTemplateVal(sym))
      if (sym.isLazy)
        LazyTemplateVal
      else
        TemplateVal
    //      else if (sym.isType) {
    //        val tpe = sym.tpe
    //        val typSym = tpe.normalize.typeSymbol
    //        if (isClass(sym))
    //          Class
    //        else
    //          Trait
    //      } 
    else
      TemplateVar
  }

  private def isLocalValVarOrParam(s: Symbol) =
    s != NoSymbol && s.isLocal && s.isValue && !s.isModule && !s.isSourceMethod

  private def isClass(s: Symbol) = s != NoSymbol && !s.isCaseClass && (
    s.isClass && !s.isTrait ||
    s.isJavaDefined && s.isModule && !s.isPackage)

  private def isCaseClass(s: Symbol) = s != NoSymbol && (
    s.isCaseClass ||
    s.isModule && s.companionClass.isCaseClass /* to cover constructor calls */ )

  private def isTrait(s: Symbol) = s != NoSymbol && s.isClass && s.isTrait

  private def isCaseObject(s: Symbol) = s != NoSymbol && s.isModule && s.isValue && s.isCase

  private def isObject(s: Symbol) = s != NoSymbol && s.isModule && s.isValue && !s.companionClass.isCaseClass && !s.isCase

  private def isTypeParam(s: Symbol) = s != NoSymbol && s.isType && s.isParameter // s.isTypeParam excludes skolems 

  private def isType(s: Symbol) = s != NoSymbol && s.isType && !isClass(s) && !isTrait(s) && !isTypeParam(s)

  private def isMethod(s: Symbol) = s != NoSymbol && s.isSourceMethod && !s.isLazy && !s.isType

  private def isPackage(s: Symbol) = s != NoSymbol && s.isPackage

  private def isTemplateVal(s: Symbol) = s != NoSymbol && s.isValue && !s.isVariable && !s.isLocal && !s.isModule && !s.isSourceMethod

  private def isTemplateVar(s: Symbol) = s != NoSymbol && s.isVariable && !s.isLocal && !s.isModule && !s.isSourceMethod

}