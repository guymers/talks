import shapeless._
import shapeless.labelled._
import shapeless.syntax.singleton._
import shapeless.record._
//import shapeless.ops.nat._

import scala.annotation.implicitNotFound

val a = Succ[Succ[_0]]()

a.toInt


def row(cols : Seq[String]) =
  cols.mkString("\"", "\", \"", "\"")

def csv[N <: Nat]
(hdrs : Sized[Seq[String], N],
  rows : List[Sized[Seq[String], N]]) = row(hdrs) :: rows.map(row(_))

val hdrs = Sized("Title", "Author")

val rows = List(
  Sized("Types and Programming Languages", "Benjamin Pierce"),
  Sized("The Implementation of Functional Programming Languages", "Simon Peyton-Jones")
)

// hdrs and rows statically known to have the same number of columns
val formatted = csv(hdrs, rows)                        // Compiles

// extendedHdrs has the wrong number of columns for rows
val extendedHdrs = Sized("Title", "Author", "ISBN")
//val badFormatted = csv(extendedHdrs, rows)             // Does not compile



val hlist = 1 :: "string" :: true :: ("tuple", 1) :: HNil
val one = hlist(0)
val tuple = hlist(3)




final case class Config(
  str: String,
  num: Int,
  bool: Boolean,
  inner: InnerConfig
)

final case class InnerConfig(str: String, num: Int)

object Config {
  val DEFAULT = Config(
    str = "str",
    num = 0,
    bool = false,
    inner = InnerConfig(str = "inner", num = 1)
  )
}


object Partial {
  case object Undefined extends Partial[Nothing]
  case object Null extends Partial[Nothing]
  final case class Value[+V](value: V) extends Partial[V]
}

sealed abstract class Partial[+A] {

  final def map[B](f: A => B): Partial[B] = this match {
    case Partial.Undefined => Partial.Undefined
    case Partial.Null => Partial.Null
    case Partial.Value(v) => Partial.Value(f(v))
  }

  final def getOrElse[B >: A](default: => B): B = this match {
    case Partial.Undefined => default
    case Partial.Null => default
    case Partial.Value(v) => v
  }

}


final case class PartialConfig(
  str: Partial[String],
  num: Partial[Int],
  bool: Partial[Boolean],
  inner: Partial[PartialInnerConfig]
)

final case class PartialInnerConfig(
  str: Partial[String],
  num: Partial[Int]
)


import io.circe.{Decoder, DecodingFailure, FailedCursor, HCursor}
import io.circe.generic.semiauto.deriveDecoder

object Decoders {

  implicit val partialConfigDecoder: Decoder[PartialConfig] = deriveDecoder
  implicit val partialInnerConfigDecoder: Decoder[PartialInnerConfig] = deriveDecoder

  implicit def partialDecoder[V](implicit D: Decoder[V]): Decoder[Partial[V]] = {
    Decoder.withReattempt {
      case c: HCursor =>
        if (c.value.isNull) Right(Partial.Null)
        else c.as[V].map(Partial.Value.apply)
      case c: FailedCursor =>
        if (!c.incorrectFocus) Right(Partial.Undefined)
        else Left(DecodingFailure("Partial[V]", c.history))
    }
  }
}
import Decoders._


val json = """
  {
    "str": "new str 1",
    "inner": {
      "num": 9
    }
  }
"""
val partialConfig = PartialConfig(
  str = Partial.Value("new str 1"),
  num = Partial.Undefined,
  bool = Partial.Undefined,
  inner = Partial.Value(PartialInnerConfig(
    str = Partial.Undefined,
    num = Partial.Value(9)
  ))
)
val result = io.circe.parser.decode[PartialConfig](json)
assert(result == Right(partialConfig))

sealed trait MergePartial[V, P] {
  def merge(value: V, partialValue: P): V
}

object MergePartial {

  def derive[V, P](implicit v: MergePartialFields.Aux[V, P, V]): MergePartial[V, P] = {
    new MergePartial[V, P] {
      def merge(value: V, partialValue: P): V = v.apply(value, partialValue)
    }
  }

  // if the types are the same then take the partial value
  implicit def mergeSameType[V]: MergePartial[V, V] = new MergePartial[V, V] {
    override def merge(value: V, partialValue: V): V = partialValue
  }

}

sealed trait MergePartialFields[V, P] extends DepFn2[V, P] {
  type Out
}

object MergePartialFields {

  type Aux[V, P, Out0] = MergePartialFields[V, P] { type Out = Out0 }

  implicit val hnil: Aux[HNil, HNil, HNil] =
    new MergePartialFields[HNil, HNil] {
      type Out = HNil

      def apply(v: HNil, p: HNil): Out = HNil
    }

  // merge two values that have an instance of MergePartial instance between them.
  implicit def hcons[K, V, P, VT <: HList, PT <: HList](implicit
    merge: MergePartial[V, P],
    opt: Aux[VT, PT, VT]
  ): Aux[
    FieldType[K, V] :: VT,
    FieldType[K, Partial[P]] :: PT,
    FieldType[K, V] :: opt.Out
  ] =
    new MergePartialFields[FieldType[K, V] :: VT, FieldType[K, Partial[P]] :: PT] {
      type Out = FieldType[K, V] :: opt.Out

      def apply(v: FieldType[K, V] :: VT, p: FieldType[K, Partial[P]] :: PT): Out = {
        val head: V = (p.head: Partial[P]) match {
          case Partial.Undefined | Partial.Null => v.head
          case Partial.Value(_p) => merge.merge(v.head, _p)
        }
        field[K](head) :: opt(v.tail, p.tail)
      }
    }

  implicit def clazz[V, P, VL <: HList, PL <: HList](implicit
    vGen: LabelledGeneric.Aux[V, VL],
    pGen: LabelledGeneric.Aux[P, PL],
    merge: Aux[VL, PL, VL]
  ): Aux[V, P, V] =
    new MergePartialFields[V, P] {
      type Out = V

      def apply(v: V, p: P): Out = {
        val hlist = merge(vGen.to(v), pGen.to(p))
        vGen.from(hlist)
      }
    }

}

//implicit val innerMP: MergePartial[InnerConfig, PartialInnerConfig] = MergePartial.derive
//implicit val configMP: MergePartial[Config, PartialConfig] = MergePartial.derive

implicit val configMergePartial: MergePartial[Config, PartialConfig] = {
  implicit val inner = MergePartial.derive[InnerConfig, PartialInnerConfig]
  MergePartial.derive[Config, PartialConfig]
}

configMergePartial.merge(Config.DEFAULT, partialConfig)


final case class Config2(
  str: String,
  optStr: Option[String],
  num: Int,
  bool: Boolean,
  inner: InnerConfig
)
