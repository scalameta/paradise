package org.scalameta.paradise
package converters

import scala.meta.internal.ast.InternalTreeXtensions
import scala.tools.nsc.Global

import org.scalameta.paradise.reflect.ReflectToolkit

trait Converter
    extends ReflectToolkit
    with ToMtree
    with DenotationConverter
    with InternalTreeXtensions // TODO(olafur) remove once semantic api settles.
