package com.cheacher.app.engine

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Same trick as [com.cheacher.app.progress.AndroidDataStoreLocation]: MainActivity
 * drops the native-library dir in here at startup so the expect/actual factory can find
 * the packaged Stockfish without a `Context` parameter infecting common code.
 */
object AndroidEngineLocation {
    @Volatile
    var nativeLibraryDir: String? = null
}

/**
 * Real Stockfish when the APK packaged one for this ABI, PocketFish when it didn't —
 * and PocketFish again, mid-game, if the process ever dies or talks nonsense
 * ([resilient]).
 *
 * The binary is Stockfish 11 (classical eval, ~1MB — see jniLibs/README.md for why
 * not a modern NNUE build), shipped disguised as `libstockfish.so`: Android will
 * happily execute a file from the app's own native-library dir, and packaging it as a
 * "library" is the one sanctioned way to get an executable there (`useLegacyPackaging`
 * keeps it extracted on disk).
 */
actual fun createSparringEngine(random: Random): SparringEngine {
    val fallback = PocketFish(random)
    val binary = AndroidEngineLocation.nativeLibraryDir
        ?.let { File(it, "libstockfish.so") }
        ?.takeIf { it.canExecute() }
        ?: return fallback
    return resilient(UciSparringEngine("Stockfish", { ProcessUciTransport(binary) }), fallback)
}

/** UCI over the Stockfish process's stdin/stdout, all I/O parked on [Dispatchers.IO]. */
private class ProcessUciTransport(binary: File) : UciTransport {
    private val process: Process = ProcessBuilder(binary.absolutePath)
        .redirectErrorStream(true)
        .start()
    private val writer: BufferedWriter = process.outputStream.bufferedWriter()
    private val reader: BufferedReader = process.inputStream.bufferedReader()

    override suspend fun writeLine(line: String) = withContext(Dispatchers.IO) {
        writer.write(line)
        writer.write("\n")
        writer.flush()
    }

    override suspend fun readLine(): String? = withContext(Dispatchers.IO) {
        reader.readLine()
    }

    override fun close() {
        runCatching { writer.write("quit\n"); writer.flush() }
        process.destroy()
    }
}
