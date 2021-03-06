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

trait EntryIdxOps extends Base with GenericEntryOps {  
  // Type representation
  val EntryIdxType = EntryIdxIRs.EntryIdxType
  type EntryIdxType[E <: ddbt.lib.store.Entry] = EntryIdxIRs.EntryIdxType[E]
  implicit def typeEntryIdx[E <: ddbt.lib.store.Entry: TypeRep]: TypeRep[EntryIdx[E]] = EntryIdxType(implicitly[TypeRep[E]])
  implicit class EntryIdxRep[E <: ddbt.lib.store.Entry](self : Rep[EntryIdx[E]])(implicit typeE : TypeRep[E]) {
     def cmp(e1 : Rep[E], e2 : Rep[E]) : Rep[Int] = entryIdxCmp[E](self, e1, e2)(typeE)
     def hash(e : Rep[E]) : Rep[Int] = entryIdxHash[E](self, e)(typeE)
  }
  object EntryIdx {
     def apply[E <: ddbt.lib.store.Entry](h : Rep[(E => Int)], c : Rep[((E,E) => Int)], entryidxname : Rep[String])(implicit typeE : TypeRep[E]) : Rep[EntryIdx[E]] = entryIdxApplyObject[E](h, c, entryidxname)(typeE)
     def genericOps(cols : Rep[Seq[Int]]) : Rep[EntryIdx[GenericEntry]] = entryIdxGenericOpsObject(cols)
     def genericCmp[R](cols : Rep[Seq[Int]], f : Rep[(GenericEntry => R)])(implicit typeR : TypeRep[R]) : Rep[EntryIdx[GenericEntry]] = entryIdxGenericCmpObject[R](cols, f)(typeR)
     def genericFixedRangeOps(colsRange : Rep[Seq[Tuple3[Int, Int, Int]]]) : Rep[EntryIdx[GenericEntry]] = entryIdxGenericFixedRangeOpsObject(colsRange)
  }
  // constructors

