package io.benchmark.ce3

import cats.effect.IO
import cats.effect.unsafe.IORuntime
import cats.syntax.all._

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MINUTES)
class WorkStealingBenchmark {

  @Param(Array("1000000"))
  var size: Int = _

  def schedulingBenchmark(implicit rt: IORuntime): Int = {
    def fiber(i: Int): IO[Int] =
      IO.cede.flatMap { _ =>
        IO(i).flatMap { j =>
          IO.cede.flatMap { _ =>
            if (j > 10000)
              IO.cede.flatMap(_ => IO.pure(j))
            else
              IO.cede.flatMap(_ => fiber(j + 1))
          }
        }
      }

    List
      .range(0, size)
      .traverse(fiber(_).start)
      .flatMap(_.traverse(_.joinAndEmbedNever))
      .map(_.sum)
      .unsafeRunSync()
  }

  @Benchmark
  def scheduling(): Int = {
    import cats.effect.unsafe.implicits.global
    schedulingBenchmark
  }

  def allocBenchmark(implicit rt: IORuntime): Int = {
    def allocation(n: Int): IO[Array[AnyRef]] =
      IO {
        val size = math.max(100, math.min(n, 2000))
        val array = new Array[AnyRef](size)
        for (i <- (0 until size)) {
          array(i) = new AnyRef()
        }
        array
      }

    def sum(array: Array[AnyRef]): IO[Int] =
      IO {
        array.map(_.hashCode()).sum
      }

    def fiber(i: Int): IO[Int] =
      IO.cede.flatMap { _ =>
        allocation(i).flatMap { arr =>
          IO.cede.flatMap(_ => sum(arr)).flatMap { _ =>
            if (i > 1000)
              IO.cede.flatMap(_ => IO.pure(i))
            else
              IO.cede.flatMap(_ => fiber(i + 1))
          }
        }
      }

    List
      .range(0, 2500)
      .traverse(_ => fiber(0).start)
      .flatMap(_.traverse(_.joinAndEmbedNever))
      .map(_.sum)
      .unsafeRunSync()
  }

  @Benchmark
  def alloc(): Int = {
    import cats.effect.unsafe.implicits.global
    allocBenchmark
  }
}
