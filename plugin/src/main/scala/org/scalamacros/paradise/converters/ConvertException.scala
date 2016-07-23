package org.scalamacros.paradise.converters

import org.scalameta.data._

@data class ConvertException(culprit: Any, message: String, cause: Option[Throwable] = None)
extends Exception(message, cause.orNull) {
  override def toString = super.toString
}
