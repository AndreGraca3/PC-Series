package pt.isel.pc.problemsets.set2

import org.junit.jupiter.api.assertThrows
import pt.isel.pc.problemsets.auxs.pressureTest
import kotlin.test.Test
import java.util.*
import java.util.concurrent.BrokenBarrierException
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.test.assertEquals


class CyclicBarrierTests {

    @Test
    fun `perform group with N threads, N-1, reset and N again`() {
        val N = 6
        val parties = 3
        var str = "ISEL"
        var threads: List<Thread>
        var exception: AssertionError? = null
        val barrier = CyclicBarrier(parties) { str = "PC" }

        val res: MutableList<Int> = Collections.synchronizedList(ArrayList())
        val formGroupTest = { barrier: CyclicBarrier, res: MutableList<Int>, amount: Int ->
            res.clear()
            threads = (1..amount).map {
                thread {
                    val value = barrier.await()
                    res.add(value)
                }
            }

            threads.forEach(Thread::join)

            assertEquals(amount, res.size)
            assertEquals("PC", str)
            repeat(3) { num ->
                assertEquals(amount / parties, res.count { it == num })
            }
        }

        formGroupTest(barrier, res, N)

        threads = (1..parties).map {
            thread {
                try {
                    if (it == parties) {
                        Thread.sleep(50)   // give other threads time to reach barrier
                        barrier.reset()
                    } else assertThrows<BrokenBarrierException> {
                        barrier.await()
                    }
                } catch (e: AssertionError) {
                    exception = e
                }
            }
        }

        threads.forEach(Thread::join)
        if (exception != null) throw exception as AssertionError

        assertEquals(parties, barrier.getParties())
        assertEquals(0, barrier.getNumberWaiting())

        formGroupTest(barrier, res, N)
    }

    @Test
    fun `pressure test perform a group with N size`() {
        pressureTest(::`perform group with N threads, N-1, reset and N again`, 1)
    }
}