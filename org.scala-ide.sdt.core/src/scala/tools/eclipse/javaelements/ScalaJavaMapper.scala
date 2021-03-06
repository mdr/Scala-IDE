/*
 * Copyright 2005-2010 LAMP/EPFL
 */
// $Id$

package scala.tools.eclipse.javaelements

import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants

import scala.tools.nsc.symtab.Flags
import scala.tools.eclipse.ScalaPresentationCompiler
import ch.epfl.lamp.fjbg.{ JObjectType, JType }

trait ScalaJavaMapper { self : ScalaPresentationCompiler => 

  def mapModifiers(mods : Modifiers) : Int = {
    var jdtMods = 0
    if(mods.isPrivate)
      jdtMods = jdtMods | ClassFileConstants.AccPrivate
    else if(mods.isProtected)
      jdtMods = jdtMods | ClassFileConstants.AccProtected
    else
      jdtMods = jdtMods | ClassFileConstants.AccPublic
    
    if(mods.isAbstract || mods.isDeferred)
      jdtMods = jdtMods | ClassFileConstants.AccAbstract

    if(mods.isFinal || mods.hasFlag(Flags.MODULE))
      jdtMods = jdtMods | ClassFileConstants.AccFinal
    
    if(mods.isTrait)
      jdtMods = jdtMods | ClassFileConstants.AccInterface
    
    jdtMods
  }
        
  def mapType(t : Tree) : String = {
    (t match {
      case tt : TypeTree => {
        if(tt.symbol == null || tt.symbol == NoSymbol || tt.symbol.isRefinementClass || tt.symbol.owner.isRefinementClass)
          "scala.AnyRef"
        else
          tt.symbol.fullName
      }
      case Ident(name) => name.toString
      case _ => "scala.AnyRef"
    }) match {
      case "scala.AnyRef" => "java.lang.Object"
      case "scala.Unit" => "void"
      case "scala.Boolean" => "boolean"
      case "scala.Byte" => "byte"
      case "scala.Short" => "short"
      case "scala.Int" => "int"
      case "scala.Long" => "long"
      case "scala.Float" => "float"
      case "scala.Double" => "double"
      case "<NoSymbol>" => "void"
      case n => n
    }
  }

  def mapModifiers(sym : Symbol) : Int = {
    var jdtMods = 0
    if(sym.hasFlag(Flags.PRIVATE))
      jdtMods = jdtMods | ClassFileConstants.AccPrivate
    else if(sym.hasFlag(Flags.PROTECTED))
      jdtMods = jdtMods | ClassFileConstants.AccProtected
    else
      jdtMods = jdtMods | ClassFileConstants.AccPublic
    
    if(sym.hasFlag(Flags.ABSTRACT) || sym.hasFlag(Flags.DEFERRED))
      jdtMods = jdtMods | ClassFileConstants.AccAbstract

    if(sym.isFinal || sym.hasFlag(Flags.MODULE))
      jdtMods = jdtMods | ClassFileConstants.AccFinal
    
    if(sym.isTrait)
      jdtMods = jdtMods | ClassFileConstants.AccInterface
    
    jdtMods
  }

  def mapType(s : Symbol) : String = {
    (if(s == null || s == NoSymbol || s.isRefinementClass || s.owner.isRefinementClass)
        "scala.AnyRef"
      else
        s.fullName
    ) match {
      case "scala.AnyRef" => "java.lang.Object"
      case "scala.Unit" => "void"
      case "scala.Boolean" => "boolean"
      case "scala.Byte" => "byte"
      case "scala.Short" => "short"
      case "scala.Int" => "int"
      case "scala.Long" => "long"
      case "scala.Float" => "float"
      case "scala.Double" => "double"
      case "<NoSymbol>" => "void"
      case n => n
    }
  }
  
  def mapParamTypePackageName(t : Type) : String = {
    if (t.typeSymbolDirect.isTypeParameter)
      ""
    else {
      val jt = javaType(t)
      if (jt.isValueType)
        ""
      else
        t.typeSymbol.enclosingPackage.fullName
    }
  }
  
  def mapParamTypeName(t : Type) : String = {
    if (t.typeSymbolDirect.isTypeParameter)
      t.typeSymbolDirect.name.toString
    else {
      val jt = javaType(t)
      if (jt.isValueType)
        jt.toString
      else
        mapTypeName(t.typeSymbol)
    }
  }
  
  def mapParamTypeSignature(t : Type) : String = {
    if (t.typeSymbolDirect.isTypeParameter)
      "T"+t.typeSymbolDirect.name.toString+";"
    else {
      val jt = javaType(t)
      val fjt = if (jt == JType.UNKNOWN)
        JObjectType.JAVA_LANG_OBJECT
      else
        jt
      fjt.getSignature.replace('/', '.')
    }
  }
  
  def mapTypeName(s : Symbol) : String =
    if (s == NoSymbol || s.hasFlag(Flags.PACKAGE)) ""
    else {
      val owner = s.owner
      val prefix = if (owner != NoSymbol && !owner.hasFlag(Flags.PACKAGE)) mapTypeName(s.owner)+"." else ""
      val suffix = if (s.hasFlag(Flags.MODULE) && !s.hasFlag(Flags.JAVA)) "$" else ""
      prefix+s.nameString+suffix
    }

  def enclosingTypeNames(sym : Symbol): List[String] = {
    def enclosing(sym : Symbol) : List[String] =
      if (sym == NoSymbol || sym.owner.hasFlag(Flags.PACKAGE))
        Nil
      else {
        val owner = sym.owner 
        val name0 = owner.simpleName.toString
        val name = if (owner.isModuleClass) name0+"$" else name0
        name :: enclosing(owner)
      }
        
    enclosing(sym).reverse
  }
}
