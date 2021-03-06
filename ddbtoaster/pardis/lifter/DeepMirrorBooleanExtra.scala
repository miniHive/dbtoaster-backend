/* Generated by Purgatory 2014-2017 */

package ddbt.lib.store.deep

import ch.epfl.data.sc.pardis
import pardis.ir._
import pardis.types.PardisTypeImplicits._
import pardis.effects._
import pardis.deep._
import pardis.deep.scalalib._
import pardis.deep.scalalib.collection._
import pardis.deep.scalalib.io._

trait BooleanExtraOps extends Base  {  
  // Type representation
  val BooleanExtraType = BooleanExtraIRs.BooleanExtraType
  implicit val typeBooleanExtra: TypeRep[BooleanExtra] = BooleanExtraType
  implicit class BooleanExtraRep(self : Rep[BooleanExtra]) {

  }
  object BooleanExtra {
     def conditional[T](cond : Rep[Boolean], ift : Rep[T], iff : Rep[T])(implicit typeT : TypeRep[T]) : Rep[T] = booleanExtraConditionalObject[T](cond, ift, iff)(typeT)
  }
  // constructors

  // IR defs
  val BooleanExtraConditionalObject = BooleanExtraIRs.BooleanExtraConditionalObject
  type BooleanExtraConditionalObject[T] = BooleanExtraIRs.BooleanExtraConditionalObject[T]
  // method definitions
   def booleanExtraConditionalObject[T](cond : Rep[Boolean], ift : Rep[T], iff : Rep[T])(implicit typeT : TypeRep[T]) : Rep[T] = BooleanExtraConditionalObject[T](cond, ift, iff)
  type BooleanExtra = ddbt.lib.store.BooleanExtra
}
object BooleanExtraIRs extends Base {
  // Type representation
  case object BooleanExtraType extends TypeRep[BooleanExtra] {
    def rebuild(newArguments: TypeRep[_]*): TypeRep[_] = BooleanExtraType
    val name = "BooleanExtra"
    val typeArguments = Nil
  }
      implicit val typeBooleanExtra: TypeRep[BooleanExtra] = BooleanExtraType
  // case classes
  case class BooleanExtraConditionalObject[T](cond : Rep[Boolean], ift : Rep[T], iff : Rep[T])(implicit val typeT : TypeRep[T]) extends FunctionDef[T](None, "BooleanExtra.conditional", List(List(cond,ift,iff))){
    override def curriedConstructor = (copy[T] _).curried
    override def isPure = true

    override def partiallyEvaluate(children: Any*): T = {
      val cond = children(0).asInstanceOf[Boolean]
      val ift = children(1).asInstanceOf[T]
      val iff = children(2).asInstanceOf[T]
      ddbt.lib.store.BooleanExtra.conditional[T](cond, ift, iff)
    }
    override def partiallyEvaluable: Boolean = true

  }

  type BooleanExtra = ddbt.lib.store.BooleanExtra
}
trait BooleanExtraImplicits extends BooleanExtraOps { 
  // Add implicit conversions here!
}
trait BooleanExtraComponent extends BooleanExtraOps with BooleanExtraImplicits {  }

trait BooleanExtraPartialEvaluation extends BooleanExtraComponent with BasePartialEvaluation {  
  // Immutable field inlining 

  // Mutable field inlining 
  // Pure function partial evaluation
}


