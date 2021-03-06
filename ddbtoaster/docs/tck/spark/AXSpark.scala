package examples
import ddbt.lib._
import Messages._

import spark.SparkContext
import SparkContext._

import spark.RDD
//import spark.HashPartitioner
//import spark.Partitioner
import scala.reflect.ClassTag

// Idea: use a Partitioner to create buckets (?)
// http://ampcamp.berkeley.edu/wp-content/uploads/2012/06/matei-zaharia-amp-camp-2012-advanced-spark.pdf
// Store hash maps instead of values within the sequences

abstract class AXSparkBase {
  val sc = new SparkContext("local[8]", "SimpleJob", "$YOUR_SPARK_HOME",
                  List("target/scala-2.10/distributeddbtoaster_2.10-0.1.jar")) // Match SBT name/version

  val N = 64 // approximately #nodes x #cpus/nodes

  import scala.language.implicitConversions  
  implicit def boolConv(b:Boolean):Long = if (b) 1L else 0L

  def onAddBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double):Unit
  def onDelBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double):Unit
  def onAddASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double):Unit
  def onDelASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double):Unit
  
  def dispatch(ev:TupleEvent):Unit = ev match {
    case TupleEvent(TupleInsert,"BIDS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onAddBIDS(t,id,b,v,p)
    case TupleEvent(TupleDelete,"BIDS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onDelBIDS(t,id,b,v,p)
    case TupleEvent(TupleInsert,"ASKS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onAddASKS(t,id,b,v,p)
    case TupleEvent(TupleDelete,"ASKS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onDelASKS(t,id,b,v,p)
  }

  def read(file:String) = {
    val d = Decoder(dispatch);
    val bufSize:Int=128*1024
    val in = new java.io.FileInputStream(file)
    val buf = new Array[Byte](bufSize)
    var n:Int = 0
    val t0 = System.nanoTime()
    do { n=in.read(buf); d.add(buf,n); } while (n>0);
    val t1 = System.nanoTime()
    in.close()
    (t1-t0) / 1000
  }
}


object AXSpark3 extends AXSparkBase {
  val AXFINDER = KDMap.make[Long,Double](sc,N,0.0)
  val AXFINDER_mASKS1 = KDMap.make[(Long,Double),Double](sc,N,0.0,List((x:(Long,Double))=>x._1))
  val AXFINDER_mASKS2 = KDMap.make[(Long,Double),Long](sc,N,0L,List((x:(Long,Double))=>x._1))
  val AXFINDER_mBIDS1 = KDMap.make[(Long,Double),Long](sc,N,0L,List((x:(Long,Double))=>x._1))
  val AXFINDER_mBIDS3 = KDMap.make[(Long,Double),Double](sc,N,0.0,List((x:(Long,Double))=>x._1))

  def onAddBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double) {
    val agg1 = sc.accumulator(0L)
    AXFINDER_mBIDS1.rdd.foreach { case (n,m) => var lacc=0L
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg1 += lacc
    }
    val agg4 = sc.accumulator(0.0)
    AXFINDER_mBIDS3.rdd.foreach { case (n,m) =>  var lacc = 0.0;
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg4 += lacc
    }
    AXFINDER.add(BIDS_BROKER_ID,((agg1.value * -BIDS_VOLUME) + agg4.value));
    AXFINDER_mASKS1.add((BIDS_BROKER_ID,BIDS_PRICE),BIDS_VOLUME);
    AXFINDER_mASKS2.add((BIDS_BROKER_ID,BIDS_PRICE),1L);
  }

  def onDelBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double) {
    val agg7 = sc.accumulator(0L)
    AXFINDER_mBIDS1.rdd.foreach { case (n,m) => var lacc=0L
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg7 += lacc
    }
    val agg10 = sc.accumulator(0.0)
    AXFINDER_mBIDS3.rdd.foreach { case (n,m) => var lacc=0.0
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg10 += lacc
    }
    AXFINDER.add(BIDS_BROKER_ID,((agg7.value * BIDS_VOLUME) + (agg10.value * -1L)));
    AXFINDER_mASKS1.add((BIDS_BROKER_ID,BIDS_PRICE),-BIDS_VOLUME);
    AXFINDER_mASKS2.add((BIDS_BROKER_ID,BIDS_PRICE),-1L);
  }

  def onAddASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double) {
    val agg13 = sc.accumulator(0.0)
    AXFINDER_mASKS1.rdd.foreach { case (n,m) => var lacc=0.0
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg13 += lacc
    }
    val agg16 = sc.accumulator(0L)
    AXFINDER_mASKS2.rdd.foreach { case (n,m) => var lacc=0L
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg16 += lacc
    }
    AXFINDER.add(ASKS_BROKER_ID,((agg13.value * -1L) + (agg16.value * ASKS_VOLUME)));
    AXFINDER_mBIDS1.add((ASKS_BROKER_ID,ASKS_PRICE),1L);
    AXFINDER_mBIDS3.add((ASKS_BROKER_ID,ASKS_PRICE),ASKS_VOLUME);
  }

  def onDelASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double) {
    var agg19 = sc.accumulator(0.0)
    AXFINDER_mASKS1.rdd.foreach { case (n,m) => var lacc=0.0
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg19 += lacc
    }
    val agg22 = sc.accumulator(0L)
    AXFINDER_mASKS2.rdd.foreach { case (n,m) => var lacc=0L
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg22 += lacc
    }
    AXFINDER.add(ASKS_BROKER_ID,(agg19.value + (agg22.value * -ASKS_VOLUME)));
    AXFINDER_mBIDS1.add((ASKS_BROKER_ID,ASKS_PRICE),-1L);
    AXFINDER_mBIDS3.add((ASKS_BROKER_ID,ASKS_PRICE),-ASKS_VOLUME);
  }

  // Run with: sbt ';package;run-main examples.AXSpark3'
  def main(args:Array[String]) {
    val t = read("resources/data/finance.csv")
    AXFINDER.collect { case (k,v) => println(k+" => "+v); }
    sc.stop
    println("Running time: %d.%06d".format(t/1000000,t%1000000))
  }
}