  // IR defs
  val EntryIdxCmp = EntryIdxIRs.EntryIdxCmp
  type EntryIdxCmp[E <: ddbt.lib.store.Entry] = EntryIdxIRs.EntryIdxCmp[E]
  val EntryIdxHash = EntryIdxIRs.EntryIdxHash
  type EntryIdxHash[E <: ddbt.lib.store.Entry] = EntryIdxIRs.EntryIdxHash[E]
  val EntryIdxApplyObject = EntryIdxIRs.EntryIdxApplyObject
  type EntryIdxApplyObject[E <: ddbt.lib.store.Entry] = EntryIdxIRs.EntryIdxApplyObject[E]
  val EntryIdxGenericOpsObject = EntryIdxIRs.EntryIdxGenericOpsObject
  type EntryIdxGenericOpsObject = EntryIdxIRs.EntryIdxGenericOpsObject
  val EntryIdxGenericCmpObject = EntryIdxIRs.EntryIdxGenericCmpObject
  type EntryIdxGenericCmpObject[R] = EntryIdxIRs.EntryIdxGenericCmpObject[R]
  val EntryIdxGenericFixedRangeOpsObject = EntryIdxIRs.EntryIdxGenericFixedRangeOpsObject
  type EntryIdxGenericFixedRangeOpsObject = EntryIdxIRs.EntryIdxGenericFixedRangeOpsObject
  // method definitions
   def entryIdxCmp[E <: ddbt.lib.store.Entry](self : Rep[EntryIdx[E]], e1 : Rep[E], e2 : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Int] = EntryIdxCmp[E](self, e1, e2)
   def entryIdxHash[E <: ddbt.lib.store.Entry](self : Rep[EntryIdx[E]], e : Rep[E])(implicit typeE : TypeRep[E]) : Rep[Int] = EntryIdxHash[E](self, e)
   def entryIdxApplyObject[E <: ddbt.lib.store.Entry](h : Rep[(E => Int)], c : Rep[((E,E) => Int)], entryidxname : Rep[String])(implicit typeE : TypeRep[E]) : Rep[EntryIdx[E]] = EntryIdxApplyObject[E](h, c, entryidxname)
   def entryIdxGenericOpsObject(cols : Rep[Seq[Int]]) : Rep[EntryIdx[GenericEntry]] = EntryIdxGenericOpsObject(cols)
   def entryIdxGenericCmpObject[R](cols : Rep[Seq[Int]], f : Rep[(GenericEntry => R)])(implicit typeR : TypeRep[R]) : Rep[EntryIdx[GenericEntry]] = EntryIdxGenericCmpObject[R](cols, f)
   def entryIdxGenericFixedRangeOpsObject(colsRange : Rep[Seq[Tuple3[Int, Int, Int]]]) : Rep[EntryIdx[GenericEntry]] = EntryIdxGenericFixedRangeOpsObject(colsRange)
  type EntryIdx[E <: ddbt.lib.store.Entry] = ddbt.lib.store.EntryIdx[E]
}
object EntryIdxIRs extends Base {
  import GenericEntryIRs._
  // Type representation
  case class EntryIdxType[E <: ddbt.lib.store.Entry](typeE: TypeRep[E]) extends TypeRep[EntryIdx[E]] {
    def rebuild(newArguments: TypeRep[_]*): TypeRep[_] = EntryIdxType(newArguments(0).asInstanceOf[TypeRep[_ <: ddbt.lib.store.Entry]])
    val name = s"EntryIdx[${typeE.name}]"
    val typeArguments = List(typeE)
  }
      implicit def typeEntryIdx[E <: ddbt.lib.store.Entry: TypeRep]: TypeRep[EntryIdx[E]] = EntryIdxType(implicitly[TypeRep[E]])
  // case classes
  case class EntryIdxCmp[E <: ddbt.lib.store.Entry](self : Rep[EntryIdx[E]], e1 : Rep[E], e2 : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[Int](Some(self), "cmp", List(List(e1,e2))){
    override def curriedConstructor = (copy[E] _).curried
    override def isPure = true

    override def partiallyEvaluate(children: Any*): Int = {
      val self = children(0).asInstanceOf[EntryIdx[E]]
      val e1 = children(1).asInstanceOf[E]
      val e2 = children(2).asInstanceOf[E]
      self.cmp(e1, e2)
    }
    override def partiallyEvaluable: Boolean = true

  }

  case class EntryIdxHash[E <: ddbt.lib.store.Entry](self : Rep[EntryIdx[E]], e : Rep[E])(implicit val typeE : TypeRep[E]) extends FunctionDef[Int](Some(self), "hash", List(List(e))){
    override def curriedConstructor = (copy[E] _).curried
    override def isPure = true

    override def partiallyEvaluate(children: Any*): Int = {
      val self = children(0).asInstanceOf[EntryIdx[E]]
      val e = children(1).asInstanceOf[E]
      self.hash(e)
    }
    override def partiallyEvaluable: Boolean = true

  }

  case class EntryIdxApplyObject[E <: ddbt.lib.store.Entry](h : Rep[(E => Int)], c : Rep[((E,E) => Int)], entryidxname : Rep[String])(implicit val typeE : TypeRep[E]) extends FunctionDef[EntryIdx[E]](None, "EntryIdx.apply", List(List(h,c,entryidxname))){
    override def curriedConstructor = (copy[E] _).curried
    override def isPure = true

    override def partiallyEvaluate(children: Any*): EntryIdx[E] = {
      val h = children(0).asInstanceOf[(E => Int)]
      val c = children(1).asInstanceOf[((E,E) => Int)]
      val entryidxname = children(2).asInstanceOf[String]
      ddbt.lib.store.MirrorEntryIdx.apply[E](h, c, entryidxname)
    }
    override def partiallyEvaluable: Boolean = true

  }

  case class EntryIdxGenericOpsObject(cols : Rep[Seq[Int]]) extends FunctionDef[EntryIdx[GenericEntry]](None, "EntryIdx.genericOps", List(List(cols))){
    override def curriedConstructor = (copy _)
    override def isPure = true

    override def partiallyEvaluate(children: Any*): EntryIdx[GenericEntry] = {
      val cols = children(0).asInstanceOf[Seq[Int]]
      ddbt.lib.store.MirrorEntryIdx.genericOps(cols)
    }
    override def partiallyEvaluable: Boolean = true

  }

  case class EntryIdxGenericCmpObject[R](cols : Rep[Seq[Int]], f : Rep[(GenericEntry => R)])(implicit val typeR : TypeRep[R]) extends FunctionDef[EntryIdx[GenericEntry]](None, "EntryIdx.genericCmp", List(List(cols,f))){
    override def curriedConstructor = (copy[R] _).curried
    override def isPure = true

    override def partiallyEvaluate(children: Any*): EntryIdx[GenericEntry] = {
      val cols = children(0).asInstanceOf[Seq[Int]]
      val f = children(1).asInstanceOf[(GenericEntry => R)]
      ddbt.lib.store.MirrorEntryIdx.genericCmp[R](cols, f)
    }
    override def partiallyEvaluable: Boolean = true

  }

  case class EntryIdxGenericFixedRangeOpsObject(colsRange : Rep[Seq[Tuple3[Int, Int, Int]]]) extends FunctionDef[EntryIdx[GenericEntry]](None, "EntryIdx.genericFixedRangeOps", List(List(colsRange))){
    override def curriedConstructor = (copy _)
    override def isPure = true

    override def partiallyEvaluate(children: Any*): EntryIdx[GenericEntry] = {
      val colsRange = children(0).asInstanceOf[Seq[Tuple3[Int, Int, Int]]]
      ddbt.lib.store.MirrorEntryIdx.genericFixedRangeOps(colsRange)
    }
    override def partiallyEvaluable: Boolean = true

  }

  type EntryIdx[E <: ddbt.lib.store.Entry] = ddbt.lib.store.EntryIdx[E]
}
trait EntryIdxImplicits extends EntryIdxOps { 
  // Add implicit conversions here!
}
trait EntryIdxComponent extends EntryIdxOps with EntryIdxImplicits {  }

trait EntryIdxPartialEvaluation extends EntryIdxComponent with BasePartialEvaluation {  
  // Immutable field inlining 

  // Mutable field inlining 
  // Pure function partial evaluation
}


