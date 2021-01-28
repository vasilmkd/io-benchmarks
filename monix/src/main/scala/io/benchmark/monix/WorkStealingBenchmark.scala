package io.benchmark.monix

import cats.implicits._

import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

import java.util.concurrent.{Executors, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import org.openjdk.jmh.annotations._

@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.MINUTES)
class WorkStealingBenchmark {

  @Param(Array("1000000"))
  var size: Int = _

  def schedulingBenchmark(implicit s: Scheduler): Int = {
    def fiber(i: Int): Task[Int] =
      Task.shift.flatMap { _ =>
        Task(i).flatMap { j =>
          Task.shift.flatMap { _ =>
            if (j > 10000)
              Task.shift.flatMap(_ => Task.pure(j))
            else
              Task.shift.flatMap(_ => fiber(j + 1))
          }
        }
      }

    val f =
      List
        .range(0, size)
        .traverse(fiber(_).start)
        .flatMap(_.traverse(_.join))
        .map(_.sum)
        .runToFuture

    Await.result(f, Duration.Inf)
  }

  @Benchmark
  def scheduling(): Int = {
    import monix.execution.Scheduler.Implicits.global
    schedulingBenchmark
  }

  def allocBenchmark(implicit s: Scheduler): Int = {
    def allocation(n: Int): Task[Array[AnyRef]] =
      Task {
        val size = math.max(100, math.min(n, 2000))
        val array = new Array[AnyRef](size)
        for (i <- (0 until size)) {
          array(i) = new AnyRef()
        }
        array
      }

    def sum(array: Array[AnyRef]): Task[Int] =
      Task {
        array.map(_.hashCode()).sum
      }

    def fiber(i: Int): Task[Int] =
      Task.shift.flatMap { _ =>
        allocation(i).flatMap { arr =>
          Task.shift.flatMap(_ => sum(arr)).flatMap { _ =>
            if (i > 1000)
              Task.shift.flatMap(_ => Task.pure(i))
            else
              Task.shift.flatMap(_ => fiber(i + 1))
          }
        }
      }

    val f =
      List
        .range(0, 2500)
        .traverse(_ => fiber(0).start)
        .flatMap(_.traverse(_.join))
        .map(_.sum)
        .runToFuture

    Await.result(f, Duration.Inf)
  }

  @Benchmark
  def alloc(): Int = {
    import monix.execution.Scheduler.Implicits.global
    allocBenchmark
  }
}
