package io.benchmark.zio

import org.openjdk.jmh.annotations._

import zio._

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@Threads(1)
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class SequenceBenchmark {

  @Param(Array("100", "1000"))
  var count: Int = _

  val parallelism: Int = 10

  @Benchmark
  def zioSequence(): Long = {
    val tasks = (0 until count).map(_ => ZIO.effectTotal(1)).toList
    val result = ZIO.collectAll(tasks).map(_.sum.toLong)
    Runtime.default.unsafeRun(result)
  }

  @Benchmark
  def zioParSequence(): Long = {
    val tasks = (0 until count).map(_ => ZIO.effectTotal(1)).toList
    val result = ZIO.collectAllPar(tasks).map(_.sum.toLong)
    Runtime.default.unsafeRun(result)
  }

  @Benchmark
  def zioParSequenceN(): Long = {
    val tasks = (0 until count).map(_ => ZIO.effectTotal(1)).toList
    val result = ZIO.collectAllParN(parallelism)(tasks).map(_.sum.toLong)
    Runtime.default.unsafeRun(result)
  }

  @Benchmark
  def futureSequence(): Long = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val futures = (0 until count).map(_ => Future(1)).toList
    val f: Future[Long] = Future.sequence(futures).map(_.sum.toLong)
    Await.result(f, Duration.Inf)
  }

}
