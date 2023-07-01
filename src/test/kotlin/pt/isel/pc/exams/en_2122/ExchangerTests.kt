package pt.isel.pc.exams.en_2122

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pt.isel.pc.problemsets.auxs.pressureTest
import kotlin.test.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertEquals

class ExchangerTests {

    @Test
    fun `exchange values by N pair of threads`() {
        val N = 20
        val exchanger = Exchanger<String>()
        val res: MutableList<String> = Collections.synchronizedList(ArrayList())
        runBlocking {
            repeat(N * 2) {
                launch {
                    val str = if (it % 2 == 0) "isel" else "pc"
                    val value = exchanger.exchange(str)
                    res.add(value)
                }
            }
        }

        assertEquals(40, res.size)
        assertEquals(20, res.count { it == "isel" })
        assertEquals(20, res.count { it == "pc" })
    }

    @Test
    fun `pressure test exchange values by N pair of threads`() {
        pressureTest(::`exchange values by N pair of threads`, 1)
    }
}