package pt.isel.pc.exams.er_2122

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pt.isel.pc.exams.utils.write
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageBox<T> {

    private val lock = ReentrantLock()
    private val waiters = mutableListOf<Continuation<T>>()

    suspend fun waitForMessage(): T {
        lock.lock()

        return suspendCoroutine {
            waiters.add(it)
            lock.unlock()
        }
    }

    fun sendToAll(message: T): Int {
        lock.lock()

        waiters.forEach { it.resume(message) }
        val res = waiters.size
        waiters.clear()
        lock.unlock()
        return res
    }
}


fun main() {
    val mb = MessageBox<String>()

    runBlocking {
        repeat(5) {
            launch {
                write("Going to wait for message")
                write("Message received: ${mb.waitForMessage()}")
            }
        }
        delay(3000)
        write("Sending message to ${mb.sendToAll("ISEL")} suspended coroutines")
    }
}