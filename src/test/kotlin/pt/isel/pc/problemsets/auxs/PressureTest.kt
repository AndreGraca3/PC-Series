package pt.isel.pc.problemsets.auxs

import java.time.LocalTime
import kotlin.reflect.KFunction


/**
 * Executes the given test function for the specified duration and checks for failures.
 *
 * @param test The test function to execute.
 * @param durationMinutes The duration of the test in minutes.
 * @throws AssertionError if the test fails.
 * @throws Exception if any other exception is thrown during the test.
 */
fun pressureTest(action: () -> Unit, time: Long) {
    val endTime = LocalTime.now().plusMinutes(time)
    var iteration = 0
    println("Test stops at $endTime \n Testing...")

    while (LocalTime.now() < endTime) {
        ++iteration
        try {
            action()
        } catch (e: Throwable) {
            println("Failed at iteration $iteration")
            throw e
        }
    }
}