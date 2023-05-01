package pt.isel.pc.problemsets.set2

import kotlin.test.Test
import java.time.LocalTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.test.assertEquals

class ContainerTests {

    @Test
    fun `pressure test for N time`() {
        val endTime = LocalTime.now().plusMinutes(10)
        var iteration = 0
        println("Test stops at $endTime \n Testing...")

        while (LocalTime.now() < endTime) {
            iteration++
            val values = arrayOf(Value("A", 2), Value("B", 1), Value("C", 3))
            val container = Container(values)
            val res: MutableList<String> = Collections.synchronizedList(ArrayList())

            val threads = (1..10).map {
                thread {
                    while (true) {
                        val value = container.consume() ?: break
                        res.add(value)
                    }
                }
            }

            threads.forEach(Thread::join)
            assertEquals(6, res.size, "Failed at iteration $iteration with result: $res")
            assertEquals(2, res.count { it == "A" })
            assertEquals(1, res.count { it == "B" })
            assertEquals(3, res.count { it == "C" })
        }
    }
}