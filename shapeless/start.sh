#!/bin/bash
set -e

readonly IVY_HOME=~/.ivy2

declare -a classpath=(
  "$IVY_HOME/cache/com.chuusai/shapeless_2.12/bundles/shapeless_2.12-2.3.3.jar"
  "$IVY_HOME/cache/io.circe/circe-core_2.12/jars/circe-core_2.12-0.9.3.jar"
  "$IVY_HOME/cache/io.circe/circe-generic_2.12/jars/circe-generic_2.12-0.9.3.jar"
  "$IVY_HOME/cache/io.circe/circe-numbers_2.12/jars/circe-numbers_2.12-0.9.3.jar"
  "$IVY_HOME/cache/io.circe/circe-parser_2.12/jars/circe-parser_2.12-0.9.3.jar"
  "$IVY_HOME/cache/io.circe/circe-jawn_2.12/jars/circe-jawn_2.12-0.9.3.jar"
  "$IVY_HOME/cache/org.spire-math/jawn-parser_2.12/jars/jawn-parser_2.12-0.11.1.jar"
  "$IVY_HOME/cache/org.typelevel/cats-core_2.12/jars/cats-core_2.12-1.1.0.jar"
  "$IVY_HOME/cache/org.typelevel/cats-macros_2.12/jars/cats-macros_2.12-1.1.0.jar"
  "$IVY_HOME/cache/org.typelevel/cats-kernel_2.12/jars/cats-kernel_2.12-1.1.0.jar"
  "$IVY_HOME/cache/org.typelevel/machinist_2.12/jars/machinist_2.12-0.6.2.jar"
)
readonly classpath_length=${#classpath[@]}

classpath_str="${classpath[0]}"
for (( i = 1; i < ${classpath_length}; i++ )); do
  classpath_str="$classpath_str:${classpath[i]}"
done

scala -J-Xmx1024M -cp "$classpath_str" -Dscala.color -language:_ -nowarn \
  -explaintypes \
  -Ypartial-unification \
  -i REPLesent.scala
