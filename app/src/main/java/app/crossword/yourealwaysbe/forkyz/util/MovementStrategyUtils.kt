
package app.crossword.yourealwaysbe.forkyz.util

import java.util.function.Consumer

import androidx.annotation.MainThread

import app.crossword.yourealwaysbe.forkyz.settings.ForkyzSettings
import app.crossword.yourealwaysbe.forkyz.settings.CycleUnfilledMode
import app.crossword.yourealwaysbe.puz.MovementStrategy

/**
 * Get the stop on on strategy for the current cycle settings
 *
 * The get method builds the right strategy and keeps it for future calls, only
 * refreshes if the settings have changed.
 */
class StopOnEndStrategyHolder {
    private var cycleForwards : Boolean = false
    private var cycleBackwards : Boolean = false
    private var movementStrategy : MovementStrategy? = null

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

            if (
                movementStrategy == null
                || this.cycleForwards != cycleForwards
                || this.cycleBackwards != cycleBackwards
            ) {
                if (cycleForwards || cycleBackwards) {
                    movementStrategy = MovementStrategy.CycleUnfilled(
                        MovementStrategy.STOP_ON_END,
                        cycleForwards,
                        cycleBackwards,
                    )
                } else {
                    movementStrategy = MovementStrategy.STOP_ON_END
                }

                this.cycleForwards = cycleForwards
                this.cycleBackwards = cycleBackwards
            }

            cb?.accept(movementStrategy ?: MovementStrategy.STOP_ON_END)
        }
    }
}
