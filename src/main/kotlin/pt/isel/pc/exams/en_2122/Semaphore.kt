package pt.isel.pc.exams.en_2122

import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class Semaphore(private val initialUnits: Int) {

    private val lock = ReentrantLock()

    private inner class Request(var isDone: Boolean = false, val condition: Condition = lock.newCondition())

    private val waiters = mutableListOf<Request>()
    private val terminationWaiters = Request()

    private var units = initialUnits
    private var isShuttingDown = false

    val currUnits
        get() = lock.withLock { units }

    fun release() {
        lock.withLock {
            units++
            if (waiters.isNotEmpty()) {
                val waiter = waiters.removeFirst()
                waiter.isDone = true
                waiter.condition.signal()
            }
            if (isShuttingDown && units == initialUnits) {
                terminationWaiters.isDone = true
                terminationWaiters.condition.signal()
            }
        }
    }

    @Throws(InterruptedException::class, RejectedExecutionException::class)
    fun acquire(timeout: Duration): Boolean {
        lock.withLock {
            // fast path
            if (isShuttingDown) throw RejectedExecutionException("Semaphore shutting down.")
            if (units > 0) {
                units--
                return true
            }

            // wait path
            val req = Request()
            waiters.add(req)
            var remainingTime = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingTime = req.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (isShuttingDown) {
                        waiters.remove(req)
                        throw RejectedExecutionException("Semaphore shutting down.")
                    }
                    if (req.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    waiters.remove(req)
                    throw e
                }

                if (isShuttingDown) {
                    println("T${Thread.currentThread().id} got rejected.")
                    waiters.remove(req)
                    throw RejectedExecutionException("Semaphore shutting down.")
                }

                if (req.isDone) return true

                if (remainingTime <= 0) {
                    waiters.remove(req)
                    throw TimeoutException("Timeout exceeded.")
                }
            }
        }
    }

    fun shutdown() {
        lock.withLock {
            if (isShuttingDown) return
            isShuttingDown = true
            for (i in 1..waiters.size) {
                val waiter = waiters.removeFirst()
                waiter.condition.signal()
            }
        }
    }

    @Throws(InterruptedException::class)
    fun awaitTermination(timeout: Duration): Boolean {
        lock.withLock {
            // fast path
            if (isShuttingDown && units == initialUnits) return true

            // wait path
            var remainingTime = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingTime = terminationWaiters.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw e
                }

                if (terminationWaiters.isDone) return true

                if (remainingTime <= 0) return false
            }
        }
    }
}


fun main() {
    val n = 3
    val semaphore = Semaphore(n)

    repeat(2 * n) {
        thread {
            try {
                semaphore.acquire(15.toDuration(DurationUnit.SECONDS))
                println("T${Thread.currentThread().id} acquired.")
                Thread.sleep(1000 * it.toLong())
            } finally {
                println("T${Thread.currentThread().id} released.")
                semaphore.release()
            }
        }
    }

    println("Semaphore Shutdown: ${semaphore.awaitTermination(10.toDuration(DurationUnit.SECONDS))}")
    println("Semaphore has ${semaphore.currUnits}/$n units")
}