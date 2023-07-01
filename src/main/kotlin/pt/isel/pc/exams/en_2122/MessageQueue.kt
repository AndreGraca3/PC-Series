package pt.isel.pc.exams.en_2122

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MessageQueue<T>() {

    private val lock = ReentrantLock()
    private val items = mutableListOf<T>()

    private inner class Request(
        val nOfMessages: Int,
        var values: List<T> = emptyList(),
        var condition: Condition = lock.newCondition(),
        var isDone: Boolean = false
    )

    private var consumers = mutableListOf<Request>()

    fun enqueue(message: T) {
        lock.withLock {
            items.add(message)
            if (consumers.isEmpty()) return
            if (consumers.first().nOfMessages <= items.size) {
                val consumer = consumers.removeFirst()
                consumer.values = dequeueMessages(consumer.nOfMessages)
                consumer.isDone = true
                consumer.condition.signal()
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {
        lock.withLock {
            // fast path
            if (items.size >= nOfMessages) return dequeueMessages(nOfMessages)

            // wait path
            val req = Request(nOfMessages)
            consumers.add(req)
            var remainingTime = timeout.inWholeNanoseconds

            while (true) {
                try {
                    remainingTime = req.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (req.isDone) {
                        Thread.currentThread().interrupt()
                        return req.values
                    }
                    consumers.remove(req)
                    throw e
                }

                if (req.isDone) return req.values

                if (remainingTime <= 0) {
                    println("Time is up!")
                    consumers.remove(req)
                    return null
                }
            }
        }
    }

    private fun dequeueMessages(nOfMessages: Int): List<T> {
        val res = mutableListOf<T>()
        for (i in 1..nOfMessages) {
            res.add(items.removeFirst())
        }
        return res
    }
}

fun main() {
    val queue = MessageQueue<String>()

    repeat(11) {
        thread {
            queue.tryDequeue(1, 5.toDuration(DurationUnit.SECONDS))
        }
    }

    Thread.sleep(10)

    repeat(10) {
        thread {
            queue.enqueue("coco")
        }
    }

}