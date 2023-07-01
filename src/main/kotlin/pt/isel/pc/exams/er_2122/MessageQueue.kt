package pt.isel.pc.exams.er_2122

import pt.isel.pc.exams.utils.write
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MessageQueue<T> {

    private val lock = ReentrantLock()

    private val consumers = mutableListOf<Consumer>()
    private val producers = mutableListOf<Producer>()

    val consumersSize
        get() = consumers.size

    val producersSize
        get() = producers.size

    private inner class Consumer(
        val nOfMessages: Int,
        val thread: Thread,
        var values: MutableList<T> = mutableListOf<T>(),
        val condition: Condition = lock.newCondition(),
        var isDone: Boolean = false
    )

    private inner class Producer(
        val item: T,
        var consumer: Thread? = null,
        val condition: Condition = lock.newCondition(),
        var isDone: Boolean = false
    )


    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Thread? {
        lock.withLock {
            val producer = Producer(message)
            producers.add(producer)
            // fast path
            if (consumers.isNotEmpty() && consumers.first().nOfMessages == producers.size) {
                val consumer = consumers.removeFirst()
                consume(consumer)
                return consumer.thread
            }

            // wait path
            var remainingTime = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingTime = producer.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (producer.isDone) {
                        Thread.currentThread().interrupt()
                        return producer.consumer
                    }
                    producers.remove(producer)
                    throw e
                }

                if (producer.isDone) return producer.consumer

                if (remainingTime <= 0) {
                    producers.remove(producer)
                    return null
                }
            }
        }
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T> {
        lock.withLock {
            val consumer = Consumer(nOfMessages, Thread.currentThread())

            // fast path
            if(consumers.isEmpty() && producers.size >= nOfMessages) {
                return consume(consumer)
            }

            // wait path
            consumers.add(consumer)
            var remainingTime = timeout.inWholeNanoseconds
            while (true) {
                try {
                    remainingTime = consumer.condition.awaitNanos(remainingTime)
                } catch (e: InterruptedException) {
                    if (consumer.isDone) {
                        Thread.currentThread().interrupt()
                        return consumer.values
                    }
                    consumers.remove(consumer)
                    throw e
                }

                if (consumer.isDone) return consumer.values

                if (remainingTime <= 0) {
                    return consume(consumer)
                }
            }
        }
    }

    private fun consume(consumer: Consumer): List<T> {
        val values = mutableListOf<T>()
        while (producers.isNotEmpty() && values.size < consumer.nOfMessages) {
            val producer = producers.removeFirst()
            values.add(producer.item)
            producers.remove(producer)
            producer.consumer = consumer.thread
            producer.isDone = true
            producer.condition.signal()
        }
        consumers.remove(consumer)
        consumer.isDone = true
        consumer.condition.signal()
        consumer.values = values
        return values
    }
}

fun main() {
    val queue = MessageQueue<String>()
    val n = 3

    val insertValue = { value: String ->
        thread {
            write("Enqueued to T${queue.tryEnqueue(value, 10.toDuration(DurationUnit.SECONDS))?.id}")
        }
    }

    println("consumers: ${queue.consumersSize} producers: ${queue.producersSize}")

    repeat(n-1) {
        insertValue("ISEL")
    }

    Thread.sleep(1000)
    println("consumers: ${queue.consumersSize} producers: ${queue.producersSize}")

    thread {
        queue.tryDequeue(n, 5.toDuration(DurationUnit.SECONDS))
            .forEach { write("Dequeued $it") }
    }

    Thread.sleep(1000)
    println("consumers: ${queue.consumersSize} producers: ${queue.producersSize}")

    println("Going to insert final value")
    insertValue("PC")

    Thread.sleep(100)
    println("consumers: ${queue.consumersSize} producers: ${queue.producersSize}")
}
