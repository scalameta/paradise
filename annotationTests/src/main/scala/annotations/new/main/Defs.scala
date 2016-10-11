package main

import scala.annotation.compileTimeOnly
import scala.collection.immutable
import scala.meta._

@compileTimeOnly("@printDef not expanded")
class printDef extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    assert(defn.is[Defn.Def])
    q"println(${defn.toString})"
  }
}

@compileTimeOnly("@printVal not expanded")
class printVal extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    assert(defn.is[Defn.Val])
    q"println(${defn.toString})"
  }
}

@compileTimeOnly("@printClass not expanded")
class printClass extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    assert(defn.is[Defn.Class])
    q"println(${defn.toString})"
  }
}

@compileTimeOnly("@identity not expanded")
class identity extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn
  }
}

@compileTimeOnly("@populateDef not expanded")
class helloWorld extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" = defn
    q"""..$mods def $name[..$tparams](...$paramss): $tpeopt = "hello world""""
  }
}

@compileTimeOnly("@appendA not expanded")
class appendA extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    Helpers.appendStat(defn, "letters += 'a'")
  }
}

@compileTimeOnly("@appendB not expanded")
class appendB extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    Helpers.appendStat(defn, "letters += 'b'")

  }
}

@compileTimeOnly("@appendC not expanded")
class appendC extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    Helpers.appendStat(defn, "letters += 'c'")
  }
}

object Helpers {
  def appendStat(defn: Tree, stat: String) = {
    val parsedStat = stat.parse[Stat].get

    defn match {
      case q"""..$mods def $name[..$tparams](...$paramss): $tpeopt = {
                 ..${ stats: immutable.Seq[Stat] }
               }""" =>
        q"..$mods def $name[..$tparams](...$paramss): $tpeopt = { ..$stats;  $parsedStat}"
      case q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" =>
        q"..$mods def $name[..$tparams](...$paramss): $tpeopt = { $expr; $parsedStat  }"
    }

  }
}

package placebo {
  class appendA extends scala.annotation.StaticAnnotation
}
