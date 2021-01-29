package io.benchmark.ce3

import org.openjdk.jmh.annotations.{
  Benchmark,
  BenchmarkMode,
  Fork,
  Level,
  Measurement,
  Mode,
  OutputTimeUnit,
  Param,
  Scope,
  Setup,
  State,
  Warmup
}

import java.util.concurrent.TimeUnit
import scala.collection.Iterable
import scala.util.Random

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS, time = 5)
@Warmup(iterations = 5, timeUnit = TimeUnit.SECONDS, time = 5)
class ParallelMergeSortBenchmark {

  @Param(Array("10000"))
  var size: Int = _

  @Param(Array("128"))
  var samples: Int = _

  @Param(Array("4096"))
  var parThreshold: Int = _

  private var sortInput: List[Vector[Int]] = _

  @Setup(Level.Iteration)
  def setup(): Unit =
    sortInput = 1
      .to(samples)
      .map(_ => Random.shuffle(1.to(size).toVector))
      .toList

  @Benchmark
  def ce3Sort(): Unit = benchMergeSort(cats.effect.unsafe.implicits.global)

  private def benchMergeSort(implicit runtime: IORuntime): Unit =
    (for {
      sortOutput <- sortInput.traverse(mergeSort(_))
      _ <- sortInput.zip(sortOutput).traverse(verifySorted(_))
    } yield ()).unsafeRunSync()

  private def verifySorted(inOut: (Iterable[Int], Iterable[Int])): IO[Unit] = {
    val sorted = inOut._2.toArray.sliding(2).forall {
      case Array(i1, i2) => i1 <= i2
      case _             => true
    }

    if (!sorted) IO.raiseError(new AssertionError(s"Not sorted: ${inOut._2} <-- ${inOut._1}"))
    else IO.unit
  }

  private def mergeSort(is: Iterable[Int]): IO[Iterable[Int]] =
    for {
      array <- IO(is.toArray)
      buf   <- IO(new Array[Int](array.length / 2))
      _     <- mergeSortInPlace(array, buf, 0, array.length)
    } yield array.toIterable

  private def mergeSortInPlace(is: Array[Int], buf: Array[Int], start: Int, end: Int): IO[Unit] = {
    val len = end - start
    if (len < 2) IO.unit
    else if (len == 2) {
      if (is(start) <= is(start + 1)) IO.unit
      else IO(swap(is, start, start + 1))
    } else {
      val middle    = start + len / 2
      val leftSort  = mergeSortInPlace(is, buf, start, middle)
      val rightSort = mergeSortInPlace(is, buf, middle, end)

      implicit val par = cats.Parallel[IO]

      val sortParts =
        if (len >= parThreshold) par.parProductL(leftSort)(rightSort)
        else leftSort.productL(rightSort)

      sortParts *> mergeInPlace(is, buf, start, middle, end)
    }
  }

  private def mergeInPlace(is: Array[Int], buf: Array[Int], start: Int, middle: Int, end: Int): IO[Unit] = IO {
    var i  = start / 2
    val ie = i + middle - start
    var j  = middle
    var k  = start
    System.arraycopy(is, start, buf, i, middle - start)

    while (i < ie && j < end) {
      val (a, b) = (buf(i), is(j))
      if (a < b) {
        is(k) = a
        i += 1
      } else {
        is(k) = b
        j += 1
      }

      k += 1
    }

    if (i < ie) {
      System.arraycopy(buf, i, is, k, ie - i)
    }
  }

  private def swap(is: Array[Int], i: Int, j: Int): Unit = {
    val tmp = is(i)
    is(i) = is(j)
    is(j) = tmp
  }
}
