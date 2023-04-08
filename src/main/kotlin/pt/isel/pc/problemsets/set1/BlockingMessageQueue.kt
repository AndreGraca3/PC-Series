package pt.isel.pc.problemsets.set1

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration


class BlockingMessageQueue<T>(private val capacity: Int) {

    private val lock = ReentrantLock()

    var producers = mutableListOf<ProducerRequest>()
    var consumers = mutableListOf<ConsumerRequest>()

    private val producersWaiters = lock.newCondition()

    var items = mutableListOf<T>()

    inner class ProducerRequest(
        val msg: T,
        var isDone: Boolean = false
    )

    inner class ConsumerRequest(
        val nrOfMessages: Int,
        var values: MutableList<T> = mutableListOf(),
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
            val request = ProducerRequest(message)
            producers.add(request)
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = producersWaiters.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (request.isDone && items.size + 1 <= capacity) {   // in case thread is interrupted right when it's done
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
                val list = ArrayList(items.subList(0, nOfMessages))
                for (i in 0 until nOfMessages) {
                    items.removeFirst()
                }
                if (nOfMessages > producers.size) {
                    producers.forEach { it.isDone = true }
                } else {
                    producers.subList(0, nOfMessages).forEach { it.isDone = true }
                }
                producersWaiters.signalAll()
                return list
            }
            val request = ConsumerRequest(nOfMessages)
            consumers.add(request)

            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = request.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (request.isDone) {   // in case thread is interrupted right when it's done
                        val list = ArrayList(items.subList(0, nOfMessages))
                        for (i in 0 until nOfMessages) {
                            items.removeFirst()
                        }
                        consumers.remove(request)
                        if (nOfMessages > producers.size) {
                            producers.forEach { it.isDone = true }
                        } else {
                            producers.subList(0, nOfMessages).forEach { it.isDone = true }
                        }
                        producersWaiters.signalAll()
                        Thread.currentThread().interrupt()
                        return list
                    }
                    consumers.remove(request)
                    throw e
                }

                if (request.isDone && items.size >= request.nrOfMessages) {
                    val list = ArrayList(items.subList(0, nOfMessages))
                    for (i in 0 until nOfMessages) {
                        items.removeFirst()
                    }
                    consumers.remove(request)
                    if (nOfMessages > producers.size) {
                        producers.forEach { it.isDone = true }
                    } else {
                        producers.subList(0, nOfMessages).forEach { it.isDone = true }
                    }
                    producersWaiters.signalAll()
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
}