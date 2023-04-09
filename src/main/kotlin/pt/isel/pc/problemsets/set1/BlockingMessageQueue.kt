package pt.isel.pc.problemsets.set1

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration


class BlockingMessageQueue<T>(private val capacity: Int) {

    private val lock = ReentrantLock()

    private var producers = mutableListOf<Request>()
    private var consumers = mutableListOf<Request>()

    private var items = mutableListOf<T>()

    private inner class Request(
        val condition: Condition = lock.newCondition(),
        var isDone: Boolean = false
    )

    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Boolean {
        lock.withLock {
            // fast path

            if (items.size + 1 <= capacity) {
                items.add(message)
                return true
            }

            // wait path
            val request = Request()
            producers.add(request)
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = request.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (request.isDone && items.size + 1 <= capacity) {
                        items.add(message)
                        if (consumers.isNotEmpty()) consumers.first().condition.signal()
                        Thread.currentThread().interrupt()
                        producers.remove(request)
                        return true
                    }
                    producers.remove(request)
                    throw e
                }

                if (request.isDone && items.size + 1 <= capacity) {
                    items.add(message)
                    if (consumers.isNotEmpty()) consumers.first().condition.signal()
                    producers.remove(request)
                    return true
                }

                if (remainingTime <= 0) {
                    producers.remove(request)
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {
        lock.withLock {
            if (consumers.isEmpty() && items.size >= nOfMessages) {
                return consume(nOfMessages)
            }

            val request = Request()
            consumers.add(request)
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = request.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (request.isDone) {
                        val list = consume(nOfMessages)
                        consumers.remove(request)
                        Thread.currentThread().interrupt()
                        return list
                    }
                    consumers.remove(request)
                    throw e
                }

                if (request.isDone && items.size >= nOfMessages) {
                    val list = consume(nOfMessages)
                    consumers.remove(request)
                    if (consumers.isNotEmpty()) {
                        val next = consumers.first()
                        next.isDone = true
                        next.condition.signal()
                    }
                    return list
                }

                if (remainingTime <= 0) {
                    consumers.remove(request)
                    if (consumers.isNotEmpty()) {
                        val next = consumers.first()
                        next.isDone = true
                        next.condition.signal()
                    }
                    return null
                }
            }
        }
    }

    private fun consume(nOfMessages: Int): List<T> {
        val list = ArrayList(items.subList(0, nOfMessages))
        for (i in 0 until nOfMessages) {
            items.removeFirst()
        }
        if (nOfMessages > producers.size) {
            producers.forEach {
                it.isDone = true
                it.condition.signal()
            }
        } else {
            producers.subList(0, nOfMessages).forEach {
                it.isDone = true
                it.condition.signal()
            }
        }
        return list
    }
}