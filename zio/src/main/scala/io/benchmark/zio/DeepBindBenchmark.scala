package io.benchmark.zio

import zio.{Task, Runtime}
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Await
import scala.concurrent.duration._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DeepBindBenchmark {
  @Param(Array("10000"))
  var size: Int = _

  @Benchmark
  def async(): Int = {
    def loop(i: Int): Task[Int] =
      Task(i).flatMap { j =>
        Task.yieldNow.flatMap { _ =>
          if (j > size)
            Task.succeed(j)
          else
            loop(j + 1)
        }
      }
    
    Runtime.default.unsafeRun(loop(0))
  }
}
