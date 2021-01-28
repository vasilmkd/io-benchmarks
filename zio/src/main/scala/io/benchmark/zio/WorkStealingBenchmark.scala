package io.benchmark.zio

import zio.{Runtime, Task}

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

  def schedulingBenchmark(implicit rt: Runtime[Any]): Int = {
    def fiber(i: Int): Task[Int] =
      Task.yieldNow.flatMap { _ =>
        Task(i).flatMap { j =>
          Task.yieldNow.flatMap { _ =>
            if (j > 10000)
              Task.yieldNow.flatMap(_ => Task.succeed(j))
            else
              Task.yieldNow.flatMap(_ => fiber(j + 1))
          }
        }
      }

    val t =
      Task
        .foreach(List.range(0, size))(fiber(_).fork)
        .flatMap(Task.foreach(_)(_.join))
        .map(_.sum)

    rt.unsafeRun(t)
  }

  @Benchmark
  def scheduling(): Int = {
    implicit val rt = Runtime.default
    schedulingBenchmark
  }

  def allocBenchmark(implicit rt: Runtime[Any]): Int = {
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
      Task.yieldNow.flatMap { _ =>
        allocation(i).flatMap { arr =>
          Task.yieldNow.flatMap(_ => sum(arr)).flatMap { _ =>
            if (i > 1000)
              Task.yieldNow.flatMap(_ => Task.succeed(i))
            else
              Task.yieldNow.flatMap(_ => fiber(i + 1))
          }
        }
      }

    val t =
      Task.foreach(List.range(0, 2500))(_ => fiber(0).fork)
        .flatMap(Task.foreach(_)(_.join))
        .map(_.sum)

    rt.unsafeRun(t)
  }

  @Benchmark
  def alloc(): Int = {
    implicit val rt = Runtime.default
    allocBenchmark
  }
}
