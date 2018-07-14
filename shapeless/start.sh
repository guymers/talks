#!/bin/bash
set -e

readonly IVY_HOME=~/.ivy2

readonly SHAPELESS_JAR="$IVY_HOME/cache/com.chuusai/shapeless_2.12/bundles/shapeless_2.12-2.3.3.jar"

scala -J-Xmx1024M -cp "$SHAPELESS_JAR" -Dscala.color -language:_ -nowarn -i REPLesent.scala