// -----------------------------------------------------------------------------

object AXSpark2 extends AXSparkBase {
  // This is manually written for the moment, needs to go into ddbt.lib

  type Bulk[K,V] = RDD[(Int,KSMap[K,V])] // partition id, internal map
  def makeBulk[K,V](v0:V,projs:List[K=>_]=Nil) : Bulk[K,V] = {
    def idx[P](f:K=>P) = new KSIndex[P,K,V](f)
    sc.makeRDD((0 until N).map{x=>(x,new KSMap(v0,projs.map(idx(_))))},N)
  }
  // Pay attention to modulo for negative numbers
  def h(k:Any):Int = { val x = k.hashCode % N; (x + N) % N }
  def add[K,V](b:Bulk[K,V],k:K,v:V):Bulk[K,V] = b.map{ case (n,m) => if (h(k)==n) (n,m.add(k,v)) else (n,m) }

  var AXFINDER : Bulk[Long,Double] = makeBulk[Long,Double](0.0)
  var AXFINDER_mASKS1 : Bulk[(Long,Double),Double] = makeBulk[(Long,Double),Double](0.0,List((x:(Long,Double))=>x._1))
  var AXFINDER_mASKS2 : Bulk[(Long,Double),Long] = makeBulk[(Long,Double),Long](0L,List((x:(Long,Double))=>x._1))
  var AXFINDER_mBIDS1 : Bulk[(Long,Double),Long] = makeBulk[(Long,Double),Long](0L,List((x:(Long,Double))=>x._1))
  var AXFINDER_mBIDS3 : Bulk[(Long,Double),Double] = makeBulk[(Long,Double),Double](0.0,List((x:(Long,Double))=>x._1))

