package org.scalameta.paradise.reflect

trait RemoveRangePositions { self: ReflectToolkit =>
  import g._
  def removeAllRangePositions(tree: Tree): Unit = {
    def loop(parent: Tree)(child: Tree): Unit = {
      if (child.pos.isDefined) child.setPos(child.pos.focus)
      else child.setPos(parent.pos)
      child.children.foreach(loop(child))
    }
    loop(tree)(tree)
  }
}
