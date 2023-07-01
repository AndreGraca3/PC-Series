package pt.isel.pc.exams

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pt.isel.pc.exams.utils.write

suspend fun mutexTest(mutex: Mutex, delay: Int): Int {
    mutex.withLock {
        write("Obtained lock")
        write("Going to sleep")
        delay(2000 + delay.toLong())
        write("Woke up")
    }
    write("Released lock")
    return 0
}

fun main() {
    runBlocking {
        val mutex = Mutex()
        repeat(6) {
            launch(Dispatchers.IO) {
                mutexTest(mutex, it)
            }
        }
    }
}