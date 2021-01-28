package io.benchmark.ce2

import cats.effect.{ContextShift, IO}
import cats.syntax.all._

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MINUTES)
class WorkStealingBenchmark {

  @Param(Array("1000000"))
  var size: Int = _

  def schedulingBenchmark(implicit cs: ContextShift[IO]): IO[Int] = {
    def fiber(i: Int): IO[Int] =
      IO.shift.flatMap { _ =>
        IO(i).flatMap { j =>
          IO.shift.flatMap { _ =>
            if (j > 10000)
              IO.shift.flatMap(_ => IO.pure(j))
            else
              IO.shift.flatMap(_ => fiber(j + 1))
          }
        }
      }

    List
      .range(0, size)
      .traverse(fiber(_).start)
      .flatMap(_.traverse(_.join))
      .map(_.sum)
  }

  @Benchmark
  def scheduling(): Int = {
    val app = new cats.effect.IOApp {
      def run(args: List[String]) =
        schedulingBenchmark.map(_ => cats.effect.ExitCode.Success)
    }
    app.run(Nil).unsafeRunSync().code
  }

  def allocBenchmark(implicit cs: ContextShift[IO]): IO[Int] = {
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
      IO.shift.flatMap { _ =>
        allocation(i).flatMap { arr =>
          IO.shift.flatMap(_ => sum(arr)).flatMap { _ =>
            if (i > 1000)
              IO.shift.flatMap(_ => IO.pure(i))
            else
              IO.shift.flatMap(_ => fiber(i + 1))
          }
        }
      }

    List
      .range(0, 2500)
      .traverse(_ => fiber(0).start)
      .flatMap(_.traverse(_.join))
      .map(_.sum)
  }

  @Benchmark
  def alloc(): Int = {
    val app = new cats.effect.IOApp {
      def run(args: List[String]) =
        allocBenchmark.map(_ => cats.effect.ExitCode.Success)
    }
    app.run(Nil).unsafeRunSync().code
  }
}
