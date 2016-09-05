package main

import scala.annotation.compileTimeOnly
import scala.meta._

@compileTimeOnly("@classMacro not expanded")
class classMacro extends scala.annotation.StaticAnnotation {

  inline def apply(stats: Any): Any = meta {
    def extractClass(classDefn: Defn.Class): Stat = {
      val q"""
        ..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends { ..$earlyStats } with ..$ctorcalls {
          $selfParam =>
          ..$stats
        }
      """ = classDefn
      q"""
        ..$mods class $tname[..$tparams] ..$ctorMods (...$paramss) extends { ..$earlyStats } with ..$ctorcalls {
          $selfParam =>
          ..$stats
        }
      """
    }

    def extractObj(objDefn: Defn.Object): Stat = {
      val q"""
        ..$mods object $tname extends { ..$earlyStats } with ..$ctorcalls {
          $selfParam =>
          ..$stats
        }
      """ = objDefn
      q"""
        ..$mods object $tname extends { ..$earlyStats } with ..$ctorcalls {
          $selfParam =>
          ..$stats
        }
      """
    }

    stats match {
      case Term.Block(Seq(classDefn: Defn.Class, objDefn: Defn.Object)) =>
        Term.Block(scala.collection.immutable.Seq(extractClass(classDefn), extractObj(objDefn)))
      case classDefn: Defn.Class => extractClass(classDefn)
    }
  }

}
