package pt.isel.pc.problemsets.set1

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Latch(private var count: Int) {
    private val lock = ReentrantLock()
    private val waiters = lock.newCondition()

    val counter
        get() = lock.withLock { count }

    fun await(timeout: Long, unit: TimeUnit): Boolean {
        lock.withLock {
            println("T${Thread.currentThread().id} Awaiting termination for $count threads")
            return waiters.await(timeout, unit)
        }
    }

    fun countdown() {
        lock.withLock {
            if (--count == 0) waiters.signalAll()
        }
    }

    fun countup() {
        lock.withLock {
            count++
        }
    }
}