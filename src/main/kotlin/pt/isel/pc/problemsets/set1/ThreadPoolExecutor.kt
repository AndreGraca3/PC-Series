package pt.isel.pc.problemsets.set1

import java.util.concurrent.Callable
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ThreadPoolExecutor(private val maxThreadPoolSize: Int, private val keepAliveTime: Duration) {

    private val lock = ReentrantLock()
    private val workersAvailable = lock.newCondition()

    private var isShuttingDown = false
    private val workQueue = mutableListOf<Runnable>()
    private val latch = Latch(0)

    val numberOfThreads
        get() = latch.counter

    fun <T> execute(callable: Callable<T>): Future<T> {
        val future = Future<T>()
        execute {
            try {
                future.start()
                future.setResult(callable.call())
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    @Throws(RejectedExecutionException::class)
    fun execute(runnable: Runnable) {
        lock.withLock {
            if (isShuttingDown) throw RejectedExecutionException()

            if (latch.counter < maxThreadPoolSize) {
                latch.countup()
                thread { workerLoop(runnable) }
            } else {
                workQueue.add(runnable)
                workersAvailable.signal()
            }
        }
    }

    fun shutdown() {
        println("Shutting Down!")
        lock.withLock {
            if (!isShuttingDown) {
                isShuttingDown = true
                workersAvailable.signalAll()
            }
        }
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {

        if (!isShuttingDown) return false

        try {
            return latch.await(timeout.inWholeSeconds, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw e
        }
    }

    private fun workerLoop(runnable: Runnable) {
        var currTask = runnable
        while (true) {
            kotlin.runCatching { currTask.run() }

            currTask = when (val result = getNextWorkItem()) {
                is Runnable -> result
                else -> return
            }
        }
    }

    private fun getNextWorkItem(): Runnable? {
        lock.withLock {
            while (true) {
                if (workQueue.isNotEmpty()) return workQueue.removeFirst()

                if (isShuttingDown) {
                    latch.countdown()
                    return null
                }

                try {
                    var remainingTime = keepAliveTime.inWholeNanoseconds

                    remainingTime = workersAvailable.awaitNanos(remainingTime)

                    if (remainingTime <= 0) {
                        latch.countdown()
                        return null
                    }
                } catch (ex: InterruptedException) {
                    latch.countdown()
                    return null
                }
            }
        }
    }
}