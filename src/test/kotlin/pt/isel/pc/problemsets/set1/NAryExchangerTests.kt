package pt.isel.pc.problemsets.set1

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class NAryExchangerTests {

    val N = 5

    @Test
    fun `exchange all N threads`() {
        val exchanger = NAryExchanger<String>(N)

        repeat(N) {
            thread {
                exchanger.exchange("thread $it", Duration.INFINITE)
            }
        }
    }

    @Test
    fun `exchange N-1 threads`() {
        val exchanger = NAryExchanger<String>(N)

        repeat(N - 1) {
            thread {
                exchanger.exchange("thread $it", Duration.INFINITE)
            }
        }
    }

    @Test
    fun `exchange N+1 threads`() {
        val exchanger = NAryExchanger<String>(N)

        repeat(N) {
            thread {
                val list = exchanger.exchange("thread $it", Duration.INFINITE)
                assertNotNull(list)
                assertEquals(N, list.size)
            }
        }
        Thread.sleep(1000)
        assertNull(exchanger.exchange("never", 1.toDuration(DurationUnit.SECONDS)))
    }

    @Test
    fun `exchange threads and interrupt one`() {
        val exchanger = NAryExchanger<String>(N)

        val cancelledThread = thread {
            assertThrows<InterruptedException> {
                exchanger.exchange("interruped", Duration.INFINITE)
            }
        }
        Thread.sleep(2000)
        cancelledThread.interrupt()
    }
}