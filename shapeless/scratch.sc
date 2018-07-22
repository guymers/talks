import shapeless._
import shapeless.test.illTyped
import shapeless.labelled._
import shapeless.syntax.singleton._
import shapeless.record._
import shapeless.ops.nat._
import shapeless.syntax.sized._

import scala.annotation.implicitNotFound
import cats.syntax.traverse._
import cats.instances.list._
import cats.instances.option._

val zero = Nat._0
val one = Nat(1)
//val two = Succ[Succ[_0]]()
val three = Nat._3

//two.toInt

val sum = Sum[Nat._2, Nat._3]
val five = Nat.toInt[sum.Out]

val min = Min[Nat._2, Nat._3]
val two = Nat.toInt[min.Out]

val gt = GT[Nat._3, Nat._2]
illTyped { """ val notGT = GT[Nat._2, Nat._3] """ }




def row(cols: IndexedSeq[String]) = cols.mkString("'", "','", "'")

def csv[N <: Nat](
  hdrs: Sized[IndexedSeq[String], N],
  rows: List[Sized[IndexedSeq[String], N]]
): List[String] = row(hdrs) :: rows.map(row(_))

val hdrs = Sized("Header 1", "Header 2")

val rows = List(
  Sized("Row 1 Value 1", "Row 1 Value 2"),
  Sized("Row 2 Value 1", "Row 2 Value 2")
)

val formatted = csv(hdrs, rows)


val extendedHdrs = Sized("Header 1", "Header 2", "Header 3")

illTyped { """ csv(extendedHdrs, rows) """ }


val dynamicRows = List(
  IndexedSeq("Row 1 Value 1", "Row 1 Value 2"),
  IndexedSeq("Row 2 Value 1", "Row 2 Value 2")
)

val rows2 = dynamicRows.map(_.sized(2)).flatten
val formatted2 = csv(hdrs, rows)



val hlist = 1 :: "string" :: ("tuple", 1) :: HNil

object size extends Poly1 {
  implicit def caseInt = at[Int](_ => 1)
  implicit def caseString = at[String](_.length)
  implicit def caseTuple[A, B](implicit
    evA: Case.Aux[A, Int],
    evB: Case.Aux[B, Int]
  ) = at[(A, B)](t => size(t._1) + size(t._2))
}

val sizes = hlist.map(size)
hlist.foldMap(0)(size)(_ + _)
sizes.toList



import syntax.std.tuple._

val tuple = (1, "string", true, ("tuple", 1))

tuple.apply(3)

tuple.head

(23, "foo") ++ (true, 2.0)
val tuple2 = tuple.take(2) ++ tuple.drop(3)

//tuple.map()


val fooRecord: shapeless.record.Record =
  ('i ->> 23) ::
    ('s ->> "foo") ::
    ('b ->> true) ::
    HNil
val fooRecordI = fooRecord('i)
