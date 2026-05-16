
package app.crossword.yourealwaysbe.forkyz.util

class ImaginaryTimer(private var elapsed : Long) {
    private var running = false
    private var incept : Long = 0

    fun getElapsed() : Long {
        return if (this.running)
            ((TimeUtils.currentTimeMillis() - this.incept) + elapsed)
        else
            this.elapsed
    }

    fun start() {
        this.incept = TimeUtils.currentTimeMillis()
        running = true
    }

    fun stop() {
        this.elapsed += (TimeUtils.currentTimeMillis() - this.incept)
        running = false
    }

    fun time() : String {
        return TimeUtils.convertMillisecondsToTime(getElapsed())
    }
}

