
package app.crossword.yourealwaysbe.forkyz.exttools

import kotlin.coroutines.cancellation.CancellationException

@Throws(AppNotFoundException::class,CancellationException::class)
actual suspend fun ExternalToolLauncher.visit(data : CrosswordSolverData) {
    // TODO
}

@Throws(AppNotFoundException::class,CancellationException::class)
actual suspend fun ExternalToolLauncher.visit(data : FreeDictionaryData) {
    // TODO
}

@Throws(AppNotFoundException::class,CancellationException::class)
actual suspend fun ExternalToolLauncher.visit(data : QuickDictionaryData) {
    // TODO
}

@Throws(AppNotFoundException::class,CancellationException::class)
actual suspend fun ExternalToolLauncher.visit(data : Aard2DictionaryData) {
    // TODO
}

@Throws(AppNotFoundException::class,CancellationException::class)
actual suspend fun ExternalToolLauncher.visit(data : HTMLToolData) {
    // TODO
}

@Throws(AppNotFoundException::class,CancellationException::class)
actual suspend fun ExternalToolLauncher.visit(data : SharePuzzleData) {
    // TODO
}

@Throws(AppNotFoundException::class,CancellationException::class)
actual suspend fun ExternalToolLauncher.visit(data : ShareTextData) {
    // TODO
}

