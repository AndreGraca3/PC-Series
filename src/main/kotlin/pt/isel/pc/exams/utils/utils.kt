package pt.isel.pc.exams.utils

fun<T> write(str: T) {
    println("T${Thread.currentThread().id} $str")
}