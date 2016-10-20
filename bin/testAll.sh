#!/usr/bin/env bash
set -e
sbt -Dsbt.ivy.home=/drone/cache/ivy2 clean test
# run separately to avoid concurrency issues with how ScalaTest run test suites in parallel.
sbt -Dsbt.ivy.home=/drone/cache/ivy2 "testsConverter/test:runMain org.scalameta.tests.LotsOfProjects"
