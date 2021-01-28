package io.benchmark.ce2

import java.util.concurrent.TimeUnit
import cats.effect.{ContextShift, IO}
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DeepBindBenchmark {
  @Param(Array("10000"))
  var size: Int = _

  @Benchmark
  def async(): Int = {
    def loop(i: Int)(implicit cs: ContextShift[IO]): IO[Int] =
      IO(i).flatMap { j =>
        IO.shift.flatMap { _ =>
          if (j > size)
            IO.pure(j)
          else
            loop(j + 1)
        }
      }

    new cats.effect.IOApp {
      def run(args: List[String]) =
        loop(0).map(_ => cats.effect.ExitCode.Success)
    }.run(Nil).unsafeRunSync().code
  }
}
