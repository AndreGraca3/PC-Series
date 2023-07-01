package pt.isel.pc.exams.en_2122

import java.util.concurrent.atomic.AtomicInteger

class Value<T>(val value: T, private val initialLives: Int) {
    var lives = AtomicInteger(initialLives)
}

class Container<T>(private val values: Array<Value<T>>) {
    private var index = AtomicInteger(0)

    fun consume(): T? {
        while (true) {
            val oldIndex = index.get()
            if (oldIndex >= values.size) return null
            val oldLives = values[oldIndex].lives.get()
            if (oldLives > 0) {
                if (!values[oldIndex].lives.compareAndSet(oldLives, oldLives - 1)) continue
                return values[oldIndex].value
            }
            if (!index.compareAndSet(oldIndex, oldIndex + 1)) continue
        }
    }
}