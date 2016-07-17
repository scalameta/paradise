//package main
//
//import scala.annotation.compileTimeOnly
//import scala.meta._
//
//@compileTimeOnly("@printDef not expanded")
//class printDef extends scala.annotation.StaticAnnotation {
//  inline def apply(defn: Any) = meta {
//    assert(defn.is[Defn.Def])
//    q"println(${defn.toString})"
//  }
//}
//
//@compileTimeOnly("@printVal not expanded")
//class printVal extends scala.annotation.StaticAnnotation {
//  inline def apply(defn: Any) = meta {
//    assert(defn.is[Defn.Val])
//    q"println(${defn.toString})"
//  }
//}
//
//@compileTimeOnly("@printClass not expanded")
//class printClass extends scala.annotation.StaticAnnotation {
//  inline def apply(defn: Any) = meta {
//    assert(defn.is[Defn.Class])
//    q"println(${defn.toString})"
//  }
//}
//
//
//@compileTimeOnly("@identity not expanded")
//class identity extends scala.annotation.StaticAnnotation {
//  inline def apply(defn: Defn) = meta {
//    defn
//  }
//}
