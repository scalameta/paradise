#!/usr/bin/env bash

# produce .class files with TASTY sections.
sbt -Dpersist.enable semanticCompile/compile
sbt test
