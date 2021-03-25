package ru.spbstu.kilosophers.sample

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.spbstu.kilosophers.AbstractKilosopher
import ru.spbstu.kilosophers.Action
import ru.spbstu.kilosophers.ActionKind
import ru.spbstu.kilosophers.ActionKind.*
import ru.spbstu.kilosophers.Fork
import ru.spbstu.kilosophers.sample.SampleKilosopher.State.*
import java.util.concurrent.Executors

class SampleKilosopher(left: Fork, right: Fork, val index: Int) : AbstractKilosopher(left, right) {

    companion object {
        private var counter = 0
        private var startingPoint = 0
        private var lastKilosopherActSwitch = false
    }

    internal enum class State {
        WAITS_BOTH,
        WAITS_RIGHT,
        EATS,
        HOLDS_BOTH,
        HOLDS_RIGHT,
        THINKS
    }

    private var state = WAITS_BOTH
    private val counterContext =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher() + CoroutineName("Action of kilosopher #$index")


    private fun isMyTurnToEat(): Boolean =
        index % 2 == startingPoint && (startingPoint != 0 ||
                startingPoint == 0 &&
                (index != 0 && index != SampleUniversity.kilosopherMaxIndex ||
                        index == 0 && !lastKilosopherActSwitch ||
                        index == SampleUniversity.kilosopherMaxIndex && lastKilosopherActSwitch)
                )

    private suspend fun finishingChores(): State {
        withContext(counterContext) {
            counter++
            if (counter == SampleUniversity.eatingKilosophersMaxCount) {
                if (startingPoint == 1) lastKilosopherActSwitch = !lastKilosopherActSwitch
                startingPoint = (startingPoint + 1) % 2
                counter = 0
            }
        }
        return THINKS
    }


    override fun nextAction(): Action {
        return if (isMyTurnToEat()) {
            when (state) {
                WAITS_BOTH -> TAKE_LEFT(10)
                WAITS_RIGHT -> TAKE_RIGHT(10)
                EATS -> EAT(50)
                HOLDS_BOTH -> DROP_LEFT(10)
                HOLDS_RIGHT -> DROP_RIGHT(10)
                THINKS -> THINK(100)
            }
        } else when (state) {
            EATS -> EAT(50)
            HOLDS_BOTH -> DROP_LEFT(10)
            HOLDS_RIGHT -> DROP_RIGHT(10)
            else -> THINK(100)
        }
    }


    override suspend fun handleResult(action: Action, result: Boolean) {
        state = when (action.kind) {
            TAKE_LEFT -> if (result) WAITS_RIGHT else WAITS_BOTH
            TAKE_RIGHT -> if (result) EATS else WAITS_RIGHT
            EAT -> HOLDS_BOTH
            DROP_LEFT -> if (result) HOLDS_RIGHT else HOLDS_BOTH
            DROP_RIGHT -> if (result) finishingChores() else HOLDS_RIGHT
            THINK -> WAITS_BOTH
        }
    }

    override fun toString(): String {
        return "Kilosopher #$index"
    }
}