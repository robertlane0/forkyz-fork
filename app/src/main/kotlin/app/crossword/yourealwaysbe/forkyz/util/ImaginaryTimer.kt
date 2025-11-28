
package app.crossword.yourealwaysbe.forkyz.util

import java.text.NumberFormat

private val SECONDS = 1000L
private val MINUTES : Long = 60L * SECONDS

class ImaginaryTimer(private var elapsed : Long) {
    private val format : NumberFormat = NumberFormat.getInstance()
    private var running = false
    private var incept : Long = 0

    init {
        this.format.setMinimumIntegerDigits(2)
    }

    fun getElapsed() : Long {
        return if (this.running)
            ((System.currentTimeMillis() - this.incept) + elapsed)
        else
            this.elapsed
    }

    fun start() {
        this.incept = System.currentTimeMillis()
        running = true
    }

    fun stop() {
        this.elapsed += (System.currentTimeMillis() - this.incept)
        running = false
    }

    fun time() : String {
        val elapsed = this.getElapsed()
        val mins : Long = elapsed / MINUTES
        val secs : Long = (elapsed - (MINUTES * mins)) / SECONDS

        return StringBuilder(mins.toString()).append(":")
            .append(this.format.format(secs))
            .toString()
    }
}

