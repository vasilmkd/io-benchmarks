package io.benchmark.monix

import monix.eval.Task
import monix.execution.Scheduler
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
    def loop(i: Int)(implicit s: Scheduler): Task[Int] =
      Task(i).flatMap { j =>
        Task.shift.flatMap { _ =>
          if (j > size)
            Task.pure(j)
          else
            loop(j + 1)
        }
      }

    implicit val s = Scheduler.Implicits.global
    
    val f = loop(0).runToFuture

    Await.result(f, Duration.Inf)
  }
}
