package pt.isel.pc.problemsets.set2

import java.util.concurrent.atomic.AtomicInteger


class Container<T>(private val values: Array<Value<T>>) {

     var index = AtomicInteger(0)

    fun consume(): T? {
        while (true) {
            val idx = index.get()
            if (idx < values.size) {
                val lives = values[idx].lives.get()
                if (lives > 0) {
                    if (values[idx].lives.compareAndSet(lives, lives - 1)) {
                        return values[idx].value
                    }
                } else {
                    index.compareAndSet(idx, idx + 1)
                }
            } else return null
        }
    }
}

class Value<T>(val value: T, initialLives: Int) {
    var lives = AtomicInteger(initialLives)
}