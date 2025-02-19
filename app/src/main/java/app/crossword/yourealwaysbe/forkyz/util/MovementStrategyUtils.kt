
package app.crossword.yourealwaysbe.forkyz.util

import java.util.function.Consumer

import androidx.annotation.MainThread

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.CycleUnfilledMode
import app.crossword.yourealwaysbe.puz.MovementStrategy

@MainThread
fun getStopOnEndStrategy(
    settings : ForkyzSettings?,
    cb : Consumer<MovementStrategy>?,
) {
    settings?.getPlayCycleUnfilledMode() { cycleUnfilled ->
        val cycleForwards
            = cycleUnfilled != CycleUnfilledMode.CU_NEVER
        val cycleBackwards
            = cycleUnfilled == CycleUnfilledMode.CU_ALWAYS

        if (cycleForwards || cycleBackwards) {
            cb?.accept(
                MovementStrategy.CycleUnfilled(
                    MovementStrategy.STOP_ON_END,
                    cycleForwards,
                    cycleBackwards,
                )
            )
        } else {
            cb?.accept(MovementStrategy.STOP_ON_END)
        }
    }
}

