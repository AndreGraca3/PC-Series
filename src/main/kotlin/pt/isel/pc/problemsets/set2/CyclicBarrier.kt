package pt.isel.pc.problemsets.set2

import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class CyclicBarrier(private val parties: Int) {

    init {
        require(parties > 0) { "Parties should be positive" }
    }

    private val lock = ReentrantLock()
    private val waiters = lock.newCondition()

    private var barrierAction: Runnable? = null
    private var isBroken = false
    private var waitingThreads = 0
    private var id = 0

    constructor(parties: Int, barrierAction: Runnable) : this(parties) {
        this.barrierAction = barrierAction
    }

    fun await(): Int {
        lock.withLock {
            if (isBroken) throw BrokenBarrierException()
            val id = id
            var idx = 0

            if(getParties() == 1) {
                println("T${Thread.currentThread().id}:: Tripping Barrier ::")
                isBroken = true
                barrierAction?.run()
                waiters.signalAll()
                this.id++
            } else {
                waitingThreads++
                idx = getParties()
                println("T${Thread.currentThread().id}:: Awaiting for $idx threads to trip the barrier")
                while (!isBroken && id == this.id) {
                    waiters.await()
                }
            }
            return idx
        }
    }

    fun getParties(): Int {
        lock.withLock {
            return parties - waitingThreads
        }
    }

    fun getNumberWaiting(): Int {
        lock.withLock {
            return waitingThreads
        }
    }

    fun isBroken(): Boolean {
        lock.withLock {
            return isBroken
        }
    }

    fun reset() {
        TODO("Docs say to throw BarrierBrokenException if the barrier is reset while any thread is waiting")

        println(":: Resetting Barrier ::")
        lock.withLock {
            waiters.signalAll()
            this.id++
            isBroken = false
            waitingThreads = 0
        }
    }
}


fun main() {
    val N = 3
    val barrier = CyclicBarrier(N) { println("Im Done") }

    repeat(1) {
        thread {
            println("T${Thread.currentThread().id}:: idx is ${barrier.await()}")
        }
    }

    Thread.sleep(500)

    barrier.reset()

    repeat(3) {
        thread {
            println("T${Thread.currentThread().id}:: idx is ${barrier.await()}")
        }
    }
}