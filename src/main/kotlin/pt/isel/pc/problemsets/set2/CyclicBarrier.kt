package pt.isel.pc.problemsets.set2

import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CyclicBarrier(private val parties: Int) {

    init {
        require(parties > 0) { "Parties should be positive" }
    }

    private val lock = ReentrantLock()
    private val waiters = lock.newCondition()

    private var barrierAction: Runnable? = null
    private var waitingThreads = 0
    private var generation = Generation(false)

    constructor(parties: Int, barrierAction: Runnable) : this(parties) {
        this.barrierAction = barrierAction
    }

    fun await(): Int {
        lock.withLock {
            if (generation.isBroken) throw BrokenBarrierException()
            val gen = generation
            var idx = 0
            // fast path
            if(waitingThreads == parties - 1) { // Last Thread to form group
                try {
                    barrierAction?.run()
                } catch (e: Exception) {
                    gen.isBroken = true
                    throw e
                }
                waiters.signalAll()
                waitingThreads = 0
                return idx
            } else {
                idx = parties - ++waitingThreads
                // wait path
                while (true) {
                    try {
                        waiters.await()
                        if (gen.isBroken) throw BrokenBarrierException()
                        return idx
                    } catch (e: InterruptedException) {
                        gen.isBroken = true
                        waiters.signalAll()
                        waitingThreads = 0
                        throw e
                    }
                }
            }
        }
    }

    fun getParties(): Int {
        lock.withLock {
            return parties
        }
    }

    fun getNumberWaiting(): Int {
        lock.withLock {
            return waitingThreads
        }
    }

    fun isBroken(): Boolean {
        lock.withLock {
            return generation.isBroken
        }
    }

    fun reset() {
        lock.withLock {
            generation.isBroken = true
            generation = Generation(false)
            waiters.signalAll()
            waitingThreads = 0
        }
    }

    inner class Generation(var isBroken: Boolean)
}