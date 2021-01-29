package io.benchmark.ce3

import cats.syntax.all._
import cats.effect.implicits._
import cats.effect.IO
import org.openjdk.jmh.annotations._

import java.util.concurrent.TimeUnit

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
  def catsSequence(): Long = {
    val tasks = (0 until count).map(_ => IO(1)).toList
    val result = tasks.sequence.map(_.sum.toLong)
    import cats.effect.unsafe.implicits.global
    result.unsafeRunSync()
  }

  @Benchmark
  def catsParSequence(): Long = {
    val tasks = (0 until count).map(_ => IO(1)).toList
    val result = tasks.parSequence.map(_.sum.toLong)
    import cats.effect.unsafe.implicits.global
    result.unsafeRunSync()
  }

  @Benchmark
  def catsParSequenceN(): Long = {
    val tasks = (0 until count).map(_ => IO(1)).toList
    val result = tasks.parSequenceN(parallelism).map(_.sum.toLong)
    import cats.effect.unsafe.implicits.global
    result.unsafeRunSync()
  }
}
