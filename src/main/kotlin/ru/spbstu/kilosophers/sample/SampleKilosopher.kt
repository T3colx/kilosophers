package ru.spbstu.kilosophers.sample

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import ru.spbstu.kilosophers.AbstractKilosopher
import ru.spbstu.kilosophers.Action
import ru.spbstu.kilosophers.ActionKind.*
import ru.spbstu.kilosophers.Fork
import ru.spbstu.kilosophers.sample.SampleKilosopher.State.*
import java.util.concurrent.Executors

//
var counter = 0
var startingPoint = 0
var kilosopherMaxIndex = 0
var eatingKilosophersMaxCount = 0
var lastKilosopherActSwitch = false
//

class SampleKilosopher(left: Fork, right: Fork, val index: Int) : AbstractKilosopher(left, right) {

    internal enum class State {
        WAITS_BOTH,
        WAITS_RIGHT,
        EATS,
        HOLDS_BOTH,
        HOLDS_RIGHT,
        THINKS
    }

    private var state = WAITS_BOTH
    private val counterContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher() + CoroutineName("Action of kilosopher #$index")

    override fun nextAction(): Action {
        if  (index % 2 == startingPoint && (startingPoint != 0 ||
            startingPoint == 0 &&
                    (index!= 0 && index != kilosopherMaxIndex ||
                            index == 0 && !lastKilosopherActSwitch ||
                            index == kilosopherMaxIndex && lastKilosopherActSwitch)
        )) {
            return when (state) {
                WAITS_BOTH -> TAKE_LEFT(10)
                WAITS_RIGHT -> TAKE_RIGHT(10)
                EATS -> EAT(50)
                HOLDS_BOTH -> DROP_LEFT(10)
                HOLDS_RIGHT -> DROP_RIGHT(10)
                THINKS -> THINK(100)
            }
        } else return when (state) {
            EATS -> EAT(50)
            HOLDS_BOTH -> DROP_LEFT(10)
            HOLDS_RIGHT -> DROP_RIGHT(10)
            else ->  THINK(100)
        }
    }


    override suspend fun handleResult(action: Action, result: Boolean) {
        state = when (action.kind) {
            TAKE_LEFT -> if (result) WAITS_RIGHT else WAITS_BOTH
            TAKE_RIGHT -> if (result) EATS else WAITS_RIGHT
            EAT -> HOLDS_BOTH
            DROP_LEFT -> if (result) {
                withContext(counterContext) {
                    counter++
                    if (counter == eatingKilosophersMaxCount) {
                        if (startingPoint == 1) lastKilosopherActSwitch = !lastKilosopherActSwitch
                        startingPoint = (startingPoint + 1) % 2
                        counter = 0
                    }
                }
                HOLDS_RIGHT
            } else HOLDS_BOTH
            DROP_RIGHT -> if (result) THINKS else HOLDS_RIGHT
            THINK -> WAITS_BOTH
        }
    }

    override fun toString(): String {
        return "Kilosopher #$index"
    }
}