package pt.isel.pc.problemsets.set1

import java.time.Duration


class BlockingMessageQueue<T>(private val capacity: Int) {

    @Throws(InterruptedException::class)
    fun tryEnqueue(message: T, timeout: Duration): Boolean {
        TODO()
    }

    @Throws(InterruptedException::class)
    fun tryDequeue(nOfMessages: Int, timeout: Duration): List<T>? {
        TODO()
    }
}