package io.benchmark.ce3

import java.util.concurrent.TimeUnit
import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DeepBindBenchmark {
  @Param(Array("10000"))
  var size: Int = _

  @Benchmark
  def async(): Int = {
    def loop(i: Int): IO[Int] =
      IO(i).flatMap { j =>
        IO.cede.flatMap { _ =>
          if (j > size)
            IO.pure(j)
          else
            loop(j + 1)
        }
      }

    import cats.effect.unsafe.implicits.global
    loop(0).unsafeRunSync()
  }
}
