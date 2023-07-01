package pt.isel.pc.exams.er_2122

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import pt.isel.pc.exams.en_2122.f0
import pt.isel.pc.exams.en_2122.f1

suspend fun <A, B, C> run(f0: suspend () -> A, f1: suspend () -> B, f2: suspend (A, B) -> C): C {
    return coroutineScope {
        val d0 = async { f0() }
        val d1 = async { f1() }

        f2(d0.await(), d1.await())
    }
}

suspend fun <A, B, C> f2(a: A, b: B): C {
    delay(10)
    return "$a and $b" as C
}

fun main() {
    runBlocking {
        launch {
            println(run<Int, Int, String>(::f0, ::f1, ::f2))
        }
    }
}