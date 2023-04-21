package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ThreadPoolExecutorTests {

    val N = 10

    @Test
    fun `N tasks with 1 Thread`() {
        val threadPool = ThreadPoolExecutor(1, 10.toDuration(DurationUnit.SECONDS))

        repeat(N) {
            threadPool.execute {
                println("hello")
                Thread.sleep(50)
            }
        }

        assertEquals(1, threadPool.numberOfThreads)
        threadPool.shutdown()
        assertTrue { threadPool.awaitTermination(15.toDuration(DurationUnit.SECONDS)) }
        assertEquals(0, threadPool.numberOfThreads)
    }

    @Test
    fun `N tasks with N Thread`() {
        val threadPool = ThreadPoolExecutor(N, 10.toDuration(DurationUnit.SECONDS))

        repeat(N) {
            threadPool.execute {
                println("hello")
                Thread.sleep(50)
            }
        }

        assertEquals(N, threadPool.numberOfThreads)
        threadPool.shutdown()
        assertTrue { threadPool.awaitTermination(15.toDuration(DurationUnit.SECONDS)) }
        assertEquals(0, threadPool.numberOfThreads)
    }

    @Test
    fun `obtain Future of task`() {
        val threadPool = ThreadPoolExecutor(N, 10.toDuration(DurationUnit.SECONDS))

        val future = threadPool.execute(Callable {
            val res = "ISEL"
            Thread.sleep(3000)
            res
        })

        assertThrows<TimeoutException> {
            future.get(1, TimeUnit.SECONDS)
        }

        assertEquals("ISEL", future.get())
        assertTrue(future.isDone())
    }
}