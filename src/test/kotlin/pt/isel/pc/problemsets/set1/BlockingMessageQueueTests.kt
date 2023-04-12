package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class BlockingMessageQueueTests {

    val N = 5

    @Test
    fun `1 producer and a consumer to consume 1`() {
        val queue = BlockingMessageQueue<String>(N)

        thread {
            queue.tryEnqueue("thread producer", Duration.INFINITE)
        }.join()

        assertEquals(1, queue.queueSize)
        assertEquals(0, queue.producersSize)

        thread {
            queue.tryDequeue(1, Duration.INFINITE)
        }.join()

        assertEquals(0, queue.consumersSize)
        assertEquals(0, queue.queueSize)
    }

    @Test
    fun `0 producers and a consumer to consume 1`() {
        val queue = BlockingMessageQueue<String>(N)

        thread {
            val value = queue.tryDequeue(1, 5.toDuration(DurationUnit.SECONDS))
            assertEquals(1, queue.consumersSize)
            assertNull(value)
        }

        assertEquals(0, queue.consumersSize)
        assertEquals(0, queue.queueSize)
    }

    @Test
    fun `N+2 producers`() {
        val queue = BlockingMessageQueue<String>(N)

        repeat(N + 2) {
            thread {
                queue.tryEnqueue("Thread $it", Duration.INFINITE)
            }
        }

        Thread.sleep(50)
        assertEquals(2, queue.producersSize)

        thread {
            queue.tryDequeue(N, Duration.INFINITE)
        }

        Thread.sleep(50)
        assertEquals(0, queue.consumersSize)
        assertEquals(2, queue.queueSize)
    }

    @Test
    fun `consumer to consume more than provided`() {
        val queue = BlockingMessageQueue<String>(N)

        repeat(N - 1) {
            thread {
                queue.tryEnqueue("Thread $it", Duration.INFINITE)
            }
        }

        Thread.sleep(50)

        thread {
            queue.tryDequeue(N, 5.toDuration(DurationUnit.SECONDS))
        }

        Thread.sleep(50)
        assertEquals(1, queue.consumersSize)
        assertEquals(N-1, queue.queueSize)
    }
}