  def onAddBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double) {
    val agg1 = sc.accumulator(0L)
    AXFINDER_mBIDS1.foreach { case (n,m) => var lacc=0L
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg1 += lacc
    }
    val agg4 = sc.accumulator(0.0)
    AXFINDER_mBIDS3.foreach { case (n,m) =>  var lacc = 0.0;
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg4 += lacc
    }
    AXFINDER = add(AXFINDER,BIDS_BROKER_ID,((agg1.value * -BIDS_VOLUME) + agg4.value));
    AXFINDER_mASKS1 = add(AXFINDER_mASKS1,(BIDS_BROKER_ID,BIDS_PRICE),BIDS_VOLUME);
    AXFINDER_mASKS2 = add(AXFINDER_mASKS2,(BIDS_BROKER_ID,BIDS_PRICE),1L);
  }

  def onDelBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double) {
    val agg7 = sc.accumulator(0L)
    AXFINDER_mBIDS1.foreach { case (n,m) => var lacc=0L
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg7 += lacc
    }
    val agg10 = sc.accumulator(0.0)
    AXFINDER_mBIDS3.foreach { case (n,m) => var lacc=0.0
      m.slice(0,BIDS_BROKER_ID).foreach { case (k,v) => val A_PRICE = k._2;
        val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg10 += lacc
    }
    AXFINDER = add(AXFINDER,BIDS_BROKER_ID,((agg7.value * BIDS_VOLUME) + (agg10.value * -1L)));
    AXFINDER_mASKS1 = add(AXFINDER_mASKS1,(BIDS_BROKER_ID,BIDS_PRICE),-BIDS_VOLUME);
    AXFINDER_mASKS2 = add(AXFINDER_mASKS2,(BIDS_BROKER_ID,BIDS_PRICE),-1L);
  }

  def onAddASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double) {
    val agg13 = sc.accumulator(0.0)
    AXFINDER_mASKS1.foreach { case (n,m) => var lacc=0.0
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg13 += lacc
    }
    val agg16 = sc.accumulator(0L)
    AXFINDER_mASKS2.foreach { case (n,m) => var lacc=0L
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg16 += lacc
    }
    AXFINDER = add(AXFINDER,ASKS_BROKER_ID,((agg13.value * -1L) + (agg16.value * ASKS_VOLUME)));
    AXFINDER_mBIDS1 = add(AXFINDER_mBIDS1,(ASKS_BROKER_ID,ASKS_PRICE),1L);
    AXFINDER_mBIDS3 = add(AXFINDER_mBIDS3,(ASKS_BROKER_ID,ASKS_PRICE),ASKS_VOLUME);
  }

  def onDelASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double) {
    var agg19 = sc.accumulator(0.0)
    AXFINDER_mASKS1.foreach { case (n,m) => var lacc=0.0
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg19 += lacc
    }
    val agg22 = sc.accumulator(0L)
    AXFINDER_mASKS2.foreach { case (n,m) => var lacc=0L
      m.slice(0,ASKS_BROKER_ID).foreach { case (k,v) => val B_PRICE = k._2;
        val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
        lacc += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
      }
      agg22 += lacc
    }
    AXFINDER = add(AXFINDER,ASKS_BROKER_ID,(agg19.value + (agg22.value * -ASKS_VOLUME)));
    AXFINDER_mBIDS1 = add(AXFINDER_mBIDS1,(ASKS_BROKER_ID,ASKS_PRICE),-1L);
    AXFINDER_mBIDS3 = add(AXFINDER_mBIDS3,(ASKS_BROKER_ID,ASKS_PRICE),-ASKS_VOLUME);
  }

  // Run with: sbt ';package;run-main examples.AXSpark2'
  def main(args:Array[String]) {
    val t = read("resources/data/finance.csv")
    AXFINDER.collect.foreach { case (n,m) => m.foreach { case (k,v) => println(k+" => "+v); }}
    sc.stop
    println("Running time: %d.%06d".format(t/1000000,t%1000000))
  }
}

// -----------------------------------------------------------------------------



/*

AXFinderRef (0.063869)
0 => 7590.0
1 => 0.0
2 => -1395.0
3 => 95.0
4 => 0.0
5 => 1080.0
6 => 490.0
7 => -907.0
8 => 2419.0
9 => 45.0

AXFinder (0.010786)
0 => 7590.0
2 => -1395.0
3 => 95.0
5 => 1080.0
6 => 490.0
7 => -907.0
8 => 2419.0
9 => 45.0

AXSpark (51.798408)
AXSpark2 (5.159698)
AXSpark3 (5.144481)

*/






// Idea: use a Partitioner to create buckets (?)
// http://ampcamp.berkeley.edu/wp-content/uploads/2012/06/matei-zaharia-amp-camp-2012-advanced-spark.pdf

