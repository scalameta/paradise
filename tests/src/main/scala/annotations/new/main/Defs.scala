package main

import scala.annotation.compileTimeOnly
import scala.meta.Term.Block
import scala.meta._

@compileTimeOnly("@printDef not expanded")
class printDef extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any) = meta {
    assert(defn.is[Defn.Def])
    q"println(${defn.toString})"
  }
}

@compileTimeOnly("@printVal not expanded")
class printVal extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any) = meta {
    assert(defn.is[Defn.Val])
    q"println(${defn.toString})"
  }
}

@compileTimeOnly("@printClass not expanded")
class printClass extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any) = meta {
    assert(defn.is[Defn.Class])
    q"println(${defn.toString})"
  }
}


@compileTimeOnly("@identity not expanded")
class identity extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Defn) = meta {
    defn
  }
}

@compileTimeOnly("@populateDef not expanded")
class helloWorld extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Defn.Def) = meta {
    val q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" = defn
    q"""..$mods def $name[..$tparams](...$paramss): $tpeopt = "hello world""""
  }
}

@compileTimeOnly("@appendA not expanded")
class appendA extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Defn.Def) = meta {
    val q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" = defn
    val stat = q"letters += 'a'"
    val newExpr = expr match {
      case b:Block => b.copy(stats = b.stats :+ stat)
      case t:Term => val stats = Vector(stat, t)
        q"{ ..$stats }"
    }
    q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $newExpr"
  }
}

@compileTimeOnly("@appendB not expanded")
class appendB extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Defn.Def) = meta {
    val q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" = defn
    val stat = q"letters += 'b'"
    val newExpr = expr match {
      case b:Block => b.copy(stats = b.stats :+ stat)
      case t:Term => val stats = Vector(stat, t)
        q"{ ..$stats }"
    }
    q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $newExpr"
  }
}

@compileTimeOnly("@appendC not expanded")
class appendC extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Defn.Def) = meta {
    val q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" = defn
    val stat = q"letters += 'c'"
    val newExpr = expr match {
      case b:Block => b.copy(stats = b.stats :+ stat)
      case t:Term => val stats = Vector(stat, t)
        q"{ ..$stats }"
    }
    q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $newExpr"
  }
}

package placebo {
  class appendA extends scala.annotation.StaticAnnotation
}
