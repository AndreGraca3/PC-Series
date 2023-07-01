package pt.isel.pc.exams.ee_2021

import pt.isel.pc.exams.utils.write
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class SemaphoreWithWaitFull(private val initialUnits: Int) {

    private val lock = ReentrantLock()

    private inner class Request(var isDone: Boolean = false, val condition: Condition = lock.newCondition())

    private val requests = mutableListOf<Request>()
    private val fullWaiters = mutableListOf<Request>()
    private var units = initialUnits

    val currUnitsSize
        get() = units

    val acquireQueueSize
        get() = requests.size

    val waitersQueueSize
        get() = fullWaiters.size

    @Throws(InterruptedException::class)
    fun acquireSingle(timeout: Long): Boolean {
        lock.withLock {
            write("Trying to acquire")
            // fast path
            if (units > 0) {
                units--
                return true
            }

            // wait path
            val request = Request()
            requests.add(request)
            var remainingTime = 0L
            while (true) {
                try {
                    remainingTime = request.condition.awaitNanos(timeout)
                } catch (e: InterruptedException) {
                    if (request.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    requests.remove(request)
                    throw e
                }

                if(request.isDone) return true

                if(remainingTime <= 0) {
                    requests.remove(request)
                    return false
                }
            }
        }
    }

    fun releaseSingle() {
        lock.withLock {
            println("Releasing")
            units++

            if (requests.isNotEmpty()) {
                val waiter = requests.removeFirst()
                waiter.isDone = true
                waiter.condition.signal()
            }

            if (units == initialUnits && requests.isEmpty()) {
                while (fullWaiters.isNotEmpty()) {
                    val waiter = fullWaiters.removeFirst()
                    waiter.isDone = true
                    waiter.condition.signal()
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun waitFull(timeout: Long): Boolean {
        lock.withLock {
            write("requests: ${requests.size} units: $units")
            // fast path
            if (units == initialUnits && requests.isEmpty()) return true

            // wait path
            val waiter = Request()
            fullWaiters.add(waiter)
            var remainingTime = timeout
            while (true) {
                try {
                    remainingTime = waiter.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (waiter.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    fullWaiters.remove(waiter)
                    throw e
                }

                if (waiter.isDone) return true

                if (remainingTime <= 0) {
                    fullWaiters.remove(waiter)
                    return false
                }
            }
        }
    }
}

/*

fun main() {
    val sem = SemaphoreWithWaitFull(3)

    val acquire = {
        thread {
            thread {
                sem.acquireSingle(1.toDuration(DurationUnit.SECONDS).inWholeNanoseconds)
                write("Acquired")
            }
        }
    }

    val release = {
        thread {
            thread {
                sem.releaseSingle()
            }
        }
    }

    val isFull = {
        thread {
            write("isFull: ${sem.waitFull(5.toDuration(DurationUnit.SECONDS).inWholeNanoseconds)}")
        }
    }

    isFull()    // false
    Thread.sleep(10)
    acquire()   // true
    Thread.sleep(10)
    isFull()    // true after 1s
    Thread.sleep(1000)
    release()
    Thread.sleep(10)
    isFull()    // true after 1s
}*/

fun main() {
    val initialUnits = 5
    val semaphore = SemaphoreWithWaitFull(initialUnits)

    repeat(initialUnits) {
        thread {
            semaphore.acquireSingle(500)
        }
    }

    TimeUnit.MILLISECONDS.sleep(100)

    println(semaphore.currUnitsSize) // 0
    println(semaphore.acquireQueueSize) // 0
    println(semaphore.waitersQueueSize) // 0

    thread {
        semaphore.waitFull(10.toDuration(DurationUnit.SECONDS).inWholeNanoseconds)
    }

    TimeUnit.MILLISECONDS.sleep(100)

    println(semaphore.currUnitsSize) // 0
    println(semaphore.acquireQueueSize) // 0
    println(semaphore.waitersQueueSize) // 1


    repeat(initialUnits) {
        thread {
            semaphore.releaseSingle()
        }
    }

    TimeUnit.MILLISECONDS.sleep(100)

    println(semaphore.currUnitsSize) // 5
    println(semaphore.acquireQueueSize) // 0
    println(semaphore.waitersQueueSize) // 0
}