object AXSpark {
  val sc = new SparkContext("local[4]", "Simple Job", "$YOUR_SPARK_HOME",
             List("target/scala-2.10/distributeddbtoaster_2.10-0.1.jar")) // Match SBT name/version

  // This is manually written for the moment, needs to go into ddbt.lib
  import scala.language.implicitConversions  
  implicit def boolConv(b:Boolean):Long = if (b) 1L else 0L

  // new HashPartitioner(2)); // XXX: this might be a bad decision, but gives better perf locally
  /*
  object Part extends Partitioner {
    val N = 2
    def numPartitions = N
    def getPartition(key:(Long,Double)) = (key._1%20)
    def getPartition(key: Any):Int = key.hashCode()%N
  }
  */

  var AXFINDER = sc.parallelize(Seq[(Long,Double)]()) //.partitionBy(Part);
  var AXFINDER_mASKS1 = sc.parallelize(Seq[((Long,Double),Double)]()) //.partitionBy(Part);
  var AXFINDER_mASKS2 = sc.parallelize(Seq[((Long,Double),Long)]()) //.partitionBy(Part);
  var AXFINDER_mBIDS1 = sc.parallelize(Seq[((Long,Double),Long)]()) //.partitionBy(Part);
  var AXFINDER_mBIDS3 = sc.parallelize(Seq[((Long,Double),Double)]()) //.partitionBy(Part);

  var ctr:Int = 0
  def count { ctr=ctr+1; println("Events: "+ctr); }

  // Without merge: 24.74
  // With merge: 47.10

  def add[K:ClassTag](c:RDD[(K,Long)],k:K,v:Long):RDD[(K,Long)] = if (v==0) c
     else if (c.lookup(k).size>0) c.map{case (k0,v0) => (k0,if (k0==k) v0+v else v0) }
     else ( c ++ sc.parallelize(Seq((k,v))) )

  def add[K:ClassTag](c:RDD[(K,Double)],k:K,v:Double):RDD[(K,Double)] = if (v==0) c
     else if (c.lookup(k).size>0) c.map{case (k0,v0) => (k0,if (k0==k) v0+v else v0) }
     else ( c ++ sc.parallelize(Seq((k,v))) )

