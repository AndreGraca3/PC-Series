package pt.isel.pc.exams.er_2122

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ExecutorWithShutdown(private val executor: Executor) {

    private val lock = ReentrantLock()
    private val terminationWaitersCondition = lock.newCondition()

    private var isShuttingDown = false
    private val tasks = mutableListOf<Runnable>()

    val nrTasks
        get() = tasks.size

    val isShut
        get() = isShuttingDown

    @Throws(RejectedExecutionException::class)
    fun execute(command: Runnable) {
        lock.withLock {
            if (isShuttingDown) throw RejectedExecutionException("Executor is shutting down.")
            tasks.add(command)
            val completable = CompletableFuture<Unit>()
            completable.whenComplete { res, err ->
                lock.withLock {
                    println("Removed task: ${tasks.remove(command)}")
                    if (isShuttingDown && tasks.isEmpty()) terminationWaitersCondition.signalAll()
                }
            }
            executor.execute {
                command.run()
                completable.complete(Unit)
            }
        }
    }

    fun shutdown() {
        lock.withLock {
            if (isShuttingDown) return
            println(":: SHUTTING DOWN ::")
            isShuttingDown = true
            terminationWaitersCondition.signalAll()
        }
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            // fast path
            if (isShuttingDown && tasks.isEmpty()) return true

            // wait path
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                remainingTime = terminationWaitersCondition.awaitNanos(remainingTime)

                if (isShuttingDown && tasks.isEmpty()) return true

                if (remainingTime <= 0) return false
            }
        }
    }
}

fun main() {
    val ex = Executors.newCachedThreadPool()
    val ews = ExecutorWithShutdown(ex)

    repeat(6) {
        thread {
            ews.execute {
                println("task$it")
                if (it == 5) {
                    Thread.sleep(10000)
                } else {
                    Thread.sleep(1000)
                }
            }
        }
    }

    Thread.sleep(10)

    println("Handled ${ews.nrTasks} tasks") // 6

    Thread.sleep(2000)

    println("After short tasks: ${ews.nrTasks}") // 1

    ews.shutdown()
    ex.shutdown()
    thread {
        println("Finished waiting termination: ${ews.awaitTermination(15.toDuration(DurationUnit.SECONDS))}")
    }

    println("isShuttingDown: ${ews.isShut}") // true
}