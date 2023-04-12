package pt.isel.pc.problemsets.set1

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration


class BlockingMessageQueue<T>(private val capacity: Int) {

    private val lock = ReentrantLock()

    val producersSize get() = lock.withLock { producers.size }
    val consumersSize get() = lock.withLock { consumers.size }
    val queueSize get() = lock.withLock { items.size }

    private var producers = mutableListOf<ProducerRequest>()
    private var consumers = mutableListOf<ConsumerRequest>()

     var items = mutableListOf<T>()

    private inner class ConsumerRequest(
        val nOfMessages: Int,
        var values: MutableList<T> = mutableListOf(),
        val condition: Condition = lock.newCondition(),
        var isDone: Boolean = false
    )

    private inner class ProducerRequest(
        var item: T,
        val condition: Condition = lock.newCondition(),
        var isDone: Boolean = false
    )

    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Boolean {
        lock.withLock {
            // fast path

            if (consumers.isNotEmpty() && items.size + 1 == consumers.first().nOfMessages) {
                val consumerReq = consumers.removeFirst()
                items.add(message)
                consumerReq.values = items
                items = mutableListOf()
                consumerReq.isDone = true
                consumerReq.condition.signal()
                return true
            }

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
                    remainingTime = request.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (request.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    producers.remove(request)
                    throw e
                }

                if (request.isDone) return true

                if (remainingTime <= 0) {
                    producers.remove(request)
                    return false
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {

        require(nOfMessages <= capacity + 1) { "nOfMessages $nOfMessages exceeds limit ${capacity + 1}" }

        lock.withLock {
            // fast path

            if (consumers.isEmpty() && items.size >= nOfMessages) {
                return consume(nOfMessages)
            }

            // wait path
            val request = ConsumerRequest(nOfMessages)
            consumers.add(request)
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = request.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (request.isDone) {
                        Thread.currentThread().interrupt()
                        return request.values
                    }
                    consumers.remove(request)
                    throw e
                }

                if (request.isDone) return request.values

                if (remainingTime <= 0) {
                    consumers.remove(request)
                    return null
                }
            }
        }
    }

    private fun consume(nOfMessages: Int): List<T> {
        val list = ArrayList(items.subList(0, nOfMessages))
        for (i in 0 until nOfMessages) {
            items.removeFirst()
            if (producers.isNotEmpty()) {
                val producerReq = producers.removeFirst()
                items.add(producerReq.item)
                producerReq.isDone = true
                producerReq.condition.signal()
            }
        }
        return list
    }
}