  def onAddBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double) {
    val agg1 = sc.accumulator(0L)
    AXFINDER_mBIDS1.foreach { case (k,v) => if (k._1==BIDS_BROKER_ID) { val A_PRICE = k._2;
      val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
      agg1 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    val agg4 = sc.accumulator(0.0)
    AXFINDER_mBIDS3.foreach { case (k,v) => if (k._1==BIDS_BROKER_ID) { val A_PRICE = k._2;
      val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
      agg4 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    AXFINDER = add(AXFINDER,BIDS_BROKER_ID,((agg1.value * -BIDS_VOLUME) + agg4.value));
    AXFINDER_mASKS1 = add(AXFINDER_mASKS1,(BIDS_BROKER_ID,BIDS_PRICE),BIDS_VOLUME);
    AXFINDER_mASKS2 = add(AXFINDER_mASKS2,(BIDS_BROKER_ID,BIDS_PRICE),1L);
    count
  }
  
  def onDelBIDS(BIDS_T:Double, BIDS_ID:Long, BIDS_BROKER_ID:Long, BIDS_VOLUME:Double, BIDS_PRICE:Double) {
    val agg7 = sc.accumulator(0L)
    AXFINDER_mBIDS1.foreach { case (k,v) => if (k._1==BIDS_BROKER_ID) { val A_PRICE = k._2;
      val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
      agg7 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    val agg10 = sc.accumulator(0.0)
    AXFINDER_mBIDS3.foreach { case (k,v) => if (k._1==BIDS_BROKER_ID) { val A_PRICE = k._2;
      val __sql_inline_or_1 = (((A_PRICE - BIDS_PRICE) > 1000L) + ((BIDS_PRICE - A_PRICE) > 1000L));
      agg10 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    AXFINDER = add(AXFINDER,BIDS_BROKER_ID,((agg7.value * BIDS_VOLUME) + (agg10.value * -1L)));
    AXFINDER_mASKS1 = add(AXFINDER_mASKS1,(BIDS_BROKER_ID,BIDS_PRICE),-BIDS_VOLUME);
    AXFINDER_mASKS2 = add(AXFINDER_mASKS2,(BIDS_BROKER_ID,BIDS_PRICE),-1L);
    count
  }

  def onAddASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double) {
    val agg13 = sc.accumulator(0.0)
    AXFINDER_mASKS1.foreach { case (k,v) => if (k._1==ASKS_BROKER_ID) { val B_PRICE = k._2;
      val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
      agg13 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    val agg16 = sc.accumulator(0L)
    AXFINDER_mASKS2.foreach { case (k,v) => if (k._1==ASKS_BROKER_ID) { val B_PRICE = k._2;
      val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
      agg16 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    AXFINDER = add(AXFINDER,ASKS_BROKER_ID,((agg13.value * -1L) + (agg16.value * ASKS_VOLUME)));
    AXFINDER_mBIDS1 = add(AXFINDER_mBIDS1,(ASKS_BROKER_ID,ASKS_PRICE),1L);
    AXFINDER_mBIDS3 = add(AXFINDER_mBIDS3,(ASKS_BROKER_ID,ASKS_PRICE),ASKS_VOLUME);
    count
  }

  def onDelASKS(ASKS_T:Double, ASKS_ID:Long, ASKS_BROKER_ID:Long, ASKS_VOLUME:Double, ASKS_PRICE:Double) {
    var agg19 = sc.accumulator(0.0)
    AXFINDER_mASKS1.foreach { case (k,v) => if (k._1==ASKS_BROKER_ID) { val B_PRICE = k._2;
      val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
      agg19 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    val agg22 = sc.accumulator(0L)
    AXFINDER_mASKS2.foreach { case (k,v) => if (k._1==ASKS_BROKER_ID) { val B_PRICE = k._2;
      val __sql_inline_or_1 = (((ASKS_PRICE - B_PRICE) > 1000L) + ((B_PRICE - ASKS_PRICE) > 1000L));
      agg22 += (v * (__sql_inline_or_1 * (__sql_inline_or_1 > 0L)));
    }}
    AXFINDER = add(AXFINDER,ASKS_BROKER_ID,(agg19.value + (agg22.value * -ASKS_VOLUME)));
    AXFINDER_mBIDS1 = add(AXFINDER_mBIDS1,(ASKS_BROKER_ID,ASKS_PRICE),-1L);
    AXFINDER_mBIDS3 = add(AXFINDER_mBIDS3,(ASKS_BROKER_ID,ASKS_PRICE),-ASKS_VOLUME);
    count
  }

  def dispatch(ev:TupleEvent):Unit = ev match {
    case TupleEvent(TupleInsert,"BIDS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onAddBIDS(t,id,b,v,p)
    case TupleEvent(TupleDelete,"BIDS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onDelBIDS(t,id,b,v,p)
    case TupleEvent(TupleInsert,"ASKS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onAddASKS(t,id,b,v,p)
    case TupleEvent(TupleDelete,"ASKS",tx,List(t:Double,id:Long,b:Long,v:Double,p:Double)) => onDelASKS(t,id,b,v,p)
  }

  // Run with
  //
  //   sbt ';package;run-main examples.AXSpark'
  //
  def main(args:Array[String]) {
    val d = Decoder(dispatch);
    // read all
    val bufSize:Int=128*1024
    val in = new java.io.FileInputStream("resources/data/finance.csv") // -standard
    // AXFinder
    // 33.064829 for standard
    // 1. accumulator avoids building intermediate filtered lists: -10 sec
    // 2. disabling log4j (echo 'log4j.rootLogger=OFF' > conf/log4j.properties): -5 sec
    // 3. Local tweak #cores workers, 2 paritions: -9 sec
    // CPS: 0.314622 for current implementation
    val buf = new Array[Byte](bufSize)
    var n:Int = 0

    val t0 = System.nanoTime()
    do { n=in.read(buf); d.add(buf,n); } while (n>0);
    val t1 = System.nanoTime()

    AXFINDER.collect.foreach{ case (k,v) => println(k+" => "+v); }

    in.close()
    sc.stop

    val t = (t1-t0) / 1000
    println("Running time: %d.%06d".format(t/1000000,t%1000000))

  }
}

